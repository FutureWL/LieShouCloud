package cn.huntercat.lieshoucloudpro.user.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 操作审计日志 DTO（append-only · DATA_SECURITY.md §7）.
 *
 * <p>ADR-0030 Stage 2：写路径已上提 core.audit（audit_events 表），本类保留 Action/Outcome 枚举（控制器沿用）
 * 与响应形状，不再映射本地表（V6 audit_logs 为历史归档）。六要素：who / when / what / from / outcome / requestId。
 */
@Schema(description = "Operation audit log DTO (append-only)")
public class AuditLog {

  public enum Action {
    CREATE,
    UPDATE,
    DELETE,
    DENIED,
    LOGIN,
    READ
  }

  public enum Outcome {
    SUCCESS,
    DENIED,
    ERROR
  }

  @Schema(description = "Auto-assigned primary key", accessMode = Schema.AccessMode.READ_ONLY)
  private Long id;

  private Long tenantId;

  private Long userId;

  private Action action;

  private String resourceType;

  private Long resourceId;

  private String detail;

  private String sourceIp;

  private String userAgent;

  private Outcome outcome = Outcome.SUCCESS;

  private String requestId;

  private Instant createdAt = Instant.now();

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

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public Long getResourceId() {
    return resourceId;
  }

  public void setResourceId(Long resourceId) {
    this.resourceId = resourceId;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public String getSourceIp() {
    return sourceIp;
  }

  public void setSourceIp(String sourceIp) {
    this.sourceIp = sourceIp;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public Outcome getOutcome() {
    return outcome;
  }

  public void setOutcome(Outcome outcome) {
    this.outcome = outcome;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
