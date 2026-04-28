import { useCallback, useEffect, useRef, useState } from 'react';
import { ChatMessage, ConflictWarningPayload, RoutineCardPayload } from '../types/chat';
import { triggerTapHaptic } from '../utils/haptics';

interface MessageBubbleProps {
  message: ChatMessage;
  isStreaming?: boolean;
  onCopySuccess?: (toastText: string) => void;
}

export function MessageBubble({ message, isStreaming = false, onCopySuccess }: MessageBubbleProps) {
  if (message.role === 'user') {
    return (
      <div className="msg-row is-user">
        <div className="msg-stack" style={{ alignItems: 'flex-end' }}>
          {message.imagePreviewUrl && (
            <div className={`msg-image-card ${isStreaming ? 'is-scanning' : ''}`}>
              <img src={message.imagePreviewUrl} alt={message.imageTag ?? '上传图片'} />
              {isStreaming && <div className="msg-scan-beam" aria-hidden="true" />}
              {message.imageTag && <span className="msg-image-tag">{message.imageTag}</span>}
            </div>
          )}
          <div className="msg-bubble is-user selectable-text">{message.content}</div>
        </div>
      </div>
    );
  }

  const showTypingDots = isStreaming && !message.content;

  return (
    <div className="msg-row">
      <div className="msg-stack">
        {showTypingDots ? (
          <div className="msg-bubble is-assistant typing-bubble" aria-label="小糯正在思考">
            <span className="typing-dot" />
            <span className="typing-dot" />
            <span className="typing-dot" />
          </div>
        ) : (
          message.content && (
            <div className="msg-bubble is-assistant selectable-text">
              {message.content}
              {isStreaming && <span className="stream-cursor" aria-hidden="true" />}
            </div>
          )
        )}
        {message.warningCard && <GroundingWarningCard card={message.warningCard} />}
        {message.routineCard && (
          <RoutineCard card={message.routineCard} onCopySuccess={onCopySuccess} />
        )}
      </div>
    </div>
  );
}

function GroundingWarningCard({ card }: { card: ConflictWarningPayload }) {
  const isForbidden = card.level === 'STRICT_FORBIDDEN';
  return (
    <div className={`warning-card ${isForbidden ? 'is-forbidden' : 'is-caution'}`}>
      <div className="warning-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2L1 21h22L12 2zm1 14h-2v2h2v-2zm0-6h-2v4h2v-4z" />
        </svg>
        <span>{card.title}</span>
      </div>
      <div className="warning-tags">
        {(card.ingredients ?? []).map((item) => (
          <span key={item} className="warning-tag">
            {item}
          </span>
        ))}
      </div>
      <p className="warning-body">{card.mechanism}</p>
      <p className="warning-advice">{card.advice}</p>
    </div>
  );
}

function RoutineCard({
  card,
  onCopySuccess
}: {
  card: RoutineCardPayload;
  onCopySuccess?: (toastText: string) => void;
}) {
  const [copied, setCopied] = useState(false);
  const copyTimerRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (copyTimerRef.current !== null) {
        window.clearTimeout(copyTimerRef.current);
      }
    };
  }, []);

  const handleCopy = useCallback(async () => {
    triggerTapHaptic();
    const amSteps = card.amSteps ?? [];
    const pmSteps = card.pmSteps ?? [];
    const lines = [
      `【${card.title}】`,
      '早间：' + amSteps.map((s) => `${s.order}.${s.action}`).join(' → '),
      '晚间：' + pmSteps.map((s) => `${s.order}.${s.action}`).join(' → ')
    ].join('\n');

    try {
      await navigator.clipboard.writeText(lines);
      setCopied(true);
      onCopySuccess?.('便签已复制');
      if (copyTimerRef.current !== null) {
        window.clearTimeout(copyTimerRef.current);
      }
      copyTimerRef.current = window.setTimeout(() => setCopied(false), 1800);
    } catch {
      onCopySuccess?.('复制失败，请长按文本复制');
    }
  }, [card, onCopySuccess]);

  return (
    <div className="routine-card">
      <div className="routine-head">
        <div className="routine-title-row">
          <span className="routine-peach-dot" aria-hidden="true" />
          <span className="routine-title">{card.title}</span>
        </div>
        <button type="button" className="interactive-btn routine-copy-btn" onClick={handleCopy}>
          {copied ? '已复制' : '复制'}
        </button>
      </div>

      <div className="routine-columns">
        <div className="routine-col is-am">
          <div className="routine-col-head is-am">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="4" />
              <path strokeLinecap="round" d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2" />
            </svg>
            <span>早间</span>
          </div>
          <div className="routine-step-list">
            {card.amSteps.map((step) => (
              <div key={step.order} className="routine-step-item">
                <span className="routine-step-num is-am">{step.order}</span>
                <span className="routine-step-action">{step.action}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="routine-col is-pm">
          <div className="routine-col-head is-pm">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
              <path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" />
            </svg>
            <span>晚间</span>
          </div>
          <div className="routine-step-list">
            {card.pmSteps.map((step) => (
              <div key={step.order} className="routine-step-item">
                <span className="routine-step-num is-pm">{step.order}</span>
                <span className="routine-step-action">{step.action}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
