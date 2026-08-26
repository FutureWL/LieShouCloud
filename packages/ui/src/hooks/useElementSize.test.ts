/**
 * useElementSize 断点纯函数单测（大屏自适应）.
 */
import { describe, expect, it } from 'vitest';

import { cockpitColumns, cockpitScale } from './useElementSize';

describe('cockpitColumns（驾驶舱布局断点）', () => {
  it('≥1500 → 4 列（大屏/4K）', () => {
    expect(cockpitColumns(1920)).toBe(4);
    expect(cockpitColumns(1500)).toBe(4);
  });

  it('1000~1499 → 2 列（中屏/笔记本）', () => {
    expect(cockpitColumns(1440)).toBe(2);
    expect(cockpitColumns(1366)).toBe(2);
    expect(cockpitColumns(1000)).toBe(2);
  });

  it('<1000 → 1 列（窄屏/竖屏滚动）', () => {
    expect(cockpitColumns(999)).toBe(1);
    expect(cockpitColumns(800)).toBe(1);
  });

  it('容器未测量（0）→ 1 列保守布局', () => {
    expect(cockpitColumns(0)).toBe(1);
  });
});

describe('cockpitScale（大屏等比缩放）', () => {
  it('16:9 等比：4K(3840×2160) → 2；笔记本内容区(1158×600) → 0.55', () => {
    expect(cockpitScale(3840, 2160)).toBe(2);
    const s = cockpitScale(1158, 600);
    expect(s).toBeCloseTo(0.555, 2);
  });

  it('超宽 21:9：高度受限（完整显示不裁剪）', () => {
    // 3440×1440 → min(3440/1920, 1440/1080) = min(1.79, 1.333) = 1.333
    expect(cockpitScale(3440, 1440)).toBeCloseTo(1.333, 2);
  });

  it('竖屏/未测量 → 0（堆叠模式）', () => {
    expect(cockpitScale(1080, 1920)).toBe(0);
    expect(cockpitScale(0, 1080)).toBe(0);
    expect(cockpitScale(0, 0)).toBe(0);
  });
});
