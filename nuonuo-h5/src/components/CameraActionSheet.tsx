import { ChangeEvent, PointerEvent, useRef, useState } from 'react';
import { useSheetPresence } from '../hooks/useSheetPresence';
import { MediaUploadScene } from '../types/chat';
import { triggerTapHaptic } from '../utils/haptics';

interface CameraActionSheetProps {
  open: boolean;
  onClose: () => void;
  onFileSelected: (file: File, scene: MediaUploadScene, modeLabel: string) => void;
}

const SHEET_DISMISS_PX = 76;

export function CameraActionSheet({ open, onClose, onFileSelected }: CameraActionSheetProps) {
  const { mounted, visible } = useSheetPresence(open, 360);
  const [dragY, setDragY] = useState(0);
  const dragStartYRef = useRef<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const pendingMetaRef = useRef<{ scene: MediaUploadScene; label: string }>({
    scene: 'ingredient',
    label: '成分表识别'
  });

  if (!mounted) {
    return null;
  }

  const handleClose = () => {
    setDragY(0);
    onClose();
  };

  const triggerPicker = (
    captureMode: 'user' | 'environment' | undefined,
    scene: MediaUploadScene,
    label: string
  ) => {
    triggerTapHaptic();
    pendingMetaRef.current = { scene, label };
    const input = fileInputRef.current;
    if (!input) {
      return;
    }
    if (captureMode) {
      input.setAttribute('capture', captureMode);
    } else {
      input.removeAttribute('capture');
    }
    input.click();
  };

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) {
      return;
    }
    onFileSelected(file, pendingMetaRef.current.scene, pendingMetaRef.current.label);
    e.target.value = '';
    handleClose();
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
    <div className={`sheet-backdrop ${visible ? 'is-visible' : ''}`} onClick={handleClose}>
      <div
        className={`action-sheet ${visible ? 'is-visible' : ''}`}
        style={dragY > 0 ? { transform: `translateY(${dragY}px)`, transition: 'none' } : undefined}
        onClick={(e) => e.stopPropagation()}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
      >
        <div className="sheet-grabber" aria-hidden="true" />
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          style={{ display: 'none' }}
          onChange={handleFileChange}
        />
        <div className="ios-inset-group">
          <button
            type="button"
            className="interactive-btn ios-row"
            onClick={() => triggerPicker('user', 'selfie', '面部自拍测肤')}
          >
            <span className="ios-row-title">拍照测肤</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ opacity: 0.45 }}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 18l6-6-6-6" />
            </svg>
          </button>
          <button
            type="button"
            className="interactive-btn ios-row"
            onClick={() => triggerPicker('environment', 'ingredient', '瓶身成分排雷')}
          >
            <span className="ios-row-title">拍成分表 / 梳妆台</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ opacity: 0.45 }}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 18l6-6-6-6" />
            </svg>
          </button>
          <button
            type="button"
            className="interactive-btn ios-row"
            onClick={() => triggerPicker(undefined, 'album', '相册选图')}
          >
            <span className="ios-row-title">从相册选取</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ opacity: 0.45 }}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 18l6-6-6-6" />
            </svg>
          </button>
        </div>
        <button type="button" className="interactive-btn sheet-cancel" onClick={handleClose}>
          取消
        </button>
      </div>
    </div>
  );
}
