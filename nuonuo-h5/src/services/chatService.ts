import {
  ChatMessage,
  ConflictWarningPayload,
  EmotionState,
  MediaUploadScene,
  RoutineCardPayload
} from '../types/chat';
import { MemoryItem } from '../types/memory';
import { buildApiUrl, getAuthHeaders, requestJson } from './apiClient';

export interface StreamCallbacks {
  onEmotion: (emotion: EmotionState) => void;
  onDelta: (chunk: string) => void;
  onWarningCard: (card: ConflictWarningPayload) => void;
  onRoutineCard: (card: RoutineCardPayload) => void;
  onMemoryExtracted?: (memory: MemoryItem) => void;
  onDone: () => void;
  onError: (message: string) => void;
}

export interface ChatStreamRequest {
  sessionId: string;
  prompt: string;
  imageFileId?: string;
  imageBase64?: string;
  imageTag?: string;
}

export interface MediaUploadResult {
  fileId: string;
  url: string;
}

export async function fetchChatHistory(sessionId: string): Promise<ChatMessage[]> {
  return requestJson<ChatMessage[]>(`/api/v1/chat/history?sessionId=${encodeURIComponent(sessionId)}`);
}

export async function uploadMedia(file: File, scene: MediaUploadScene): Promise<MediaUploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('scene', scene);

  return requestJson<MediaUploadResult>('/api/v1/media/upload', {
    method: 'POST',
    body: formData
  });
}

export async function transcribeVoice(audioBlob: Blob): Promise<{ text: string }> {
  const formData = new FormData();
  formData.append('audio', audioBlob, 'voice.webm');

  return requestJson<{ text: string }>('/api/v1/voice/transcribe', {
    method: 'POST',
    body: formData
  });
}

function dispatchSseEvent(eventName: string, rawData: string, callbacks: StreamCallbacks): void {
  if (!rawData) {
    return;
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(rawData);
  } catch {
    if (eventName === 'delta') {
      callbacks.onDelta(rawData);
    }
    return;
  }

  switch (eventName) {
    case 'emotion': {
      const payload = parsed as { emotion?: EmotionState };
      if (payload.emotion) {
        callbacks.onEmotion(payload.emotion);
      }
      break;
    }
    case 'delta': {
      const payload = parsed as { content?: string; delta?: string };
      const chunk = payload.content ?? payload.delta;
      if (chunk) {
        callbacks.onDelta(chunk);
      }
      break;
    }
    case 'warning_card':
      callbacks.onWarningCard(parsed as ConflictWarningPayload);
      break;
    case 'routine_card':
      callbacks.onRoutineCard(parsed as RoutineCardPayload);
      break;
    case 'memory_extracted':
      callbacks.onMemoryExtracted?.(parsed as MemoryItem);
      break;
    case 'error': {
      const payload = parsed as { message?: string };
      callbacks.onError(payload.message ?? '服务响应异常，请重试');
      break;
    }
    case 'done':
      callbacks.onDone();
      break;
    default:
      break;
  }
}

export function startChatSseStream(
  request: ChatStreamRequest,
  callbacks: StreamCallbacks
): () => void {
  const controller = new AbortController();

  const run = async () => {
    try {
      const response = await fetch(buildApiUrl('/api/v1/chat/stream'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
          ...getAuthHeaders()
        },
        body: JSON.stringify(request),
        signal: controller.signal
      });

      if (!response.ok || !response.body) {
        throw new Error(`流式连接失败 (${response.status})`);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';
      let doneDispatched = false;

      while (true) {
        const { value, done } = await reader.read();
        if (done) {
          break;
        }
        buffer += decoder.decode(value, { stream: true });

        const frames = buffer.split(/\r?\n\r?\n/);
        buffer = frames.pop() ?? '';

        for (const frame of frames) {
          const lines = frame.split(/\r?\n/);
          let eventName = 'delta';
          const dataLines: string[] = [];

          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventName = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
              dataLines.push(line.slice(5).trim());
            }
          }

          if (eventName === 'done') {
            doneDispatched = true;
          }
          dispatchSseEvent(eventName, dataLines.join('\n'), callbacks);
        }
      }

      if (!doneDispatched && !controller.signal.aborted) {
        callbacks.onDone();
      }
    } catch (err) {
      if (err instanceof DOMException && err.name === 'AbortError') {
        return;
      }
      const message = err instanceof Error ? err.message : '网络连接异常，请点击重试';
      callbacks.onError(message);
    }
  };

  void run();
  return () => controller.abort();
}
