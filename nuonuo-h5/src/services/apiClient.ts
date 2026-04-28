const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') ?? '';
const SESSION_STORAGE_KEY = 'nuonuo_session_id';
const DEFAULT_TIMEOUT_MS = 15000;

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export function buildApiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}

export function getOrCreateSessionId(): string {
  try {
    const existing = window.sessionStorage.getItem(SESSION_STORAGE_KEY);
    if (existing) {
      return existing;
    }
    const generated =
      typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
        ? crypto.randomUUID()
        : `sess_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
    window.sessionStorage.setItem(SESSION_STORAGE_KEY, generated);
    return generated;
  } catch {
    return `sess_${Date.now()}`;
  }
}

export function resetSessionId(): string {
  try {
    window.sessionStorage.removeItem(SESSION_STORAGE_KEY);
  } catch {
    // 忽略无痕模式等存储受限场景
  }
  return getOrCreateSessionId();
}

export async function requestJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const controller = new AbortController();
  const timerId = window.setTimeout(() => controller.abort(), DEFAULT_TIMEOUT_MS);

  if (init.signal) {
    init.signal.addEventListener('abort', () => controller.abort(), { once: true });
  }

  try {
    const headers = new Headers(init.headers);
    if (!(init.body instanceof FormData) && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }

    const response = await fetch(buildApiUrl(path), {
      ...init,
      headers,
      signal: controller.signal
    });

    if (!response.ok) {
      let errMsg = `请求失败 (${response.status})`;
      try {
        const body = (await response.json()) as { message?: string };
        if (body.message) {
          errMsg = body.message;
        }
      } catch {
        // 非 JSON 错误响应保留状态码提示
      }
      throw new ApiError(errMsg, response.status);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return (await response.json()) as T;
  } finally {
    window.clearTimeout(timerId);
  }
}
