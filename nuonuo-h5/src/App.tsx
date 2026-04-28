import { useCallback, useEffect, useRef, useState } from 'react';
import { CameraActionSheet } from './components/CameraActionSheet';
import { FloatingDock } from './components/FloatingDock';
import { MemoryDrawer } from './components/MemoryDrawer';
import { MessageBubble } from './components/MessageBubble';
import { NuonuoAvatar } from './components/NuonuoAvatar';
import { BackgroundTheme, QuickMenuSheet } from './components/QuickMenuSheet';
import { useStickyScroll } from './hooks/useStickyScroll';
import { useVisualViewport } from './hooks/useVisualViewport';
import { getOrCreateSessionId, resetSessionId } from './services/apiClient';
import {
  fetchChatHistory,
  startChatSseStream,
  transcribeVoice,
  uploadMedia
} from './services/chatService';
import {
  clearAllMemories,
  deleteMemory,
  fetchMemories,
  updateMemory
} from './services/memoryService';
import { ChatMessage, EmotionState, MediaUploadScene, StreamPhase } from './types/chat';
import { MemoryItem } from './types/memory';
import { triggerAlertHaptic } from './utils/haptics';

interface PendingRetryPayload {
  prompt: string;
  imageFileId?: string;
  imageTag?: string;
}

