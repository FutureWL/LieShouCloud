/**
 * API Client —— 跨 app 共享 HTTP 调用层.
 *
 * Phase 5+ 由 SpringDoc OpenAPI 自动生成 typed client, 替换本文件占位.
 * @see .ai/decisions/0016-springdoc-openapi.md
 */

export interface ApiRequestOptions {
  method: "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
  path: string;
  body?: unknown;
  query?: Record<string, string | number | boolean>;
  headers?: Record<string, string>;
}

/** Token 供给器：注册一次，request 时自动取 */
let tokenProvider: (() => string | null) | null = null;

/** 注册 token 供给器（应用启动时调一次） */
export function setAccessTokenProvider(fn: (() => string | null) | null): void {
  tokenProvider = fn;
}

/** 401 处理器：返回 true 表示已处理（已 logout + 跳转），调用方应停止后续 */
let unauthorizedHandler: (() => void) | null = null;
export function setUnauthorizedHandler(fn: (() => void) | null): void {
  unauthorizedHandler = fn;
}

/** Base URL：web 默认走 vite proxy（/api → gateway），RN 端需显式设置 gateway 地址 */
let baseUrl = "";
export function setBaseUrl(url: string): void {
  baseUrl = url;
}
export function getBaseUrl(): string {
  return baseUrl;
}

/** 占位 fetch client. Phase 5+ 替换为 openapi-typescript 生成的 typed client. */
export async function request<T>(opts: ApiRequestOptions): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(opts.headers ?? {}),
  };
  const token = tokenProvider?.();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const qs = opts.query
    ? "?" +
      Object.entries(opts.query)
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
        .join("&")
    : "";

  const url = `${baseUrl}/api${opts.path}${qs}`;
  const res = await fetch(url, {
    method: opts.method,
    headers,
    body: opts.body ? JSON.stringify(opts.body) : undefined,
  });

  if (res.status === 401) {
    unauthorizedHandler?.();
    throw new Error("UNAUTHORIZED");
  }
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`);
  return res.json() as Promise<T>;
}

export const __PLACEHOLDER_API_CLIENT__ = true;
