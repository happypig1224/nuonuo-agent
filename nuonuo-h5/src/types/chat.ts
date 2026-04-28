export type EmotionState = 'NORMAL' | 'CONCERNED' | 'ALERT' | 'CELEBRATE';

export type ConflictLevel = 'STRICT_FORBIDDEN' | 'CAUTION';

export type StreamPhase = 'idle' | 'streaming' | 'error' | 'done';

export type MediaUploadScene = 'selfie' | 'ingredient' | 'album';

export interface ConflictWarningPayload {
  level: ConflictLevel;
  title: string;
  ingredients: string[];
  mechanism: string;
  advice: string;
}

export interface RoutineStep {
  order: number;
  action: string;
  caution: string;
}

export interface RoutineCardPayload {
  title: string;
  badge: string;
  amSteps: RoutineStep[];
  pmSteps: RoutineStep[];
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  imagePreviewUrl?: string;
  imageTag?: string;
  warningCard?: ConflictWarningPayload;
  routineCard?: RoutineCardPayload;
  createdAt: number;
}
