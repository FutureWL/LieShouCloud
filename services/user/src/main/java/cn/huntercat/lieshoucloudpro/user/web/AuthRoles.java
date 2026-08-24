package cn.huntercat.lieshoucloudpro.user.web;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色鉴权工具（RBAC · ADR-0024）.
 *
 * <p>从 gateway 透传的 {@code X-User-Roles} header（逗号分隔，来自 JWT roles claim）解析当前用户角色。
 */
public final class AuthRoles {

  private AuthRoles() {}

  /** 角色编码常量 */
  public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

  public static final String TENANT_ADMIN = "TENANT_ADMIN";
  public static final String USER = "USER";

  /** 当前用户是否包含任一要求的角色（header 缺失/空 → false） */
  public static boolean hasAny(String rolesHeader, String... required) {
    if (rolesHeader == null || rolesHeader.isBlank()) return false;
    Set<String> roles =
        Arrays.stream(rolesHeader.split(",")).map(String::trim).collect(Collectors.toSet());
    return Arrays.stream(required).anyMatch(roles::contains);
  }
}
