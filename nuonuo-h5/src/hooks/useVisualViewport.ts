import { useEffect, useState } from 'react';

interface ViewportState {
  keyboardOffset: number;
  isKeyboardOpen: boolean;
}

// 阈值设为 120px 以过滤移动端浏览器地址栏收起引起的微小视口高度波动
const KEYBOARD_OPEN_THRESHOLD_PX = 120;

export function useVisualViewport(): ViewportState {
  const [state, setState] = useState<ViewportState>({
    keyboardOffset: 0,
    isKeyboardOpen: false
  });

  useEffect(() => {
    const syncAppHeight = () => {
      document.documentElement.style.setProperty('--app-height', `${window.innerHeight}px`);
    };

    syncAppHeight();

    const vv = window.visualViewport;
    if (!vv) {
      window.addEventListener('resize', syncAppHeight);
      return () => window.removeEventListener('resize', syncAppHeight);
    }

    const handleViewportChange = () => {
      const offset = Math.max(0, window.innerHeight - vv.height - vv.offsetTop);
      setState({
        keyboardOffset: offset,
        isKeyboardOpen: offset > KEYBOARD_OPEN_THRESHOLD_PX
      });
    };

    vv.addEventListener('resize', handleViewportChange);
    vv.addEventListener('scroll', handleViewportChange);
    window.addEventListener('resize', syncAppHeight);

    return () => {
      vv.removeEventListener('resize', handleViewportChange);
      vv.removeEventListener('scroll', handleViewportChange);
      window.removeEventListener('resize', syncAppHeight);
    };
  }, []);

  return state;
}
