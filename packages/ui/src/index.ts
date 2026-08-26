/**
 * @lieshoucloud/ui —— 跨 app 共享 React 组件库 + 工具函数.
 *
 * - HealthBadge（Phase 4 · monorepo 升级 demo）
 * - StatusTag / RoleTag / EmptyState（Phase 9 共享包充实）
 * - datav/*（2026-08-25 从驾驶舱下沉）：大屏组件（Panel/FlopStat/RollTable/Ticker/…）
 *   + 主题令牌（datavTheme）+ ECharts 深色主题（datavChartTheme）+ 纯函数（datavRisk）
 * - EChart（ECharts React 封装）、useElementSize / usePolling / useClock（大屏 hooks）
 * - 跨端格式化工具（truncateText / formatBytes / formatNumber / formatRelativeTime）
 *
 * 依赖：antd + @ant-design/icons + echarts + framer-motion + @lieshoucloud/types（peer: react 19）。
 *
 * @see .ai/decisions/0012-monorepo-upgrade.md
 */

export { HealthBadge } from './components/HealthBadge';
export { StatusTag } from './components/StatusTag';
export { RoleTag, ROLE_COLORS } from './components/RoleTag';
export { EmptyState } from './components/EmptyState';

export { truncateText, formatBytes, formatNumber, formatRelativeTime } from './utils/format';

// ── 大屏（datav）组件与主题 —— 2026-08-25 从驾驶舱下沉 packages/ui ──
export { default as DatavPanel, type ZoomInfo } from './datav/Panel';
export { default as DatavPanelBadge, type PanelBadgeTone } from './datav/PanelBadge';
export { default as DatavFlopStat } from './datav/FlopStat';
export { default as DatavRollTable, type RollTableTheme } from './datav/RollTable';
export { default as DatavTicker } from './datav/Ticker';
export { default as DatavZoomOverlay } from './datav/ZoomOverlay';
export { default as DatavDvRing, type DvRingDatum, type DvRingProps } from './datav/DvRing';
export { default as DatavStatRow, type StatItem } from './datav/StatRow';
export { default as DatavHeader, type HeaderProps } from './datav/Header';
export { default as DatavStatusBar, type StatusBarProps } from './datav/StatusBar';

// 主题令牌 / ECharts 深色主题 / 大屏纯函数（命名空间出口，避免占用包级通用名）
export * as datavTheme from './datav/theme';
export * as datavChartTheme from './datav/chartTheme';
export * as datavRisk from './datav/risk';

// ── ECharts 封装 + 大屏 hooks ──
export { default as EChart, type EChartProps } from './components/EChart';
export { useElementSize, cockpitColumns, cockpitScale, type ElementSize } from './hooks/useElementSize';
export { usePolling } from './hooks/usePolling';
export { useClock } from './hooks/useClock';

export type { HealthStatus, StatusMeta, RoleTagColor } from '@lieshoucloud/types';
