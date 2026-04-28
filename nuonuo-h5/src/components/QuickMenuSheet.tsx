import { PointerEvent, useRef, useState } from 'react';
import { useSheetPresence } from '../hooks/useSheetPresence';
import { triggerTapHaptic } from '../utils/haptics';

export type BackgroundTheme = 'sky' | 'peach' | 'night';

interface QuickMenuSheetProps {
  open: boolean;
  theme: BackgroundTheme;
  onClose: () => void;
  onChangeTheme: (next: BackgroundTheme) => void;
  onResetChat: () => void;
}

const THEME_OPTIONS: Array<{ id: BackgroundTheme; label: string }> = [
  { id: 'peach', label: '蜜桃暖阳' },
  { id: 'sky', label: '晴空水雾' },
  { id: 'night', label: '静谧深夜' }
];

const SHEET_DISMISS_PX = 76;

export function QuickMenuSheet({
  open,
  theme,
  onClose,
  onChangeTheme,
  onResetChat
}: QuickMenuSheetProps) {
  const { mounted, visible } = useSheetPresence(open, 360);
  const [dragY, setDragY] = useState(0);
  const dragStartYRef = useRef<number | null>(null);

  if (!mounted) {
    return null;
  }

  const handleClose = () => {
    setDragY(0);
    onClose();
  };

  const handlePointerDown = (e: PointerEvent<HTMLDivElement>) => {
    dragStartYRef.current = e.clientY;
  };

  const handlePointerMove = (e: PointerEvent<HTMLDivElement>) => {
    if (dragStartYRef.current === null) {
      return;
    }
    setDragY(Math.max(0, e.clientY - dragStartYRef.current));
  };

  const handlePointerUp = () => {
    if (dragStartYRef.current === null) {
      return;
    }
    dragStartYRef.current = null;
    if (dragY > SHEET_DISMISS_PX) {
      handleClose();
    } else {
      setDragY(0);
    }
  };

  return (
    <div
      className={`sheet-backdrop ${visible ? 'is-visible' : ''}`}
      onClick={handleClose}
    >
      <div
        className={`action-sheet ${visible ? 'is-visible' : ''}`}
        style={dragY > 0 ? { transform: `translateY(${dragY}px)`, transition: 'none' } : undefined}
        onClick={(e) => e.stopPropagation()}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
        aria-label="快捷功能菜单"
      >
        <div className="sheet-grabber" aria-hidden="true" />
        <div className="menu-sheet-header">
          <span className="menu-sheet-title">个性化与会话</span>
        </div>

        <div className="ios-inset-group">
          <div className="ios-row">
            <span className="ios-row-title">背景氛围</span>
            <div className="theme-segmented" role="radiogroup" aria-label="背景氛围">
              {THEME_OPTIONS.map((opt) => (
                <button
                  key={opt.id}
                  type="button"
                  role="radio"
                  aria-checked={theme === opt.id}
                  className={`theme-seg-btn ${theme === opt.id ? 'is-active' : ''}`}
                  onClick={() => {
                    triggerTapHaptic();
                    onChangeTheme(opt.id);
                  }}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          <button
            type="button"
            className="interactive-btn ios-row"
            onClick={() => {
              triggerTapHaptic();
              onResetChat();
              handleClose();
            }}
          >
            <span className="ios-row-title">开启新对话</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ opacity: 0.45 }}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 18l6-6-6-6" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
}
