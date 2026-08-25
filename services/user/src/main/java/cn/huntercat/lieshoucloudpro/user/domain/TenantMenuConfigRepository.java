package cn.huntercat.lieshoucloudpro.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 租户菜单覆盖 Repository（菜单数据驱动 · ADR-0024 Phase 2 阶段 4）. */
public interface TenantMenuConfigRepository extends JpaRepository<TenantMenuConfig, TenantMenuConfig.Key> {

  /** 租户全部菜单覆盖配置（含 disabled，用于合并时排除）。 */
  List<TenantMenuConfig> findByTenantId(Long tenantId);
}
