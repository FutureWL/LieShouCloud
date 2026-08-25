package cn.huntercat.lieshoucloudpro.file.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 文件元数据实体（file-service 独占 · 磁盘存储，DB 仅元数据）.
 *
 * <p>多租户：{@code tenant_id} NOT NULL + 应用层强制过滤；{@code is_deleted} 软删（回收站语义：软删后不可下载）。
 * 磁盘文件名（{@code stored_name}）为 UUID，避免用户文件名路径注入。
 */
@Entity
@Table(
    name = "files",
    indexes = {@Index(name = "idx_files_tenant_id", columnList = "tenant_id")})
@Schema(description = "文件元数据（file-service 独占）")
public class FileEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(
      description = "Auto-assigned primary key",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  @Schema(
      description = "Owning tenant (logical ref -> user-service tenants.id)",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long tenantId;

  @Column(name = "original_name", nullable = false, length = 255)
  @Schema(description = "原始文件名（用户上传）", example = "委托合同.pdf")
  private String originalName;

  @Column(name = "stored_name", nullable = false, length = 64)
  @Schema(description = "磁盘文件名（UUID，内部字段）", accessMode = Schema.AccessMode.READ_ONLY)
  private String storedName;

  @Column(name = "content_type", length = 128)
  @Schema(description = "MIME 类型", example = "application/pdf")
  private String contentType;

  @Column(nullable = false)
  @Schema(description = "文件大小（字节）", example = "204800")
  private long size;

  @Column(name = "is_deleted", nullable = false)
  private boolean deleted;

  @Column(name = "created_by")
  @Schema(
      description = "创建人（逻辑 ref -> users.id，gateway X-User-Id 注入）",
      accessMode = Schema.AccessMode.READ_ONLY)
  private Long createdBy;

  @Column(name = "updated_by")
  @Schema(description = "最后更新人（逻辑 ref -> users.id）", accessMode = Schema.AccessMode.READ_ONLY)
  private Long updatedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Schema(
      description = "Create timestamp (server-assigned)",
      accessMode = Schema.AccessMode.READ_ONLY)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  @Schema(
      description = "Last update timestamp (server-assigned)",
      accessMode = Schema.AccessMode.READ_ONLY)
  private Instant updatedAt;

  public FileEntity() {}

  public FileEntity(Long tenantId, String originalName, String storedName, String contentType, long size) {
    this.tenantId = tenantId;
    this.originalName = originalName;
    this.storedName = storedName;
    this.contentType = contentType;
    this.size = size;
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

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getOriginalName() {
    return originalName;
  }

  public void setOriginalName(String originalName) {
    this.originalName = originalName;
  }

  public String getStoredName() {
    return storedName;
  }

  public void setStoredName(String storedName) {
    this.storedName = storedName;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  /** 软删标记（内部字段，不对外序列化） */
  @JsonIgnore
  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
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

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
