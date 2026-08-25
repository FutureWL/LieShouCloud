package cn.huntercat.lieshoucloudpro.file.web;

/**
 * 租户上下文缺失/非法异常（ADR-0025 强制租户模式）→ 401 TENANT_CONTEXT_REQUIRED。
 */
public class TenantContextRequiredException extends RuntimeException {

  public TenantContextRequiredException(String message) {
    super(message);
  }
}
