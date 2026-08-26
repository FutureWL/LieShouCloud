package cn.huntercat.lieshoucloudpro.user.web;

/**
 * 租户/用户上下文解析工具（gateway 注入的 X-Tenant-Id / X-User-Id header → Long）.
 *
 * <p>各 Controller 共用：header 缺失/空白/非法 → {@code null}（= 平台上下文，由业务规则决定是否拒绝）。 自 RoleController /
 * TenantController / TenantAccessController / TenantGrantController / UserController 的重复 {@code
 * parseLong} 下沉（同 UserService 下沉先例）。
 */
public final class TenantContext {

  private TenantContext() {}

  /** 解析 Long header（X-Tenant-Id / X-User-Id）；null/空白/非法 → null */
  public static Long parseLong(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
