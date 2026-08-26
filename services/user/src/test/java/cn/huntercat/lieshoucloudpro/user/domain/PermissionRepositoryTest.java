package cn.huntercat.lieshoucloudpro.user.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import cn.huntercat.lieshoucloudpro.user.PostgresTestSupport;
import java.util.List;

/**
 * PermissionRepository 切片测试（JPA 切片 + 真 PG · ADR-0024 Phase 2 权限点）.
 *
 * <p>验证：权限码 seed 存在、按用户查权限码（角色并集）、按角色查权限码、无角色用户 → 空集合。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PermissionRepository（JPA 切片 + 真 PG · 权限点）")
class PermissionRepositoryTest extends PostgresTestSupport {

  @Autowired private UserRepository users;
  @Autowired private RoleRepository roles;
  @Autowired private PermissionRepository permissions;

  /** 给用户分配角色（RBAC · ADR-0024） */
  private User withRole(User u, String code) {
    u.setRoles(java.util.List.of(roles.findByCode(code).orElseThrow()));
    return u;
  }

  @Test
  @DisplayName("V10 seed：平台内置权限点齐全（tenant:manage / legal:use / iot:monitor 等）")
  void seed_exists() {
    assertThat(permissions.findCodesByRoleCodes(List.of("PLATFORM_ADMIN")))
        .contains("tenant:manage", "user:manage", "legal:use", "iot:monitor", "iot:config");
    assertThat(permissions.findCodesByRoleCodes(List.of("USER")))
        .contains("user:list", "crm:use", "legal:use")
        .doesNotContain("tenant:manage", "iot:config");
  }

  @Test
  @DisplayName("按用户查权限码：角色并集（USER + TENANT_ADMIN）去重排序")
  void findCodesByUserId_union() {
    User u = users.save(withRole(new User(1L, "perm-alice", "Alice", "hash"), "USER"));
    u.setRoles(
        new java.util.ArrayList<>(
            List.of(
                roles.findByCode("USER").orElseThrow(),
                roles.findByCode("TENANT_ADMIN").orElseThrow())));
    users.save(u);

    List<String> codes = permissions.findCodesByUserId(u.getId());
    // USER（user:list/crm:use/legal:use...）+ TENANT_ADMIN（含 user:manage）并集
    assertThat(codes)
        .contains("user:list", "user:manage", "crm:use", "legal:use", "iot:monitor")
        .doesNotContain("tenant:manage"); // 仅 PLATFORM_ADMIN
    // 去重 + 排序
    assertThat(codes).isSorted();
  }

  @Test
  @DisplayName("无角色用户 → 空权限集合（登录侧回退 USER 角色）")
  void findCodesByUserId_noRoles_empty() {
    User u = users.save(new User(1L, "perm-bob", "Bob", "hash"));
    assertThat(permissions.findCodesByUserId(u.getId())).isEmpty();
  }
}