export function App() {
  const [sessionId, setSessionId] = useState<string>(() => getOrCreateSessionId());
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [memories, setMemories] = useState<MemoryItem[]>([]);
  const [isHydrating, setIsHydrating] = useState<boolean>(true);
  const [emotion, setEmotion] = useState<EmotionState>('NORMAL');
  const [streamPhase, setStreamPhase] = useState<StreamPhase>('idle');
  const [streamError, setStreamError] = useState<string | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [cameraOpen, setCameraOpen] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [toastText, setToastText] = useState<string | null>(null);
  const [theme, setTheme] = useState<BackgroundTheme>(() => {
    try {
      const saved = window.localStorage.getItem('nuonuo_theme');
      if (saved === 'peach' || saved === 'sky' || saved === 'night') {
        return saved;
      }
    } catch {
      // 忽略受限存储
    }
    const hour = new Date().getHours();
    return hour >= 0 && hour < 5 ? 'night' : 'peach';
  });

  const abortStreamRef = useRef<(() => void) | null>(null);
  const toastTimerRef = useRef<number | null>(null);
  const objectUrlsRef = useRef<string[]>([]);
  const lastRequestRef = useRef<PendingRetryPayload | null>(null);

  const { keyboardOffset, isKeyboardOpen } = useVisualViewport();
  const { containerRef, showNewMessagePill, handleScroll, followStreamIfSticky, scrollToBottom } =
    useStickyScroll();

  const revokeTrackedObjectUrls = useCallback(() => {
    objectUrlsRef.current.forEach((url) => URL.revokeObjectURL(url));
    objectUrlsRef.current = [];
  }, []);

  useEffect(() => {
    return () => {
      abortStreamRef.current?.();
      revokeTrackedObjectUrls();
      if (toastTimerRef.current !== null) {
        window.clearTimeout(toastTimerRef.current);
      }
    };
  }, [revokeTrackedObjectUrls]);

  useEffect(() => {
    const themeColorMap: Record<BackgroundTheme, string> = {
      peach: '#f7ebe4',
      sky: '#5d8fb2',
      night: '#131926'
    };
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute('content', themeColorMap[theme]);
    try {
      window.localStorage.setItem('nuonuo_theme', theme);
    } catch {
      // 忽略受限存储
    }
  }, [theme]);

  useEffect(() => {
    let cancelled = false;
    setIsHydrating(true);
    const hydrate = async () => {
      try {
        const [history, profileMemories] = await Promise.all([
          fetchChatHistory(sessionId),
          fetchMemories()
        ]);
        if (!cancelled) {
          setMessages(history);
          setMemories(profileMemories);
        }
      } catch {
        // 后端尚未就绪时保持空会话与空记忆态，等待用户发起对话或重试
      } finally {
        if (!cancelled) {
          setIsHydrating(false);
        }
      }
    };
    void hydrate();
    return () => {
      cancelled = true;
    };
  }, [sessionId]);

  const showToast = useCallback((text: string) => {
    if (toastTimerRef.current !== null) {
      window.clearTimeout(toastTimerRef.current);
    }
    setToastText(text);
    toastTimerRef.current = window.setTimeout(() => {
      setToastText(null);
    }, 2000);
  }, []);

  const streamAssistantReply = useCallback(
    (payload: PendingRetryPayload, assistantId: string) => {
      abortStreamRef.current?.();
      setStreamError(null);
      setStreamPhase('streaming');
      lastRequestRef.current = payload;

      abortStreamRef.current = startChatSseStream(
        {
          sessionId,
          prompt: payload.prompt,
          imageFileId: payload.imageFileId,
          imageTag: payload.imageTag
        },
        {
          onEmotion: (nextEmotion) => {
            setEmotion(nextEmotion);
            if (nextEmotion === 'ALERT') {
              triggerAlertHaptic();
            }
          },
          onDelta: (chunk) => {
            setMessages((prev) =>
              prev.map((m) => (m.id === assistantId ? { ...m, content: m.content + chunk } : m))
            );
            followStreamIfSticky();
          },
          onWarningCard: (warningCard) => {
            triggerAlertHaptic();
            setMessages((prev) =>
              prev.map((m) => (m.id === assistantId ? { ...m, warningCard } : m))
            );
            followStreamIfSticky();
          },
          onRoutineCard: (routineCard) => {
            setMessages((prev) =>
              prev.map((m) => (m.id === assistantId ? { ...m, routineCard } : m))
            );
            followStreamIfSticky();
          },
          onMemoryExtracted: (extracted) => {
            setMemories((prev) => {
              const idx = prev.findIndex((item) => item.id === extracted.id || item.profileKey === extracted.profileKey);
              if (idx >= 0) {
                const next = [...prev];
                next[idx] = extracted;
                return next;
              }
              return [extracted, ...prev];
            });
            showToast('小糯记住一条新画像');
          },
          onDone: () => {
            setStreamPhase('done');
          },
          onError: (msg) => {
            setStreamPhase('error');
            setStreamError(msg);
          }
        }
      );
    },
    [followStreamIfSticky, sessionId, showToast]
  );

  const dispatchPrompt = useCallback(
    (
      promptText: string,
      options?: { imageFileId?: string; imagePreviewUrl?: string; imageTag?: string }
    ) => {
      const userMsg: ChatMessage = {
        id: `user_${Date.now()}`,
        role: 'user',
        content: promptText,
        imagePreviewUrl: options?.imagePreviewUrl,
        imageTag: options?.imageTag,
        createdAt: Date.now()
      };

      const assistantId = `assistant_${Date.now() + 1}`;
      const assistantMsg: ChatMessage = {
        id: assistantId,
        role: 'assistant',
        content: '',
        createdAt: Date.now() + 1
      };

      setMessages((prev) => [...prev, userMsg, assistantMsg]);
      window.requestAnimationFrame(followStreamIfSticky);

      streamAssistantReply(
        {
          prompt: promptText,
          imageFileId: options?.imageFileId,
          imageTag: options?.imageTag
        },
        assistantId
      );
    },
    [followStreamIfSticky, streamAssistantReply]
  );

  const handleRetryStream = useCallback(() => {
    const lastPayload = lastRequestRef.current;
    if (!lastPayload || streamPhase === 'streaming') {
      return;
    }
    const assistantId = `assistant_${Date.now()}`;
    setMessages((prev) => {
      const last = prev[prev.length - 1];
      if (last && last.role === 'assistant' && !last.content) {
        return prev.map((m, i) => (i === prev.length - 1 ? { ...m, id: assistantId } : m));
      }
      return [...prev, { id: assistantId, role: 'assistant', content: '', createdAt: Date.now() }];
    });
    streamAssistantReply(lastPayload, assistantId);
  }, [streamAssistantReply, streamPhase]);

  const handleFileSelected = useCallback(
    async (file: File, scene: MediaUploadScene, modeLabel: string) => {
      const previewUrl = URL.createObjectURL(file);
      objectUrlsRef.current.push(previewUrl);
      const defaultPrompt =
        scene === 'selfie'
          ? '帮我分析这张面部照片的当前肤况'
          : '帮我识别图中的护肤品成分并排查冲突';

      try {
        const uploaded = await uploadMedia(file, scene);
        dispatchPrompt(defaultPrompt, {
          imageFileId: uploaded.fileId,
          imagePreviewUrl: uploaded.url || previewUrl,
          imageTag: modeLabel
        });
      } catch (err) {
        showToast(err instanceof Error ? err.message : '图片上传失败，请稍后重试');
      }
    },
    [dispatchPrompt, showToast]
  );

  const handleSendVoice = useCallback(
    async (audioBlob: Blob) => {
      try {
        const { text } = await transcribeVoice(audioBlob);
        const trimmed = text.trim();
        if (!trimmed) {
          showToast('未识别到有效语音，请重试');
          return;
        }
        dispatchPrompt(trimmed);
      } catch (err) {
        showToast(err instanceof Error ? err.message : '语音识别失败，请改用文字输入');
      }
    },
    [dispatchPrompt, showToast]
  );

  const handleUpdateMemory = useCallback(
    async (id: string, nextValue: string) => {
      try {
        const updated = await updateMemory(id, nextValue);
        setMemories((prev) => prev.map((item) => (item.id === id ? updated : item)));
        showToast('已更新记忆');
      } catch (err) {
        showToast(err instanceof Error ? err.message : '记忆更新失败');
      }
    },
    [showToast]
  );

  const handleDeleteMemory = useCallback(
    async (id: string) => {
      try {
        await deleteMemory(id);
        setMemories((prev) => prev.filter((item) => item.id !== id));
        showToast('已遗忘该条记忆');
      } catch (err) {
        showToast(err instanceof Error ? err.message : '删除记忆失败');
      }
    },
    [showToast]
  );

  const handleClearMemories = useCallback(async () => {
    try {
      await clearAllMemories();
      setMemories([]);
      showToast('已清空全部记忆');
    } catch (err) {
      showToast(err instanceof Error ? err.message : '清空记忆失败');
    }
  }, [showToast]);

  const handleResetChat = useCallback(() => {
    abortStreamRef.current?.();
    revokeTrackedObjectUrls();
    lastRequestRef.current = null;
    setStreamPhase('idle');
    setStreamError(null);
    setEmotion('NORMAL');
    setMessages([]);
    setSessionId(resetSessionId());
    showToast('已开启新对话');
  }, [revokeTrackedObjectUrls, showToast]);

  return (
    <div className="app-shell" data-theme={theme}>
      <div className="ambient-wallpaper" aria-hidden="true" />

      {toastText && (
        <div className="island-toast" role="status">
          {toastText}
        </div>
      )}

      <header className="top-nav">
        <button
          type="button"
          className="interactive-btn lg-capsule nav-icon-btn"
          aria-label="快捷菜单"
          onClick={() => setMenuOpen(true)}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9">
            <path strokeLinecap="round" d="M4 7h16M4 12h16M4 17h16" />
          </svg>
        </button>

        <div className="lg-capsule nav-title-pill">
          <span className="brand-title">小糯 · 你的AI护肤助手</span>
        </div>

        <button
          type="button"
          className="interactive-btn lg-capsule nav-memory-btn"
          aria-label="小糯记住了我"
          onClick={() => setDrawerOpen(true)}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
            <rect x="3" y="5" width="18" height="16" rx="3" />
            <path strokeLinecap="round" d="M8 3v4M16 3v4M3 10h18" />
            <path d="M12 17.2l-2.1-1.9a1.5 1.5 0 012.1-2.1 1.5 1.5 0 012.1 2.1L12 17.2z" fill="#FF8A80" stroke="none" />
          </svg>
          {memories.length > 0 && <span className="nav-memory-dot">{memories.length}</span>}
        </button>
      </header>

      <main
        ref={containerRef}
        className="chat-scroll-container"
        onScroll={handleScroll}
      >
        <NuonuoAvatar
          emotion={emotion}
          compact={isKeyboardOpen}
        />

          {!isHydrating && messages.length === 0 && (
        <div className="lg-capsule empty-chat-hint">
          拍照识别成分表、自拍记录肤况，或直接输入你想搭配的护肤品名称
        </div>
        )}

        {messages.map((msg, idx) => {
          const isLastStreamingMsg = streamPhase === 'streaming' && idx >= messages.length - 2;
          return (
            <MessageBubble
              key={msg.id}
              message={msg}
              isStreaming={isLastStreamingMsg}
              onCopySuccess={showToast}
            />
          );
        })}

        {streamPhase === 'error' && streamError && (
          <div className="warning-card is-caution">
            <div className="warning-header">{streamError}</div>
            {lastRequestRef.current && (
              <button
                type="button"
                className="interactive-btn warning-retry-btn"
                onClick={handleRetryStream}
              >
                点击重试
              </button>
            )}
          </div>
        )}
      </main>

      {showNewMessagePill && (
        <button
          type="button"
          className="interactive-btn new-msg-pill"
          onClick={scrollToBottom}
        >
          收到新消息 ↓
        </button>
      )}

      <FloatingDock
        keyboardOffset={keyboardOffset}
        disabled={streamPhase === 'streaming'}
        onOpenCamera={() => setCameraOpen(true)}
        onSendText={(text) => dispatchPrompt(text)}
        onSendVoice={handleSendVoice}
        onVoiceError={showToast}
      />

      <QuickMenuSheet
        open={menuOpen}
        theme={theme}
        onClose={() => setMenuOpen(false)}
        onChangeTheme={(next) => {
          setTheme(next);
          const label = next === 'sky' ? '晴空水雾' : next === 'peach' ? '蜜桃暖阳' : '静谧深夜';
          showToast(`已切换${label}`);
        }}
        onResetChat={handleResetChat}
      />

      <CameraActionSheet
        open={cameraOpen}
        onClose={() => setCameraOpen(false)}
        onFileSelected={handleFileSelected}
      />

      <MemoryDrawer
        open={drawerOpen}
        items={memories}
        onClose={() => setDrawerOpen(false)}
        onUpdateItem={handleUpdateMemory}
        onDeleteItem={handleDeleteMemory}
        onClearAll={handleClearMemories}
      />
    </div>
  );
}
