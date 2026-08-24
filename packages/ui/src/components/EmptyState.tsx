/**
 * EmptyState — 标准化空态占位（Phase 9 共享包充实）.
 *
 * antd Empty 的轻包装 + 默认描述 / 行动槽位，方便各列表/详情页统一空态 UX。
 */
import { Empty } from "antd";
import type { ReactNode } from "react";

interface EmptyStateProps {
  description?: ReactNode;
  /** 主行动按钮 */
  action?: ReactNode;
  /** 'table' / 'card' 等上下文不同，图标略变（默认通用） */
  size?: "small" | "default";
}

export function EmptyState({ description = "暂无数据", action, size = "default" }: EmptyStateProps): ReactNode {
  return (
    <Empty
      image={Empty.PRESENTED_IMAGE_SIMPLE}
      description={description}
      style={{
        padding: size === "small" ? 16 : 32,
      }}
    >
      {action}
    </Empty>
  );
}
