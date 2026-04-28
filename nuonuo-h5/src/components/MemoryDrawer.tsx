import { PointerEvent, useRef, useState } from 'react';
import { useSheetPresence } from '../hooks/useSheetPresence';
import { MEMORY_CATEGORY_LABELS, MemoryCategory, MemoryItem } from '../types/memory';
import { triggerTapHaptic } from '../utils/haptics';

interface MemoryDrawerProps {
  open: boolean;
  items: MemoryItem[];
  onClose: () => void;
  onUpdateItem: (id: string, nextValue: string) => void;
  onDeleteItem: (id: string) => void;
  onClearAll: () => void;
}

const CATEGORIES: MemoryCategory[] = ['SKIN_TYPE', 'ALLERGY', 'PREFERENCE'];

// 向右拖拽抽屉超过 72px 触发原生跟手收纳
const DRAWER_DISMISS_PX = 72;

export function MemoryDrawer({
  open,
  items,
  onClose,
  onUpdateItem,
  onDeleteItem,
  onClearAll
}: MemoryDrawerProps) {
  const { mounted, visible } = useSheetPresence(open, 360);
  const [confirmClear, setConfirmClear] = useState(false);
  const [dragX, setDragX] = useState(0);
  const dragStartXRef = useRef<number | null>(null);

  if (!mounted) {
    return null;
  }

  const handleClose = () => {
    setConfirmClear(false);
    setDragX(0);
    onClose();
  };

  const handleDrawerPointerDown = (e: PointerEvent<HTMLElement>) => {
    // 仅在抽屉左侧边缘区域触发右滑关闭，避免与内部卡片左滑删除冲突
    const rect = e.currentTarget.getBoundingClientRect();
    if (e.clientX - rect.left <= 32) {
      dragStartXRef.current = e.clientX;
    }
  };

  const handleDrawerPointerMove = (e: PointerEvent<HTMLElement>) => {
    if (dragStartXRef.current === null) {
      return;
    }
    const delta = Math.max(0, e.clientX - dragStartXRef.current);
    setDragX(delta);
  };

  const handleDrawerPointerUp = () => {
    if (dragStartXRef.current === null) {
      return;
    }
    dragStartXRef.current = null;
    if (dragX > DRAWER_DISMISS_PX) {
      handleClose();
    } else {
      setDragX(0);
    }
  };

  return (
    <div
      className={`drawer-backdrop ${visible ? 'is-visible' : ''}`}
      onClick={handleClose}
    >
      <aside
        className={`memory-drawer ${visible ? 'is-visible' : ''}`}
        style={dragX > 0 ? { transform: `translateX(${dragX}px)`, transition: 'none' } : undefined}
        onClick={(e) => e.stopPropagation()}
        onPointerDown={handleDrawerPointerDown}
        onPointerMove={handleDrawerPointerMove}
        onPointerUp={handleDrawerPointerUp}
        onPointerCancel={handleDrawerPointerUp}
        aria-label="小糯记住了我"
      >
        <div className="drawer-header">
          <div className="drawer-title-group">
            <h2 className="drawer-title">小糯记住了我</h2>
            <span className="drawer-count">{items.length}</span>
          </div>
          <button
            type="button"
            className="interactive-btn sheet-close-btn"
            aria-label="收起"
            onClick={handleClose}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
              <path strokeLinecap="round" d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>
        </div>

        <div className="drawer-list">
          {items.length === 0 ? (
            <div className="memory-empty">
              暂无护肤记忆，和小糯聊聊你的肤质或忌口成分吧
            </div>
          ) : (
            CATEGORIES.map((cat) => {
              const group = items.filter((item) => item.category === cat);
              if (group.length === 0) {
                return null;
              }
              return (
                <div key={cat}>
                  <div className="memory-group-title">{MEMORY_CATEGORY_LABELS[cat]}</div>
                  <div className="memory-group-box">
                    {group.map((item) => (
                      <SwipeableMemoryCard
                        key={item.id}
                        item={item}
                        onUpdate={onUpdateItem}
                        onDelete={onDeleteItem}
                      />
                    ))}
                  </div>
                </div>
              );
            })
          )}
        </div>

        {items.length > 0 && (
          <div className="drawer-footer">
            {confirmClear ? (
              <div className="drawer-confirm-row">
                <button
                  type="button"
                  className="interactive-btn sheet-cancel"
                  onClick={() => setConfirmClear(false)}
                >
                  取消
                </button>
                <button
                  type="button"
                  className="interactive-btn drawer-clear-btn is-danger"
                  onClick={() => {
                    triggerTapHaptic();
                    onClearAll();
                    setConfirmClear(false);
                  }}
                >
                  确认清空
                </button>
              </div>
            ) : (
              <div className="ios-inset-group">
                <button
                  type="button"
                  className="interactive-btn drawer-clear-btn"
                  onClick={() => setConfirmClear(true)}
                >
                  清空全部记忆
                </button>
              </div>
            )}
          </div>
        )}
      </aside>
    </div>
  );
}

