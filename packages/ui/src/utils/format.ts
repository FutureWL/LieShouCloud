/**
 * 跨端格式化工具函数（Phase 9 共享包充实）.
 *
 * 纯函数，无 React / 无 antd 依赖；web / mobile / desktop / mini-program 都可复用。
 */

/** 截断字符串（中文按 1 字符计） */
export function truncateText(text: string | null | undefined, maxLen = 30): string {
  if (text === null || text === undefined) return '';
  const s = String(text);
  if (s.length <= maxLen) return s;
  return `${s.slice(0, maxLen)}…`;
}

/** 字节大小 → 人类可读（如 1.5 MB / 230 KB） */
export function formatBytes(bytes: number, decimals = 1): string {
  if (!Number.isFinite(bytes) || bytes < 0) return '0 B';
  if (bytes === 0) return '0 B';
  const k = 1024;
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), units.length - 1);
  return `${(bytes / Math.pow(k, i)).toFixed(decimals)} ${units[i]}`;
}

/** 数字千分位 */
export function formatNumber(n: number): string {
  if (!Number.isFinite(n)) return '0';
  return n.toLocaleString('en-US');
}

/** 时间间隔 → 人类可读（"3 分钟前" / "2 小时前" / "昨天" / "2026-08-23"） */
export function formatRelativeTime(
  iso: string | Date | null | undefined,
  now: Date = new Date(),
): string {
  if (!iso) return '';
  const d = typeof iso === 'string' ? new Date(iso) : iso;
  if (Number.isNaN(d.getTime())) return '';

  const diffMs = now.getTime() - d.getTime();
  const sec = Math.round(diffMs / 1000);
  if (sec < 60) return '刚刚';
  const min = Math.round(sec / 60);
  if (min < 60) return `${min} 分钟前`;
  const hr = Math.round(min / 60);
  if (hr < 24) return `${hr} 小时前`;

  // 跨天：先看是否是昨天 / 前天，再退化成日期
  const today = new Date(now);
  today.setHours(0, 0, 0, 0);
  const dStart = new Date(d);
  dStart.setHours(0, 0, 0, 0);
  const dayDiff = Math.round((today.getTime() - dStart.getTime()) / 86_400_000);
  if (dayDiff === 1) return '昨天';
  if (dayDiff === 2) return '前天';
  if (dayDiff > 2 && dayDiff < 7) return `${dayDiff} 天前`;
  // 超过一周：返回日期
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}