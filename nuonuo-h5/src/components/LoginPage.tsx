import { CSSProperties, FormEvent, useState } from 'react';
import { loginWithAccount, registerWithAccount } from '../services/userService';
import { UserState } from '../types/user';
import { triggerTapHaptic } from '../utils/haptics';

interface LoginPageProps {
  userState: UserState;
}

type AuthMode = 'login' | 'register';

const GLASS_INPUT_STYLE: CSSProperties = {
  height: '48px',
  padding: '0 18px',
  borderRadius: '24px',
  border: '0.5px solid rgba(255, 255, 255, 0.38)',
  background: 'rgba(255, 255, 255, 0.24)',
  backdropFilter: 'blur(18px)',
  WebkitBackdropFilter: 'blur(18px)',
  fontSize: '15px',
  color: '#2d2226',
  outline: 'none',
  boxShadow: 'inset 0 1px 0 rgba(255, 255, 255, 0.45), 0 4px 16px rgba(184, 74, 110, 0.04)'
};

export function LoginPage({ userState }: LoginPageProps) {
  const [mode, setMode] = useState<AuthMode>('login');
  const [account, setAccount] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [agreed, setAgreed] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  const isSubmitting = userState.status === 'AUTHENTICATING';

  const switchMode = (nextMode: AuthMode) => {
    triggerTapHaptic();
    setMode(nextMode);
    setLocalError(null);
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const trimmedAccount = account.trim();
    if (!trimmedAccount) {
      setLocalError('请输入手机号或邮箱');
      return;
    }
    if (!password) {
      setLocalError('请输入密码');
      return;
    }
    if (mode === 'register') {
      if (password.length < 6) {
        setLocalError('密码长度不能少于 6 位');
        return;
      }
      if (password !== confirmPassword) {
        setLocalError('两次输入的密码不一致');
        return;
      }
    }
    if (!agreed) {
      setLocalError('请先阅读并同意《用户协议》与《隐私政策》');
      return;
    }

    triggerTapHaptic();
    setLocalError(null);

    if (mode === 'login') {
      void loginWithAccount(trimmedAccount, password).catch((err) => {
        setLocalError(err instanceof Error ? err.message : '登录失败，请稍后重试');
      });
    } else {
      void registerWithAccount(trimmedAccount, password, confirmPassword).catch((err) => {
        setLocalError(err instanceof Error ? err.message : '注册失败，请稍后重试');
      });
    }
  };

  const displayError =
    localError ??
    userState.errorMessage ??
    (userState.status === 'EXPIRED' ? '登录状态已过期，请重新登录' : null);

  return (
    <div
      style={{
        minHeight: '100dvh',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        padding: '48px 24px 32px',
        backgroundImage: "url('/bg-peach-vanity.png')",
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        position: 'relative'
      }}
    >
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background:
            'linear-gradient(180deg, rgba(255, 244, 238, 0.52) 0%, rgba(252, 232, 226, 0.62) 100%)',
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)'
        }}
      />

      <div style={{ position: 'relative', zIndex: 1, marginTop: '12px' }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
          <img
            src="/nuonuo-avatar.png"
            alt="小糯智肤"
            style={{
              width: '80px',
              height: '80px',
              borderRadius: '24px',
              objectFit: 'cover',
              boxShadow:
                '0 14px 32px rgba(184, 74, 110, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.55)',
              border: '0.5px solid rgba(255, 255, 255, 0.4)'
            }}
          />
          <h1
            style={{
              margin: '14px 0 4px',
              fontSize: '22px',
              fontWeight: 700,
              color: '#2d2226',
              letterSpacing: '0.4px'
            }}
          >
            小糯智肤
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: '#7a656c' }}>
            你的私人护肤成分陪伴 · 温和守护每一寸肌肤
          </p>
        </div>

        <div
          style={{
            marginTop: '24px',
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            padding: '4px',
            borderRadius: '999px',
            background: 'rgba(255, 238, 232, 0.28)',
            border: '0.5px solid rgba(255, 255, 255, 0.35)',
            backdropFilter: 'blur(14px)',
            WebkitBackdropFilter: 'blur(14px)'
          }}
        >
          {(['login', 'register'] as AuthMode[]).map((tab) => {
            const active = mode === tab;
            return (
              <button
                key={tab}
                type="button"
                onClick={() => switchMode(tab)}
                style={{
                  height: '36px',
                  borderRadius: '999px',
                  border: active ? '0.5px solid rgba(255, 255, 255, 0.45)' : 'none',
                  background: active ? 'rgba(255, 255, 255, 0.42)' : 'transparent',
                  color: active ? '#7a2845' : '#7a656c',
                  fontSize: '14px',
                  fontWeight: active ? 600 : 500,
                  boxShadow: active ? '0 2px 10px rgba(184, 74, 110, 0.08)' : 'none',
                  cursor: 'pointer'
                }}
              >
                {tab === 'login' ? '密码登录' : '注册账号'}
              </button>
            );
          })}
        </div>

        <form
          onSubmit={handleSubmit}
          style={{
            marginTop: '18px',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px'
          }}
        >
          <input
            type="text"
            value={account}
            onChange={(e) => {
              setAccount(e.target.value);
              if (localError) setLocalError(null);
            }}
            placeholder="请输入手机号或邮箱"
            autoComplete="username"
            style={GLASS_INPUT_STYLE}
          />

          <input
            type="password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              if (localError) setLocalError(null);
            }}
            placeholder={mode === 'login' ? '请输入密码' : '请设置密码（不少于 6 位）'}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            style={GLASS_INPUT_STYLE}
          />

          {mode === 'register' && (
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                if (localError) setLocalError(null);
              }}
              placeholder="请再次输入密码确认"
              autoComplete="new-password"
              style={GLASS_INPUT_STYLE}
            />
          )}

          {displayError && (
            <div
              style={{
                padding: '0 12px',
                fontSize: '12px',
                color: '#c62828',
                lineHeight: 1.4
              }}
            >
              {displayError}
            </div>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="interactive-btn"
            style={{
              height: '48px',
              marginTop: '4px',
              borderRadius: '24px',
              border: '0.5px solid rgba(255, 255, 255, 0.48)',
              background: isSubmitting
                ? 'rgba(244, 143, 177, 0.22)'
                : 'linear-gradient(135deg, rgba(244, 168, 190, 0.42) 0%, rgba(224, 122, 155, 0.34) 100%)',
              backdropFilter: 'blur(18px)',
              WebkitBackdropFilter: 'blur(18px)',
              color: '#7a2845',
              fontSize: '15px',
              fontWeight: 600,
              letterSpacing: '0.6px',
              boxShadow:
                'inset 0 1px 0 rgba(255, 255, 255, 0.55), 0 8px 24px rgba(184, 74, 110, 0.12)'
            }}
          >
            {isSubmitting ? '提交中...' : mode === 'login' ? '登录' : '立即注册'}
          </button>
        </form>
      </div>

      <label
        style={{
          position: 'relative',
          zIndex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '8px',
          fontSize: '12px',
          color: '#7a656c',
          cursor: 'pointer'
        }}
      >
        <input
          type="checkbox"
          checked={agreed}
          onChange={(e) => {
            setAgreed(e.target.checked);
            if (localError) setLocalError(null);
          }}
          style={{ accentColor: '#b84a6e', width: '15px', height: '15px' }}
        />
        <span>我已阅读并同意《用户协议》与《隐私政策》</span>
      </label>
    </div>
  );
}
