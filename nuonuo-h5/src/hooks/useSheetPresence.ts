import { useEffect, useState } from 'react';

/**
 * 管理抽屉/底部面板的挂载与退场动画生命周期，确保快速来回切换时无状态残留。
 */
export function useSheetPresence(open: boolean, exitDurationMs = 320): {
  mounted: boolean;
  visible: boolean;
} {
  const [mounted, setMounted] = useState(open);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (open) {
      let cancelled = false;
      setMounted(true);
        let raf2 = 0;
        const raf1 = window.requestAnimationFrame(() => {
          raf2 = window.requestAnimationFrame(() => {
            if (!cancelled) {
              setVisible(true);
            }
          });
        });
        return () => {
          cancelled = true;
          window.cancelAnimationFrame(raf1);
          window.cancelAnimationFrame(raf2);
        };
      }

    setVisible(false);
      const timer = window.setTimeout(() => setMounted(false), exitDurationMs);
      return () => window.clearTimeout(timer);
    }, [open, exitDurationMs]);

  return { mounted, visible };
}
