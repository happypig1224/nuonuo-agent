import {
  FormEvent,
  MouseEvent as ReactMouseEvent,
  TouchEvent as ReactTouchEvent,
  useCallback,
  useEffect,
  useRef,
  useState
} from 'react';
import { triggerTapHaptic } from '../utils/haptics';

interface FloatingDockProps {
  keyboardOffset: number;
  disabled: boolean;
  onOpenCamera: () => void;
  onSendText: (text: string) => void;
  onSendVoice: (audioBlob: Blob) => void;
  onVoiceError?: (message: string) => void;
}

type VoiceZone = 'idle' | 'cancel' | 'save';

const WAVE_BAR_COUNT = 18;
const IDLE_BAR_HEIGHTS = Array.from({ length: WAVE_BAR_COUNT }, () => 4);

export function FloatingDock({
  keyboardOffset,
  disabled,
  onOpenCamera,
  onSendText,
  onSendVoice,
  onVoiceError
}: FloatingDockProps) {
  const [inputMode, setInputMode] = useState<'pill' | 'text'>('pill');
  const [draft, setDraft] = useState('');
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [voiceActive, setVoiceActive] = useState(false);
  const [activeZone, setActiveZone] = useState<VoiceZone>('idle');
  const [voiceSeconds, setVoiceSeconds] = useState(0);
  const [waveHeights, setWaveHeights] = useState<number[]>(IDLE_BAR_HEIGHTS);

  const isPressingRef = useRef<boolean>(false);
  const touchStartPosRef = useRef<{ x: number; y: number } | null>(null);
  const activeZoneRef = useRef<VoiceZone>('idle');
  const voiceSecondsRef = useRef<number>(0);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const rafIdRef = useRef<number | null>(null);
  const chunksRef = useRef<BlobPart[]>([]);
  const shouldSendOnStopRef = useRef<boolean>(false);

  activeZoneRef.current = activeZone;
  voiceSecondsRef.current = voiceSeconds;

  const stopWaveformMonitor = useCallback(() => {
    if (rafIdRef.current !== null) {
      window.cancelAnimationFrame(rafIdRef.current);
      rafIdRef.current = null;
    }
    if (audioCtxRef.current) {
      void audioCtxRef.current.close().catch(() => {});
      audioCtxRef.current = null;
    }
    setWaveHeights(IDLE_BAR_HEIGHTS);
  }, []);

  const stopMediaTracks = useCallback(() => {
    stopWaveformMonitor();
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    recorderRef.current = null;
  }, [stopWaveformMonitor]);

  const startWaveformMonitor = useCallback((stream: MediaStream) => {
    const AudioCtx =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!AudioCtx) {
      return;
    }

    try {
      const ctx = new AudioCtx();
      if (ctx.state === 'suspended') {
        void ctx.resume();
      }
      const source = ctx.createMediaStreamSource(stream);
      const analyser = ctx.createAnalyser();
      analyser.fftSize = 64;
      analyser.smoothingTimeConstant = 0.72;
      source.connect(analyser);
      audioCtxRef.current = ctx;

      const dataArray = new Uint8Array(analyser.frequencyBinCount);
      const half = WAVE_BAR_COUNT / 2;

      const tick = () => {
        analyser.getByteFrequencyData(dataArray);
        let sum = 0;
        for (let i = 0; i < dataArray.length; i++) {
          sum += dataArray[i];
        }
        const avgLevel = sum / (dataArray.length * 255);

        // 低于底噪阈值时保持平缓基线（4px），有声时按各频段强弱实时渲染对称声波
        if (avgLevel < 0.035) {
          setWaveHeights(IDLE_BAR_HEIGHTS);
        } else {
          const next = new Array<number>(WAVE_BAR_COUNT);
          for (let i = 0; i < WAVE_BAR_COUNT; i++) {
            const distFromCenter = Math.abs(i - (half - 0.5)) / half;
            const binIndex = Math.min(dataArray.length - 1, Math.floor(distFromCenter * 10) + 1);
            const bandEnergy = dataArray[binIndex] / 255;
            const envelope = 1 - distFromCenter * 0.45;
            const normalized = Math.max(0, bandEnergy * 0.75 + avgLevel * 0.65) * envelope;
            next[i] = Math.round(4 + Math.min(1, normalized * 1.6) * 40);
          }
          setWaveHeights(next);
        }
        rafIdRef.current = window.requestAnimationFrame(tick);
      };

      rafIdRef.current = window.requestAnimationFrame(tick);
    } catch {
      // 忽略 AudioContext 初始化受限环境，保持平缓基线
    }
  }, []);

  useEffect(() => {
    return () => {
      isPressingRef.current = false;
      shouldSendOnStopRef.current = false;
      stopMediaTracks();
    };
  }, [stopMediaTracks]);

  useEffect(() => {
    if (!voiceActive) {
      setVoiceSeconds(0);
      return;
    }
    const timer = window.setInterval(() => {
      setVoiceSeconds((prev) => prev + 1);
    }, 1000);
    return () => window.clearInterval(timer);
  }, [voiceActive]);

  const handleSubmit = useCallback(
    (e: FormEvent) => {
      e.preventDefault();
      const trimmed = draft.trim();
      if (!trimmed || disabled) {
        return;
      }
      triggerTapHaptic();
      onSendText(trimmed);
      setDraft('');
      setInputMode('pill');
    },
    [draft, disabled, onSendText]
  );

  const finalizeRecording = useCallback(
    (shouldSend: boolean) => {
      isPressingRef.current = false;
      touchStartPosRef.current = null;
      setActiveZone('idle');
      setVoiceActive(false);
      shouldSendOnStopRef.current = shouldSend;

      const recorder = recorderRef.current;
      if (recorder && recorder.state !== 'inactive') {
        recorder.stop();
      } else {
        chunksRef.current = [];
        stopMediaTracks();
      }
    },
    [stopMediaTracks]
  );

  const handleVoiceMove = useCallback((clientX: number) => {
    if (!isPressingRef.current || !touchStartPosRef.current) {
      return;
    }
    const dx = clientX - touchStartPosRef.current.x;
    const screenWidth = typeof window !== 'undefined' ? window.innerWidth : 390;
    const screenRatio = clientX / screenWidth;

    let nextZone: VoiceZone = 'idle';
    if (dx < -24 || screenRatio < 0.34) {
      nextZone = 'cancel';
    } else if (dx > 24 || screenRatio > 0.66) {
      nextZone = 'save';
    }

    if (nextZone !== activeZoneRef.current) {
      setActiveZone(nextZone);
      triggerTapHaptic();
    }
  }, []);

  const handleVoiceRelease = useCallback(() => {
    if (!isPressingRef.current) {
      return;
    }
    const finalZone = activeZoneRef.current;
    if (finalZone === 'cancel') {
      finalizeRecording(false);
      return;
    }
    if (voiceSecondsRef.current < 1 && finalZone !== 'save') {
      finalizeRecording(false);
      onVoiceError?.('说话时间太短，已取消');
      return;
    }
    finalizeRecording(true);
  }, [finalizeRecording, onVoiceError]);

  useEffect(() => {
    if (!voiceActive) {
      return;
    }

    const onMouseMove = (e: MouseEvent) => handleVoiceMove(e.clientX);
    const onMouseUp = () => handleVoiceRelease();
    const onTouchMove = (e: TouchEvent) => {
      if (e.touches[0]) {
        handleVoiceMove(e.touches[0].clientX);
      }
    };
    const onTouchEnd = () => handleVoiceRelease();
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        finalizeRecording(false);
      }
    };
    const onWindowBlur = () => finalizeRecording(false);

    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    window.addEventListener('touchmove', onTouchMove, { passive: true });
    window.addEventListener('touchend', onTouchEnd);
    window.addEventListener('touchcancel', onTouchEnd);
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('blur', onWindowBlur);

    return () => {
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('mouseup', onMouseUp);
      window.removeEventListener('touchmove', onTouchMove);
      window.removeEventListener('touchend', onTouchEnd);
      window.removeEventListener('touchcancel', onTouchEnd);
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('blur', onWindowBlur);
    };
  }, [finalizeRecording, handleVoiceMove, handleVoiceRelease, voiceActive]);

  const handleStartPress = useCallback(
    async (e: ReactTouchEvent<HTMLButtonElement> | ReactMouseEvent<HTMLButtonElement>) => {
      if (disabled || isPressingRef.current) {
        return;
      }
      const clientX =
        'touches' in e && e.touches[0] ? e.touches[0].clientX : (e as ReactMouseEvent).clientX;
      const clientY =
        'touches' in e && e.touches[0] ? e.touches[0].clientY : (e as ReactMouseEvent).clientY;

      triggerTapHaptic();
      isPressingRef.current = true;
      touchStartPosRef.current = { x: clientX, y: clientY };
      setActiveZone('idle');

      if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
        isPressingRef.current = false;
        onVoiceError?.('当前环境暂不支持麦克风录音，请点击右侧改用文字输入');
        return;
      }

      const releaseEarly = () => {
        isPressingRef.current = false;
      };
      window.addEventListener('mouseup', releaseEarly, { once: true });
      window.addEventListener('touchend', releaseEarly, { once: true });
      window.addEventListener('touchcancel', releaseEarly, { once: true });

      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        window.removeEventListener('mouseup', releaseEarly);
        window.removeEventListener('touchend', releaseEarly);
        window.removeEventListener('touchcancel', releaseEarly);

        if (!isPressingRef.current) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }

        streamRef.current = stream;
        chunksRef.current = [];
        shouldSendOnStopRef.current = false;

        const recorder = new MediaRecorder(stream);
        recorderRef.current = recorder;

        recorder.ondataavailable = (ev) => {
          if (ev.data.size > 0) {
            chunksRef.current.push(ev.data);
          }
        };

        recorder.onstop = () => {
          const shouldSend = shouldSendOnStopRef.current && chunksRef.current.length > 0;
          const mimeType = recorder.mimeType || 'audio/webm';
          const blob = new Blob(chunksRef.current, { type: mimeType });
          chunksRef.current = [];
          shouldSendOnStopRef.current = false;
          stopMediaTracks();
          if (shouldSend && blob.size > 0) {
            onSendVoice(blob);
          }
        };

        recorder.start();
        startWaveformMonitor(stream);
        setVoiceActive(true);
      } catch {
        window.removeEventListener('mouseup', releaseEarly);
        window.removeEventListener('touchend', releaseEarly);
        window.removeEventListener('touchcancel', releaseEarly);
        isPressingRef.current = false;
        stopMediaTracks();
        onVoiceError?.('未获取到麦克风权限，请点击右侧改用文字输入');
      }
    },
    [disabled, onSendVoice, onVoiceError, startWaveformMonitor, stopMediaTracks]
  );

  const handleInputBlur = useCallback(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'smooth' });
  }, []);

  const formattedDuration = `${Math.floor(voiceSeconds / 60)}:${String(voiceSeconds % 60).padStart(2, '0')}`;

  return (
    <>
      {voiceActive && (
        <div
          className="ptt-sector-overlay"
          role="dialog"
          aria-label="语音录制中"
          onClick={() => finalizeRecording(false)}
        >
          <div className={`ptt-live-stage zone-${activeZone}`} onClick={(e) => e.stopPropagation()}>
            <div className="ptt-hud-card-header">
              <span className="ptt-hud-pulse-dot" aria-hidden="true" />
              <span className="ptt-hud-timer">{formattedDuration}</span>
            </div>
            <div className="ptt-hud-wave" aria-hidden="true">
              {waveHeights.map((h, idx) => (
                <span
                  key={idx}
                  className="ptt-live-bar"
                  style={{ height: `${h}px` }}
                />
              ))}
            </div>
          </div>

          <div className={`ptt-floating-prompt-zone zone-${activeZone}`}>
            <span className="ptt-hud-prompt">
              {activeZone === 'cancel'
                ? '松开取消发送'
                : activeZone === 'save'
                  ? '松开立即发送'
                  : '左滑取消，松开或右滑发送'}
            </span>
          </div>

          <div
            className={`ptt-fan-dock ${activeZone !== 'idle' ? `zone-${activeZone}` : ''}`}
            onClick={(e) => e.stopPropagation()}
          >
            <div className={`ptt-wing-container is-cancel ${activeZone === 'cancel' ? 'is-active' : ''}`}>
              <button
                type="button"
                className={`ptt-arc-paddle is-cancel ${activeZone === 'cancel' ? 'is-active' : ''}`}
                aria-label="取消语音"
                onClick={() => finalizeRecording(false)}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                </svg>
                <span>取消</span>
              </button>
            </div>

            <div className={`ptt-wing-container is-save ${activeZone === 'save' ? 'is-active' : ''}`}>
              <button
                type="button"
                className={`ptt-arc-paddle is-save ${activeZone === 'save' ? 'is-active' : ''}`}
                aria-label="发送语音"
                onClick={() => finalizeRecording(true)}
              >
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                <span>发送</span>
              </button>
            </div>
          </div>
        </div>
      )}

      <footer
        className="dock-wrapper"
        style={{ transform: `translateY(-${keyboardOffset}px)` }}
      >
        {inputMode === 'pill' ? (
          <div className="lg-capsule dock-bar is-icon-dock">
            <button
              type="button"
              className="interactive-btn lg-subpill dock-pill-side"
              aria-label="拍照识别"
              onClick={() => {
                triggerTapHaptic();
                onOpenCamera();
              }}
            >
              <svg className="dock-svg-icon" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9">
                <path strokeLinecap="round" strokeLinejoin="round" d="M3 9a2 2 0 012-2h2.4a1.5 1.5 0 001.25-.67l.85-1.28A1.5 1.5 0 0110.75 4.4h2.5a1.5 1.5 0 011.25.65l.85 1.28A1.5 1.5 0 0016.6 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                <circle cx="12" cy="13.2" r="3.4" />
                <circle cx="17.3" cy="10.2" r="0.9" fill="currentColor" stroke="none" />
              </svg>
            </button>

            <button
              type="button"
              className="interactive-btn dock-voice-btn"
              aria-label="按住说话"
              onTouchStart={handleStartPress}
              onMouseDown={handleStartPress}
            >
              <svg className="dock-svg-icon" width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.1">
                <rect x="9" y="2.5" width="6" height="11" rx="3" fill="currentColor" stroke="none" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M5.5 11.5a6.5 6.5 0 0013 0M12 18v3M9 21h6" />
              </svg>
            </button>

            <button
              type="button"
              className="interactive-btn lg-subpill dock-pill-mint"
              aria-label="输入消息"
              onClick={() => {
                triggerTapHaptic();
                setInputMode('text');
                window.requestAnimationFrame(() => inputRef.current?.focus());
              }}
            >
              <svg className="dock-svg-icon" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M20.5 11.5a8.2 8.2 0 01-.9 3.7 8.4 8.4 0 01-7.5 4.6 8.2 8.2 0 01-3.7-.9L3.5 20.5l1.7-5a8.2 8.2 0 01-.9-3.7 8.4 8.4 0 014.6-7.5 8.2 8.2 0 013.7-.9h.4a8.3 8.3 0 017.5 7.6v.5z" />
                <circle cx="9.2" cy="11.8" r="1.15" fill="currentColor" stroke="none" />
                <circle cx="12.2" cy="11.8" r="1.15" fill="currentColor" stroke="none" />
                <circle cx="15.2" cy="11.8" r="1.15" fill="currentColor" stroke="none" />
              </svg>
            </button>
          </div>
        ) : (
          <form className="lg-capsule dock-bar" onSubmit={handleSubmit}>
            <button
              type="button"
              className="interactive-btn dock-icon-btn"
              aria-label="返回语音"
              onClick={() => {
                triggerTapHaptic();
                setInputMode('pill');
              }}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 18l-6-6 6-6" />
              </svg>
            </button>
            <input
              ref={inputRef}
              type="text"
              className="dock-input"
              placeholder="输入想问小糯的成分或肤况..."
              value={draft}
              autoFocus
              onChange={(e) => setDraft(e.target.value)}
              onBlur={handleInputBlur}
              disabled={disabled}
            />
            <button
              type="submit"
              className="interactive-btn dock-send-btn"
              disabled={!draft.trim() || disabled}
            >
              发送
            </button>
          </form>
        )}
      </footer>
    </>
  );
}
