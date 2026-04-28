import { UserProfile, UserState, UserStateAction } from '../types/user';
import {
  getAuthToken,
  registerUnauthorizedHandler,
  requestJson,
  resetSessionId,
  setAuthToken
} from './apiClient';

const USER_PROFILE_STORAGE_KEY = 'nuonuo_user_profile';

interface ApiResult<T> {
  code: string;
  message: string;
  data: T;
}

interface UserProfileApiPayload {
  userId: string;
  nickname: string;
  token?: string;
  gmtCreate?: string;
}

type UserStateListener = (state: UserState) => void;
const listeners = new Set<UserStateListener>();

function loadInitialState(): UserState {
  const token = getAuthToken();
  if (!token) {
    return {
      status: 'UNAUTHENTICATED',
      profile: null,
      token: null
    };
  }
  try {
    const rawProfile = window.localStorage.getItem(USER_PROFILE_STORAGE_KEY);
    if (rawProfile) {
      const parsed = JSON.parse(rawProfile) as UserProfile;
      if (parsed && parsed.userId) {
        return {
          status: 'AUTHENTICATED',
          profile: parsed,
          token
        };
      }
    }
  } catch {
    // 忽略本地存储读取异常
  }
  return {
    status: 'AUTHENTICATED',
    profile: null,
    token
  };
}

let currentState: UserState = loadInitialState();

function persistState(state: UserState): void {
  setAuthToken(state.token);
  try {
    if (state.profile) {
      window.localStorage.setItem(USER_PROFILE_STORAGE_KEY, JSON.stringify(state.profile));
    } else {
      window.localStorage.removeItem(USER_PROFILE_STORAGE_KEY);
    }
  } catch {
    // 忽略无痕模式等存储受限场景
  }
}

export function reduceUserState(state: UserState, action: UserStateAction): UserState {
  switch (action.type) {
    case 'START_AUTH':
      return {
        ...state,
        status: 'AUTHENTICATING',
        errorMessage: undefined
      };
    case 'AUTH_SUCCESS':
      return {
        status: 'AUTHENTICATED',
        profile: action.profile,
        token: action.token,
        errorMessage: undefined
      };
    case 'AUTH_FAILURE':
      return {
        ...state,
        status: 'UNAUTHENTICATED',
        errorMessage: action.message
      };
    case 'TOKEN_EXPIRED':
      return {
        status: 'EXPIRED',
        profile: null,
        token: null,
        errorMessage: action.message ?? '登录状态已过期，请重新登录'
      };
    case 'PROFILE_UPDATED':
      return {
        ...state,
        profile: action.profile,
        errorMessage: undefined
      };
    case 'LOGOUT':
      return {
        status: 'UNAUTHENTICATED',
        profile: null,
        token: null,
        errorMessage: undefined
      };
    default:
      return state;
  }
}

export function getUserState(): UserState {
  return currentState;
}

export function dispatchUserAction(action: UserStateAction): UserState {
  const prevUserId = currentState.profile?.userId;
  currentState = reduceUserState(currentState, action);
  persistState(currentState);

  if (prevUserId !== currentState.profile?.userId) {
    resetSessionId();
  }

  listeners.forEach((listener) => listener(currentState));
  return currentState;
}

export function subscribeUserState(listener: UserStateListener): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

registerUnauthorizedHandler(() => {
  dispatchUserAction({ type: 'TOKEN_EXPIRED' });
});

export async function registerWithAccount(
  account: string,
  password: string,
  confirmPassword: string
): Promise<UserProfile> {
  dispatchUserAction({ type: 'START_AUTH' });
  try {
    const res = await requestJson<ApiResult<UserProfileApiPayload>>('/api/v1/user/register', {
      method: 'POST',
      body: JSON.stringify({ account, password, confirmPassword })
    });
    const data = res.data;
    if (!data?.token || !data?.userId) {
      throw new Error('注册响应缺少必要凭据');
    }
    const profile: UserProfile = {
      userId: data.userId,
      nickname: data.nickname,
      gmtCreate: data.gmtCreate
    };
    dispatchUserAction({ type: 'AUTH_SUCCESS', profile, token: data.token });
    return profile;
  } catch (err) {
    const message = err instanceof Error ? err.message : '注册失败，请稍后重试';
    dispatchUserAction({ type: 'AUTH_FAILURE', message });
    throw err;
  }
}

export async function loginWithAccount(account: string, password: string): Promise<UserProfile> {
  dispatchUserAction({ type: 'START_AUTH' });
  try {
    const res = await requestJson<ApiResult<UserProfileApiPayload>>('/api/v1/user/login', {
      method: 'POST',
      body: JSON.stringify({ account, password })
    });
    const data = res.data;
    if (!data?.token || !data?.userId) {
      throw new Error('登录响应缺少必要凭据');
    }
    const profile: UserProfile = {
      userId: data.userId,
      nickname: data.nickname,
      gmtCreate: data.gmtCreate
    };
    dispatchUserAction({ type: 'AUTH_SUCCESS', profile, token: data.token });
    return profile;
  } catch (err) {
    const message = err instanceof Error ? err.message : '登录失败，请稍后重试';
    dispatchUserAction({ type: 'AUTH_FAILURE', message });
    throw err;
  }
}

export async function fetchCurrentUserProfile(): Promise<UserProfile> {
  const res = await requestJson<ApiResult<UserProfileApiPayload>>('/api/v1/user/profile');
  const profile: UserProfile = {
    userId: res.data.userId,
    nickname: res.data.nickname,
    gmtCreate: res.data.gmtCreate
  };
  dispatchUserAction({ type: 'PROFILE_UPDATED', profile });
  return profile;
}

export async function updateNickname(nickname: string): Promise<UserProfile> {
  const res = await requestJson<ApiResult<UserProfileApiPayload>>('/api/v1/user/profile', {
    method: 'PATCH',
    body: JSON.stringify({ nickname })
  });
  const profile: UserProfile = {
    userId: res.data.userId,
    nickname: res.data.nickname,
    gmtCreate: res.data.gmtCreate
  };
  dispatchUserAction({ type: 'PROFILE_UPDATED', profile });
  return profile;
}

export async function logoutAccount(): Promise<void> {
  try {
    await requestJson<ApiResult<void>>('/api/v1/user/logout', { method: 'POST' });
  } finally {
    dispatchUserAction({ type: 'LOGOUT' });
  }
}

export async function deleteCurrentAccount(): Promise<void> {
  await requestJson<ApiResult<void>>('/api/v1/user/account', { method: 'DELETE' });
  dispatchUserAction({ type: 'LOGOUT' });
}
