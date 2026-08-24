/**
 * @lieshoucloud/ui —— 跨 app 共享 React 组件库 + 工具函数.
 *
 * - HealthBadge（Phase 4 · monorepo 升级 demo）
 * - StatusTag / RoleTag / EmptyState（Phase 9 共享包充实）
 * - 跨端格式化工具（truncateText / formatBytes / formatNumber / formatRelativeTime）
 *
 * 依赖：antd + @ant-design/icons + @lieshoucloud/types（peer: react 19）。
 *
 * @see .ai/decisions/0012-monorepo-upgrade.md
 */

export { HealthBadge } from "./components/HealthBadge";
export { StatusTag } from "./components/StatusTag";
export { RoleTag, ROLE_COLORS } from "./components/RoleTag";
export { EmptyState } from "./components/EmptyState";

export { truncateText, formatBytes, formatNumber, formatRelativeTime } from "./utils/format";

export type { HealthStatus, StatusMeta, RoleTagColor } from "@lieshoucloud/types";
