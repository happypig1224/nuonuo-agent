import { useCallback, useState } from 'react';
import { EmotionState } from '../types/chat';
import { triggerTapHaptic } from '../utils/haptics';

interface NuonuoAvatarProps {
  emotion: EmotionState;
  compact: boolean;
}

function getTimeBasedGreeting(): string {
  const hour = new Date().getHours();
  if (hour >= 5 && hour < 12) {
    return '早呀，今天想聊点什么？';
  }
  if (hour >= 12 && hour < 18) {
    return '午安，今天护肤状态怎么样？';
  }
  if (hour >= 18) {
    return '晚上好，来聊聊今晚的护肤安排吧';
  }
  return '夜深啦，简简单单护肤就早点休息';
}

const POKE_LINES = [
  '我在听呢',
  '随时告诉我你的肤况',
  '精简温和护肤就好'
];

export function NuonuoAvatar({ emotion, compact }: NuonuoAvatarProps) {
  const [pokeIndex, setPokeIndex] = useState<number | null>(null);

  const handleTap = useCallback(() => {
    triggerTapHaptic();
    setPokeIndex((prev) => (prev === null ? 0 : (prev + 1) % POKE_LINES.length));
  }, []);

  const greetingText =
    pokeIndex !== null
      ? POKE_LINES[pokeIndex]
      : emotion === 'NORMAL'
        ? getTimeBasedGreeting()
        : emotion === 'CONCERNED'
          ? '别慌，小糯陪你稳住'
          : emotion === 'ALERT'
            ? '先停手，这两个成分会冲突'
            : '真棒，屏障状态变稳啦';

  return (
    <section className={`avatar-stage ${compact ? 'is-compact' : ''}`} aria-label="小糯形象">
      <div className="avatar-hero-row">
        <div
          className={`avatar-character ${emotion === 'ALERT' ? 'is-alert' : ''}`}
          onClick={handleTap}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              handleTap();
            }
          }}
        >
          <img className="avatar-img" src="/nuonuo-avatar.png" alt="小糯" draggable={false} />
        </div>

        <div className="lg-capsule avatar-speech-bubble">
          <p className="avatar-speech-text">{greetingText}</p>
        </div>
      </div>
    </section>
  );
}
