package cn.huntercat.lieshoucloudpro.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 统一账号跨子公司授权仓储（集团版 · Phase 1 §3.2）. */
public interface UserTenantGrantRepository extends JpaRepository<UserTenantGrant, Long> {

  List<UserTenantGrant> findByUserId(Long userId);

  List<UserTenantGrant> findByTenantId(Long tenantId);

  /** 用户在某子公司是否已有某角色授权（唯一约束幂等预检） */
  boolean existsByUserIdAndTenantIdAndRoleId(Long userId, Long tenantId, Long roleId);
}
