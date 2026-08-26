package cn.huntercat.lieshoucloudpro.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 统一账号跨子公司授权（集团管理行业版 · Phase 1 §3.2）.
 *
 * <p>总部用户主属集团租户（{@code users.tenant_id}），通过本表被授权访问子公司租户。 一条记录 = 用户 × 子公司租户 × 角色（子公司维度授权，ADR-0024
 * RBAC 扩展）。
 *
 * <p>主属租户无需授权（天然可访问）；仅子公司访问需要本表记录。
 */
@Entity
@Table(name = "user_tenant_grants")
@Schema(description = "统一账号跨子公司授权（集团版）")
public class UserTenantGrant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @Schema(description = "被授权用户")
  private User user;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  @Schema(description = "子公司租户")
  private Tenant tenant;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  @Schema(description = "在该子公司内的角色（TENANT scope）")
  private Role role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "granted_by")
  @Schema(description = "授权人（总部管理员）")
  private User grantedBy;

  @Column(name = "granted_at", nullable = false, updatable = false)
  @Schema(description = "授权时间")
  private Instant grantedAt;

  public UserTenantGrant() {}

  public UserTenantGrant(User user, Tenant tenant, Role role, User grantedBy) {
    this.user = user;
    this.tenant = tenant;
    this.role = role;
    this.grantedBy = grantedBy;
  }

  @PrePersist
  void onCreate() {
    if (grantedAt == null) grantedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public Tenant getTenant() {
    return tenant;
  }

  public Role getRole() {
    return role;
  }

  public User getGrantedBy() {
    return grantedBy;
  }

  public Instant getGrantedAt() {
    return grantedAt;
  }
}
