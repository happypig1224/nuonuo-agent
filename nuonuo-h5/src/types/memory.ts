export type MemoryCategory = 'SKIN_TYPE' | 'ALLERGY' | 'PREFERENCE';

export interface MemoryItem {
  id: string;
  category: MemoryCategory;
  profileKey: string;
  profileValue: string;
  confidence: number;
  updatedAt: string;
}

export const MEMORY_CATEGORY_LABELS: Record<MemoryCategory, string> = {
  SKIN_TYPE: '肤质基线',
  ALLERGY: '成分禁忌',
  PREFERENCE: '使用偏好'
};
