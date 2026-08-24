/**
 * 共享业务类型 — monorepo 跨 app 共用。
 * Phase 5+ 由 SpringDoc OpenAPI 自动生成 TS 类型，替换本文件。
 */

// -------- 通用 API 响应包装 --------
export type ApiResponse<T> = {
  data: T;
  success: boolean;
  message?: string;
};

// -------- 健康状态 / 跨端共享 --------
export type HealthStatus = 'up' | 'down' | 'degraded';

// -------- 状态元数据（Phase 9 共享给 ui 包的 StatusTag） --------
/** 状态 → 中文文本 + antd Tag 颜色 */
export interface StatusMeta {
  text: string;
  color: string;
}

/** 安全查找：避免任何 key 不存在的运行时崩 */
export function getStatusMeta<T extends string>(
  meta: Record<T, StatusMeta>,
  key: T,
): StatusMeta {
  return meta[key];
}

/** antd Tag 可用颜色 */
export type RoleTagColor = string;

// -------- 用户视图（与 user-service 字段对齐） --------
// Phase 5+ 由 OpenAPI 自动派生。passwordHash 后端 WRITE_ONLY，绝不下发。

export type UserStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED';

export interface UserDTO {
  id: number;
  /** 归属租户（多租户 · ADR-0022） */
  tenantId: number;
  username: string;
  displayName: string;
  email?: string | null;
  phone?: string | null;
  status: UserStatus;
  roles: string[];
  createdAt: string;
  updatedAt?: string;
  lastLoginAt?: string | null;
}

// -------- 认证（多端共享 · Phase 9 desktop/移动端复用） --------

export interface LoginRequest {
  username: string;
  password: string;
  /** 可选租户编码（多租户 · ADR-0022） */
  tenantCode?: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number; // seconds
  tokenType: 'Bearer';
  userId: number;
  username: string;
}

export interface CurrentUser {
  userId: number;
  username: string;
  roles: string[];
  tenantId?: number;
  tenantCode?: string;
}

// 占位 token，后续接 Spring 后端实际字段
export const __PLACEHOLDER_TYPES__ = true;
