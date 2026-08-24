import type { ReactNode } from "react";
import type { HealthStatus } from "@lieshoucloud/types";

const COLORS: Record<HealthStatus, string> = {
  up: "#52c41a",
  down: "#f5222d",
  degraded: "#faad14",
};

const LABELS: Record<HealthStatus, string> = {
  up: "UP",
  down: "DOWN",
  degraded: "DEGRADED",
};

interface HealthBadgeProps {
  status: HealthStatus;
  serviceName?: string;
}

/**
 * 跨 app 共享：极简健康状态徽章。
 * Phase 4 monorepo 升级：从 apps/admin/components/HealthBadge.tsx 抽出来。
 * @see .ai/decisions/0012-monorepo-upgrade.md
 */
export function HealthBadge({ status, serviceName }: HealthBadgeProps): ReactNode {
  const color = COLORS[status];
  const label = LABELS[status];

  return (
    <span
      data-testid="health-badge"
      style={{
        display: "inline-block",
        padding: "2px 8px",
        borderRadius: 4,
        background: color,
        color: "#fff",
        fontSize: 12,
        fontWeight: 600,
      }}
    >
      {serviceName ? `${serviceName}: ` : ""}
      {label}
    </span>
  );
}
