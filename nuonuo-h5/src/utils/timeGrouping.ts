export interface TimeBucketGroup<T> {
  label: string;
  items: T[];
}

/**
 * 将带时间戳的列表按「今天 / 昨天 / 7天内 / 30天内 / YYYY-MM」分组，并按时间倒序排列
 */
export function groupByTimeBucket<T>(
  items: T[],
  getTimestamp: (item: T) => number | string | Date | undefined,
  now: Date = new Date()
): TimeBucketGroup<T>[] {
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const oneDayMs = 24 * 60 * 60 * 1000;

  const sorted = [...items].sort((a, b) => toMillis(getTimestamp(b)) - toMillis(getTimestamp(a)));
  const bucketMap = new Map<string, T[]>();

  for (const item of sorted) {
    const ms = toMillis(getTimestamp(item));
    const label = resolveBucketLabel(ms, startOfToday, oneDayMs);
    const list = bucketMap.get(label) ?? [];
    list.push(item);
    bucketMap.set(label, list);
  }

  return Array.from(bucketMap.entries()).map(([label, groupItems]) => ({
    label,
    items: groupItems
  }));
}

function toMillis(ts: number | string | Date | undefined): number {
  if (ts === undefined || ts === null) {
    return 0;
  }
  if (typeof ts === 'number') {
    return ts;
  }
  if (ts instanceof Date) {
    return ts.getTime();
  }
  const parsed = Date.parse(ts);
  return Number.isNaN(parsed) ? 0 : parsed;
}

function resolveBucketLabel(ms: number, startOfToday: number, oneDayMs: number): string {
  if (ms >= startOfToday) {
    return '今天';
  }
  if (ms >= startOfToday - oneDayMs) {
    return '昨天';
  }
  if (ms >= startOfToday - 7 * oneDayMs) {
    return '7天内';
  }
  if (ms >= startOfToday - 30 * oneDayMs) {
    return '30天内';
  }
  const d = new Date(ms || startOfToday);
  const month = String(d.getMonth() + 1).padStart(2, '0');
  return `${d.getFullYear()}-${month}`;
}
