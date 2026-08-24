package cn.huntercat.lieshoucloudpro.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 租户实体（多租户 · ADR-0022）.
 *
 * <p>共享表 + tenant_id 行级隔离：所有业务实体归属某个租户，查询强制带 tenant 维度。 登录用 {@code code} 标识租户（如 huntercat / zhiye）。
 */
@Entity
@Table(name = "tenants")
@Schema(description = "Tenant (enterprise/organization) owned by user-service")
public class Tenant {

  /** 租户状态（与 tenants.status CHECK 对齐） */
  public enum Status {
    ACTIVE,
    DISABLED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(
      description = "Auto-assigned primary key",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  private Long id;

  @Column(nullable = false, length = 128)
  @Schema(
      description = "Enterprise display name",
      example = "南昌猎手猫数字科技有限公司",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Column(nullable = false, unique = true, length = 64)
  @Schema(
      description = "Tenant code used for login (unique)",
      example = "huntercat",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  @Schema(
      description = "Tenant status",
      example = "ACTIVE",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Status status = Status.ACTIVE;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Schema(description = "Create timestamp", accessMode = Schema.AccessMode.READ_ONLY)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  @Schema(description = "Last update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
  private Instant updatedAt;

  public Tenant() {}

  public Tenant(String name, String code) {
    this.name = name;
    this.code = code;
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

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