interface SwipeableMemoryCardProps {
  item: MemoryItem;
  onUpdate: (id: string, nextValue: string) => void;
  onDelete: (id: string) => void;
}

function SwipeableMemoryCard({ item, onUpdate, onDelete }: SwipeableMemoryCardProps) {
  const [offsetX, setOffsetX] = useState(0);
  const [editing, setEditing] = useState(false);
  const [editValue, setEditValue] = useState(item.profileValue);
  const startXRef = useRef(0);
  const draggingRef = useRef(false);
  const justSwipedRef = useRef(false);

  const handlePointerDown = (e: PointerEvent<HTMLDivElement>) => {
    if (editing) {
      return;
    }
    startXRef.current = e.clientX;
    draggingRef.current = true;
    justSwipedRef.current = false;
  };

  const handlePointerMove = (e: PointerEvent<HTMLDivElement>) => {
    if (!draggingRef.current || editing) {
      return;
    }
    const delta = e.clientX - startXRef.current;
    if (Math.abs(delta) > 8) {
      justSwipedRef.current = true;
    }
    if (delta < 0) {
      setOffsetX(Math.max(-72, delta));
    } else {
      setOffsetX(0);
    }
  };

  const handlePointerUp = () => {
    if (!draggingRef.current) {
      return;
    }
    draggingRef.current = false;
    setOffsetX((prev) => (prev < -30 ? -72 : 0));
  };

  const commitEdit = () => {
    const trimmed = editValue.trim();
    if (trimmed && trimmed !== item.profileValue) {
      onUpdate(item.id, trimmed);
    }
    setEditing(false);
  };

  return (
    <div className="memory-swipe-item">
      <button
        type="button"
        className={`interactive-btn memory-delete-action ${offsetX < -4 ? 'is-exposed' : ''}`}
        onClick={() => {
          triggerTapHaptic();
          onDelete(item.id);
        }}
      >
        删除
      </button>
      <div
        className="memory-card"
        style={{ transform: `translateX(${offsetX}px)` }}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
      >
        {editing ? (
          <input
            type="text"
            className="memory-edit-input"
            value={editValue}
            autoFocus
            onChange={(e) => setEditValue(e.target.value)}
            onBlur={commitEdit}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                commitEdit();
              }
            }}
          />
        ) : (
          <div
            className="memory-card-val"
            onClick={() => {
              if (justSwipedRef.current) {
                justSwipedRef.current = false;
                return;
              }
              if (offsetX < 0) {
                setOffsetX(0);
                return;
              }
              setEditValue(item.profileValue);
              setEditing(true);
            }}
          >
            {item.profileValue}
          </div>
        )}
        <div className="memory-card-meta">
          <span>置信度 {Math.round(item.confidence * 100)}%</span>
          <span>{item.updatedAt}</span>
        </div>
      </div>
    </div>
  );
}
