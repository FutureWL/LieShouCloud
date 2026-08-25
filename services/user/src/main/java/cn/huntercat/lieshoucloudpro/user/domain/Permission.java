package cn.huntercat.lieshoucloudpro.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限点实体（RBAC · ADR-0024 Phase 2 · 平台基础层）.
 *
 * <p>菜单可见性与接口鉴权共用同一权限码数据源：{@code code} 形如 {@code <域>:<资源>:<动作>}（如
 * {@code legal:use} / {@code tenant:manage}）。角色-权限经 {@code role_permissions} 多对多关联；
 * 用户权限 = 用户角色并集。
 */
@Entity
@Table(
    name = "permissions",
    indexes = {@Index(name = "idx_permissions_scope", columnList = "scope")})
public class Permission {

  public enum Scope {
    PLATFORM,
    TENANT
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64, unique = true)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Scope scope;

  @Column(length = 500)
  private String description;

  @Column(name = "is_system", nullable = false)
  private boolean system = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @ManyToMany
  @JoinTable(
      name = "role_permissions",
      joinColumns = @JoinColumn(name = "permission_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private List<Role> roles = new ArrayList<>();

  public Permission() {}

  public Permission(String code, String name, Scope scope) {
    this.code = code;
    this.name = name;
    this.scope = scope;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Scope getScope() {
    return scope;
  }

  public void setScope(Scope scope) {
    this.scope = scope;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isSystem() {
    return system;
  }

  public void setSystem(boolean system) {
    this.system = system;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public List<Role> getRoles() {
    return roles;
  }

  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }
}
