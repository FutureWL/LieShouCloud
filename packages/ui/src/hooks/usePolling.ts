/**
 * 轮询 hook（2026-08-25 · 从驾驶舱 useCockpitData 抽出，下沉 packages/ui）.
 *
 * 大屏/看板通用数据刷新骨架：立即执行一次 + 按间隔重复。
 * - 静默失败由调用方处理（fn 内自行 try/catch，保留旧数据不打断值班）
 * - fn 变化不重建定时器（ref 持最新引用），闭包可安全捕获最新状态
 */
import { useEffect, useRef } from 'react';

export function usePolling(fn: () => void | Promise<void>, intervalMs: number): void {
  const fnRef = useRef(fn);
  fnRef.current = fn;

  useEffect(() => {
    void fnRef.current();
    const t = window.setInterval(() => void fnRef.current(), intervalMs);
    return () => window.clearInterval(t);
  }, [intervalMs]);
}
