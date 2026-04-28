import { MemoryItem } from '../types/memory';
import { requestJson } from './apiClient';

export async function fetchMemories(): Promise<MemoryItem[]> {
  return requestJson<MemoryItem[]>('/api/v1/memories');
}

export async function updateMemory(id: string, profileValue: string): Promise<MemoryItem> {
  return requestJson<MemoryItem>(`/api/v1/memories/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: JSON.stringify({ profileValue })
  });
}

export async function deleteMemory(id: string): Promise<void> {
  await requestJson<void>(`/api/v1/memories/${encodeURIComponent(id)}`, {
    method: 'DELETE'
  });
}

export async function clearAllMemories(): Promise<void> {
  await requestJson<void>('/api/v1/memories', {
    method: 'DELETE'
  });
}
