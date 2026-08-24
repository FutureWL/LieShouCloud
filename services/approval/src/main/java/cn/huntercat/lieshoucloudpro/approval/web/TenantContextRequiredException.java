package cn.huntercat.lieshoucloudpro.approval.web;

/** 业务端点强制要求租户上下文（X-Tenant-Id 缺失/非法 → 401 · ADR-0025 模式）. */
public class TenantContextRequiredException extends RuntimeException {
  public TenantContextRequiredException() {
    super("X-Tenant-Id header is required for tenant-scoped business endpoints");
  }
}
