package cn.huntercat.lieshoucloudpro.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;

/**
 * 租户菜单覆盖（菜单数据驱动 · ADR-0024 Phase 2 阶段 4）.
 *
 * <p>平台默认菜单清单（MenuService.DEFAULT_MENUS）⊕ 租户覆盖（本表：enabled/rename/sort） → 按用户权限过滤 → GET
 * /api/users/me/menus。客户差异"配数据"而非"发代码"。
 */
@Entity
@Table(name = "tenant_menu_configs")
@IdClass(TenantMenuConfig.Key.class)
public class TenantMenuConfig {

  /** 复合主键（tenant_id + menu_key） */
  public static class Key implements Serializable {
    private Long tenantId;
    private String menuKey;

    public Key() {}

    public Key(Long tenantId, String menuKey) {
      this.tenantId = tenantId;
      this.menuKey = menuKey;
    }

    public Long getTenantId() {
      return tenantId;
    }

    public void setTenantId(Long tenantId) {
      this.tenantId = tenantId;
    }

    public String getMenuKey() {
      return menuKey;
    }

    public void setMenuKey(String menuKey) {
      this.menuKey = menuKey;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Key that)) return false;
      return java.util.Objects.equals(tenantId, that.tenantId)
          && java.util.Objects.equals(menuKey, that.menuKey);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(tenantId, menuKey);
    }
  }

  @Id
  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Id
  @Column(name = "menu_key", nullable = false, length = 64)
  private String menuKey;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(nullable = false)
  private int sort;

  @Column(length = 64)
  private String rename;

  @Column(name = "updated_by")
  private Long updatedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public TenantMenuConfig() {}

  public TenantMenuConfig(Long tenantId, String menuKey) {
    this.tenantId = tenantId;
    this.menuKey = menuKey;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    if (createdAt == null) createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getMenuKey() {
    return menuKey;
  }

  public void setMenuKey(String menuKey) {
    this.menuKey = menuKey;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getSort() {
    return sort;
  }

  public void setSort(int sort) {
    this.sort = sort;
  }

  public String getRename() {
    return rename;
  }

  public void setRename(String rename) {
    this.rename = rename;
  }

  public Long getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(Long updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
