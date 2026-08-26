/**
 * 元素尺寸监听 + 大屏自适应纯函数（2026-08-24 驾驶舱 · 2026-08-25 下沉 packages/ui）.
 *
 * - useElementSize：ResizeObserver 监听容器宽高（驾驶舱 kiosk/全屏时容器=视口，自动重排）。
 *   jsdom 无 observer（测试环境）→ 静态返回 0，组件走保守布局。
 * - cockpitColumns / cockpitScale：1920×1080 设计稿等比缩放断点。
 */
import { useEffect, useRef, useState } from 'react';

export interface ElementSize {
  width: number;
  height: number;
}

export function useElementSize<T extends HTMLElement>(): readonly [React.RefObject<T | null>, ElementSize] {
  const ref = useRef<T | null>(null);
  const [size, setSize] = useState<ElementSize>({ width: 0, height: 0 });

  useEffect(() => {
    const el = ref.current;
    if (!el || typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver((entries) => {
      const r = entries[0]?.contentRect;
      if (r) setSize({ width: r.width, height: r.height });
    });
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  return [ref, size] as const;
}

/** 驾驶舱布局断点：≥1500px 大屏 4 列主区 / ≥1000px 中屏 2 列 / 窄屏 1 列 */
export function cockpitColumns(width: number): 4 | 2 | 1 {
  if (width >= 1500) return 4;
  if (width >= 1000) return 2;
  return 1;
}

/**
 * 大屏等比缩放因子（1920×1080 设计稿）：任意横屏分辨率完整显示、字号/间距/图表
 * 整体缩放保持一致观感（4K scale=2）。返回 0 表示非横屏/未测量 → 走堆叠模式。
 */
export function cockpitScale(
  cw: number,
  ch: number,
  designW = 1920,
  designH = 1080,
): number {
  if (cw <= 0 || ch <= 0) return 0;
  if (cw / ch < 1.1) return 0; // 竖屏/接近方屏 → 堆叠滚动
  return Math.min(cw / designW, ch / designH);
}
