/**
 * 大屏深色主题令牌（DataV 大屏配色 · 2026-08-25 全面令牌化）.
 *
 * 从驾驶舱（apps/admin/src/pages/IoT/cockpit/theme.ts）下沉到共享包：
 * 所有面板/图表统一从这里取色，禁止硬编码 hex —— 改主题只改本文件。
 */

// ── 边框 / 背景 / 光效 ──
export const BORDER = '1px solid #1e5b8a';
export const PANEL_BG = 'rgba(9, 30, 60, 0.72)';
export const PANEL_BG_LIGHT = 'rgba(9, 30, 60, 0.55)';
export const GLOW = '0 0 12px rgba(0, 188, 235, 0.35)';
export const GLOW_STRONG = '0 0 14px rgba(0, 188, 235, 0.5)';
export const GRID_LINE = 'rgba(0,188,235,0.05)';

// ── 文字色阶 ──
export const TXT = {
  bright: '#e6f4ff', // 主文字
  primary: '#00e5ff', // 高亮/标题
  secondary: '#8fc1e3', // 次级文字
  muted: '#5a7f9f', // 弱化
};

// ── 语义色（只表达状态，禁止装饰滥用） ──
export const STATUS = {
  online: '#52c41a', // 正常/在线
  warn: '#fa8c16', // 预警
  alert: '#ff4d4f', // 告警
  offline: '#5a7f9f', // 离线
  info: '#00e5ff', // 中性信息
  data: '#1677ff', // 普通数据
};

// ── 字号令牌（1080 设计稿基准 · 全驾驶舱统一） ──
// 大屏远距离观看，宁大勿小；新组件一律从这里取字号，禁止散落硬编码
export const FONT = {
  hero: 56, // 最大数字：KPI / 最高温 / 风险百分比
  big: 40, // 大数字：局放峰值 / 环心总数
  title: 20, // 标题：面板标题栏
  strong: 17, // 强调：KPI 卡标题 / 等级文字 / 重要行
  body: 15, // 正文：健康度正文 / 列表正文 / 图例
  label: 14, // 普通：表格 / 标签 / 分布图例 / 设备名
  muted: 12, // 减弱：辅助说明 / 次级信息
};
