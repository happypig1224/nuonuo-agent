import { useCallback, useRef, useState } from 'react';

// 距离底部 80px 内判定为用户处于实时阅读态，允许流式推流自动跟随滚动
const STICKY_THRESHOLD_PX = 80;

export function useStickyScroll() {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const isStickyRef = useRef<boolean>(true);
  const [showNewMessagePill, setShowNewMessagePill] = useState(false);

  const handleScroll = useCallback(() => {
    const el = containerRef.current;
    if (!el) {
      return;
    }
    const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    const atBottom = distanceToBottom <= STICKY_THRESHOLD_PX;
    isStickyRef.current = atBottom;
    if (atBottom) {
      setShowNewMessagePill(false);
    }
  }, []);

  const followStreamIfSticky = useCallback(() => {
    const el = containerRef.current;
    if (!el) {
      return;
    }
    if (isStickyRef.current) {
      el.scrollTo({ top: el.scrollHeight, behavior: 'instant' as ScrollBehavior });
    } else {
      setShowNewMessagePill(true);
    }
  }, []);

  const scrollToBottom = useCallback(() => {
    const el = containerRef.current;
    if (!el) {
      return;
    }
    isStickyRef.current = true;
    setShowNewMessagePill(false);
    el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
  }, []);

  return {
    containerRef,
    showNewMessagePill,
    handleScroll,
    followStreamIfSticky,
    scrollToBottom
  };
}
