export type UserAuthStatus = 'UNAUTHENTICATED' | 'AUTHENTICATING' | 'AUTHENTICATED' | 'EXPIRED';

export interface UserProfile {
  userId: string;
  nickname: string;
  gmtCreate?: string;
}

export interface UserState {
  status: UserAuthStatus;
  profile: UserProfile | null;
  token: string | null;
  errorMessage?: string;
}

export type UserStateAction =
  | { type: 'START_AUTH' }
  | { type: 'AUTH_SUCCESS'; profile: UserProfile; token: string }
  | { type: 'AUTH_FAILURE'; message: string }
  | { type: 'TOKEN_EXPIRED'; message?: string }
  | { type: 'PROFILE_UPDATED'; profile: UserProfile }
  | { type: 'LOGOUT' };
