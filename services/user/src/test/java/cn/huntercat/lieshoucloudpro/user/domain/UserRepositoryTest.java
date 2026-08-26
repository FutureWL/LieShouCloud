package cn.huntercat.lieshoucloudpro.user.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import cn.huntercat.lieshoucloudpro.user.PostgresTestSupport;

/**
 * UserRepository 切片测试（{@code @DataJpaTest} + Testcontainers PostgreSQL）.
 *
 * <p>Flyway 跑 V1 + V2（多租户 tenants 表）→ Hibernate validate → Repository 断言。 Phase 8（ADR-0022）: 租户维度查询
 * + 租户内唯一约束。
 *
 * @see .ai/TESTING.md §3 / §4 / §9
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserRepository（JPA 切片 + 真 PG + 多租户）")
class UserRepositoryTest extends PostgresTestSupport {

  /** Flyway V2 seed 的猎手猫租户 */
  private static final long TENANT_HUNTERCAT = 1L;

  @Autowired private UserRepository repo;
  @Autowired private TenantRepository tenantRepo;
  @Autowired private RoleRepository roleRepo;

  /** 给用户分配角色（RBAC · ADR-0024） */
  private User withRole(User u, String code) {
    u.setRoles(java.util.List.of(roleRepo.findByCode(code).orElseThrow()));
    return u;
  }

  @Test
  @DisplayName("Flyway V2 seed：存在默认租户 huntercat")
  void tenantSeed_exists() {
    var tenant = tenantRepo.findByCode("huntercat");
    assertThat(tenant).isPresent();
    assertThat(tenant.get().getName()).contains("猎手猫");
  }

  @Test
  @DisplayName("保存用户后能由 id 找回（含租户维度 + 角色）")
  void save_andFindById_returnsPersistedUser() {
    User saved =
        repo.save(withRole(new User(TENANT_HUNTERCAT, "alice", "Alice", "hash-alice"), "USER"));
    assertThat(saved.getId()).isNotNull();

    User found = repo.findById(saved.getId()).orElseThrow();
    assertThat(found.getTenantId()).isEqualTo(TENANT_HUNTERCAT);
    assertThat(found.getUsername()).isEqualTo("alice");
    assertThat(found.getDisplayName()).isEqualTo("Alice");
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getStatus()).isEqualTo(User.Status.ACTIVE);
    // RBAC（ADR-0024）：角色走 user_roles 关联
    assertThat(found.getRoles()).extracting(Role::getCode).containsExactly("USER");
  }

  @Test
  @DisplayName("按租户 + username 查找存在的用户应返回 Optional<User>")
  void findByTenantIdAndUsername_existing_returnsPresent() {
    repo.save(new User(TENANT_HUNTERCAT, "bob", "Bob", "hash-bob"));

    var found = repo.findByTenantIdAndUsername(TENANT_HUNTERCAT, "bob");

    assertThat(found).isPresent();
    assertThat(found.get().getDisplayName()).isEqualTo("Bob");
  }

  @Test
  @DisplayName("按租户 + username 查找其他租户的用户应返回 empty（租户隔离）")
  void findByTenantIdAndUsername_otherTenant_returnsEmpty() {
    repo.save(new User(TENANT_HUNTERCAT, "carol", "Carol", "hash-carol"));

    var found = repo.findByTenantIdAndUsername(999L, "carol");

    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("username 唯一约束为「租户内」：同租户重复抛异常")
  void save_duplicateUsernameInSameTenant_violatesUniqueConstraint() {
    repo.saveAndFlush(new User(TENANT_HUNTERCAT, "dave", "Dave 1", "hash-dave-1"));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> repo.saveAndFlush(new User(TENANT_HUNTERCAT, "dave", "Dave 2", "hash-dave-2")))
        .isInstanceOfAny(
            org.springframework.dao.DataIntegrityViolationException.class,
            org.hibernate.exception.ConstraintViolationException.class);
  }

  @Test
  @DisplayName("findByTenantId 只返回该租户用户（租户内列表过滤）")
  void findByTenantId_onlyReturnsThatTenant() {
    Tenant zhiye = tenantRepo.save(new Tenant("深圳市智野教育科技有限公司", "zhiye"));
    repo.save(new User(TENANT_HUNTERCAT, "alice", "Alice", "h1"));
    repo.save(new User(TENANT_HUNTERCAT, "bob", "Bob", "h2"));
    repo.save(new User(zhiye.getId(), "carol", "Carol", "h3"));

    // 按用户名断言（不断言精确条数）：huntercat 有 dev seed（R__seed_admin）时也多一条 admin，
    // 本测试关注隔离语义——本租户用户都在、其他租户用户不在。
    var huntercatUsers = repo.findByTenantId(TENANT_HUNTERCAT);
    assertThat(huntercatUsers).allMatch(u -> u.getTenantId() == TENANT_HUNTERCAT);
    assertThat(huntercatUsers).extracting(User::getUsername).contains("alice", "bob");
    assertThat(huntercatUsers).extracting(User::getUsername).doesNotContain("carol");

    var zhiyeUsers = repo.findByTenantId(zhiye.getId());
    assertThat(zhiyeUsers).hasSize(1);
    assertThat(zhiyeUsers.get(0).getUsername()).isEqualTo("carol");
  }

  @Test
  @DisplayName("不同租户可用相同 username（联合唯一 (tenant_id, username)）")
  void save_sameUsernameDifferentTenant_ok() {
    Tenant zhiye = tenantRepo.save(new Tenant("深圳市智野教育科技有限公司", "zhiye"));
    repo.saveAndFlush(new User(TENANT_HUNTERCAT, "erin", "Erin A", "hash-erin-a"));
    repo.saveAndFlush(new User(zhiye.getId(), "erin", "Erin B", "hash-erin-b"));

    assertThat(repo.findByTenantIdAndUsername(zhiye.getId(), "erin")).isPresent();
    assertThat(repo.findByTenantIdAndUsername(TENANT_HUNTERCAT, "erin")).isPresent();
  }
}
