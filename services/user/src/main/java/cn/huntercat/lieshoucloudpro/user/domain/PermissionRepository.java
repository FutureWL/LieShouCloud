package cn.huntercat.lieshoucloudpro.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 权限点 Repository（RBAC · ADR-0024 Phase 2 · 平台基础层）.
 *
 * <p>提供按用户查权限码（user → roles → permissions 并集）与按角色查权限码，供鉴权视图与
 * 菜单裁决复用。
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

  /** 用户全部权限码（角色并集，去重排序；未分配角色用户 → 空集合，登录侧回退 USER）。 */
  @Query(
      """
      select distinct p.code
      from Permission p
        join p.roles r
        join r.users u
      where u.id = :userId
      order by p.code
      """)
  List<String> findCodesByUserId(@Param("userId") Long userId);

  /** 角色全部权限码（按角色 code，去重排序）。 */
  @Query(
      """
      select distinct p.code
      from Permission p
        join p.roles r
      where r.code in :roleCodes
      order by p.code
      """)
  List<String> findCodesByRoleCodes(@Param("roleCodes") List<String> roleCodes);
}
