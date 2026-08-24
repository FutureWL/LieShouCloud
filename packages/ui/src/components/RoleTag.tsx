/**
 * RoleTag — 角色 Tag（Phase 9 共享包充实）.
 *
 * 平台/租户管理员/普通用户三档颜色：gold / orange / blue.
 */
import { Tag } from 'antd';
import type { ReactNode } from 'react';

import type { RoleTagColor } from '@lieshoucloud/types';

export const ROLE_COLORS: Record<string, RoleTagColor> = {
  PLATFORM_ADMIN: 'gold',
  TENANT_ADMIN: 'orange',
  ADMIN: 'orange', // 别名（V5 迁移期）
  USER: 'blue',
};

const DEFAULT_COLOR: RoleTagColor = 'blue';

interface RoleTagProps {
  role: string;
  /** 自定义角色颜色映射，覆盖默认 */
  colorMap?: Record<string, RoleTagColor>;
}

export function RoleTag({ role, colorMap }: RoleTagProps): ReactNode {
  const map = colorMap ?? ROLE_COLORS;
  const color = map[role] ?? DEFAULT_COLOR;
  return <Tag color={color}>{role}</Tag>;
}