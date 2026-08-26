/**
 * 时钟 hook（2026-08-25 · 从驾驶舱 useCockpitData 抽出，下沉 packages/ui）.
 *
 * 大屏顶栏秒级时钟：按 intervalMs 更新，format 缺省 HH:mm:ss。
 */
import { useEffect, useState } from 'react';
import { formatClock } from '../datav/risk';

export function useClock(
  intervalMs = 1000,
  format: (d: Date) => string = formatClock,
): string {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const t = window.setInterval(() => setNow(new Date()), intervalMs);
    return () => window.clearInterval(t);
  }, [intervalMs]);
  return format(now);
}
