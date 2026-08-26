/**
 * 跨端格式化工具单测.
 */
import { describe, expect, it } from 'vitest';

import { formatBytes, formatNumber, formatRelativeTime, truncateText } from './format';

describe('truncateText', () => {
  it('短字符串不变', () => {
    expect(truncateText('hello', 10)).toBe('hello');
  });

  it('超过 maxLen 截断 + …', () => {
    expect(truncateText('hello world', 5)).toBe('hello…');
  });

  it('null / undefined → 空字符串', () => {
    expect(truncateText(null)).toBe('');
    expect(truncateText(undefined)).toBe('');
  });

  it('中文字符按 1 计（slice 走代码点）', () => {
    // 注意：当前实现按 JS string.length（含 UTF-16 code unit），中文每个 = 1
    expect(truncateText('你好世界', 3)).toBe('你好世…');
  });
});

describe('formatBytes', () => {
  it('0 B', () => {
    expect(formatBytes(0)).toBe('0 B');
  });
  it('KB / MB / GB', () => {
    expect(formatBytes(1024)).toBe('1.0 KB');
    expect(formatBytes(1024 * 1024)).toBe('1.0 MB');
    expect(formatBytes(1024 * 1024 * 1024)).toBe('1.0 GB');
  });
  it('非整数', () => {
    expect(formatBytes(1536)).toBe('1.5 KB');
  });
  it('非法输入', () => {
    expect(formatBytes(-1)).toBe('0 B');
    expect(formatBytes(NaN)).toBe('0 B');
  });
});

describe('formatNumber', () => {
  it('千分位', () => {
    expect(formatNumber(1234567)).toMatch(/1[,.]234[,.]567/);
  });
  it('非法输入 → "0"', () => {
    expect(formatNumber(NaN)).toBe('0');
  });
});

describe('formatRelativeTime', () => {
  const NOW = new Date('2026-08-23T12:00:00Z');

  it('null / undefined / 非法 → 空串', () => {
    expect(formatRelativeTime(null, NOW)).toBe('');
    expect(formatRelativeTime(undefined, NOW)).toBe('');
    expect(formatRelativeTime('not-a-date', NOW)).toBe('');
  });

  it('刚刚（< 60s）', () => {
    const d = new Date(NOW.getTime() - 30_000).toISOString();
    expect(formatRelativeTime(d, NOW)).toBe('刚刚');
  });

  it('分钟前', () => {
    const d = new Date(NOW.getTime() - 5 * 60_000).toISOString();
    expect(formatRelativeTime(d, NOW)).toBe('5 分钟前');
  });

  it('小时前', () => {
    const d = new Date(NOW.getTime() - 3 * 60 * 60_000).toISOString();
    expect(formatRelativeTime(d, NOW)).toBe('3 小时前');
  });

  it('昨天', () => {
    const d = new Date(NOW.getTime() - 24 * 60 * 60_000).toISOString();
    expect(formatRelativeTime(d, NOW)).toBe('昨天');
  });

  it('N 天前', () => {
    const d = new Date(NOW.getTime() - 3 * 24 * 60 * 60_000).toISOString();
    expect(formatRelativeTime(d, NOW)).toBe('3 天前');
  });

  it('超过一周 → 日期', () => {
    const d = new Date(NOW.getTime() - 30 * 24 * 60 * 60_000).toISOString();
    expect(formatRelativeTime(d, NOW)).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('接受 Date 对象', () => {
    const d = new Date(NOW.getTime() - 90_000);
    expect(formatRelativeTime(d, NOW)).toBe('2 分钟前');
  });
});