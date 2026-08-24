package cn.huntercat.lieshoucloudpro.approval.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import cn.huntercat.lieshoucloudpro.approval.domain.ApprovalAuditLog;
import cn.huntercat.lieshoucloudpro.approval.domain.ApprovalAuditLogRepository;
import cn.huntercat.lieshoucloudpro.approval.domain.ApprovalRequest;
import cn.huntercat.lieshoucloudpro.approval.domain.ApprovalRequestRepository;
import cn.huntercat.lieshoucloudpro.approval.feign.UserQueryClient;
import cn.huntercat.lieshoucloudpro.approval.feign.UserView;
import cn.huntercat.lieshoucloudpro.approval.service.ApprovalAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 通用审批请求 REST 端点（审批流 · 租户内强制过滤 · ADR-0025 模式 · ADR-0032）. */
@RestController
@RequestMapping("/api/approvals")
@Tag(name = "Approval · Requests", description = "通用审批请求：发起 / 审批 / 撤销（租户内）")
public class ApprovalController {

  private static final String HDR_TENANT_ID = "X-Tenant-Id";
  private static final String HDR_USER_ID = "X-User-Id";

  private final ApprovalRequestRepository repo;
  private final ApprovalAuditLogRepository auditRepo;
  private final ApprovalAuditService auditService;
  private final UserQueryClient userClient;

  public ApprovalController(
      ApprovalRequestRepository repo,
      ApprovalAuditLogRepository auditRepo,
      ApprovalAuditService auditService,
      UserQueryClient userClient) {
    this.repo = repo;
    this.auditRepo = auditRepo;
    this.auditService = auditService;
    this.userClient = userClient;
  }

  @Operation(summary = "List approval requests (tenant-scoped)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "List (may be empty)"),
    @ApiResponse(responseCode = "401", description = "TENANT_CONTEXT_REQUIRED")
  })
  @GetMapping
  public ResponseEntity<List<ApprovalRequest>> list(
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userHeader,
      @Parameter(description = "mine=我发起的 / inbox=待我审批 / all=全部（默认）")
          @RequestParam(required = false)
          String role,
      @Parameter(description = "EXPENSE / PURCHASE / SALE / OTHER 过滤")
          @RequestParam(required = false)
          String type,
      @Parameter(description = "PENDING / APPROVED / REJECTED / CANCELLED 过滤")
          @RequestParam(required = false)
          String status) {
    Long tid = requireTenant(tenantHeader);
    ApprovalRequest.Type typeFilter = parseType(type);
    ApprovalRequest.Status statusFilter = parseStatus(status);
    Long userId = parseUserId(userHeader);

    List<ApprovalRequest> data;
    if ("mine".equals(role)) {
      data = userId == null ? List.of() : repo.findByRequester(tid, userId, statusFilter);
    } else if ("inbox".equals(role)) {
      data = userId == null ? List.of() : repo.findInbox(tid, userId);
    } else {
      data = repo.findTenantRequests(tid, typeFilter, statusFilter);
    }
    return ResponseEntity.ok(data);
  }

  @Operation(summary = "待办计数（工作台角标）")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Counts"),
    @ApiResponse(responseCode = "401", description = "TENANT_CONTEXT_REQUIRED")
  })
  @GetMapping("/counts")
  public ResponseEntity<Map<String, Long>> counts(
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userHeader) {
    Long tid = requireTenant(tenantHeader);
    Long userId = parseUserId(userHeader);
    long inbox = 0L;
    long mine = 0L;
    if (userId != null) {
      inbox =
          repo.countByTenantIdAndApproverIdAndStatus(tid, userId, ApprovalRequest.Status.PENDING);
      mine =
          repo.countByTenantIdAndRequesterIdAndStatus(tid, userId, ApprovalRequest.Status.PENDING);
    }
    return ResponseEntity.ok(Map.of("inbox", inbox, "mine", mine));
  }

  @Operation(summary = "Get approval request by id")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Found"),
    @ApiResponse(responseCode = "404", description = "Not found (or cross-tenant)"),
    @ApiResponse(responseCode = "401", description = "TENANT_CONTEXT_REQUIRED")
  })
  @GetMapping("/{id}")
  public ResponseEntity<ApprovalRequest> get(
      @Parameter(description = "Approval id", example = "1") @PathVariable Long id,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader) {
    Long tid = requireTenant(tenantHeader);
    return repo.findById(id)
        .filter(a -> tenantMatches(a, tid))
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(summary = "发起审批（PENDING）")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Created"),
    @ApiResponse(responseCode = "400", description = "Validation error"),
    @ApiResponse(responseCode = "401", description = "TENANT_CONTEXT_REQUIRED")
  })
  @PostMapping
  public ResponseEntity<ApprovalRequest> create(
      @Valid @RequestBody CreateApprovalRequest body,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userHeader,
      HttpServletRequest req) {
    Long tid = requireTenant(tenantHeader);
    Long requesterId = parseUserId(userHeader);
    if (requesterId == null) {
      throw new ApprovalForbiddenException("X-User-Id 缺失，无法识别发起人");
    }
    // 阶段 2：approverId 可空 —— 业务挂接自动触发时由租户管理员兜底（ADR-0032）
    Long approverId = resolveApprover(tid, body.approverId());
    if (approverId == null) {
      throw new ApproverResolveException("无法解析审批人（租户无可用用户）");
    }
    ApprovalRequest a =
        new ApprovalRequest(
            tid,
            parseTypeRequired(body.type()),
            body.title().trim(),
            body.amount(),
            requesterId,
            approverId);
    a.setDetail(blankToNull(body.detail()));
    ApprovalRequest saved = repo.save(a);
    auditService.recordSuccess(
        tid,
        requesterId,
        ApprovalAuditLog.Action.CREATE,
        saved.getId(),
        "发起审批 " + saved.getTitle(),
        clientIp(req),
        userAgent(req),
        req.getHeader("X-Request-Id"));
    return ResponseEntity.ok(saved);
  }

  @Operation(summary = "审批操作审计（租户内 · append-only · 阶段 2）")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Audit logs (newest first)"),
    @ApiResponse(responseCode = "401", description = "TENANT_CONTEXT_REQUIRED")
  })
  @GetMapping("/audit-logs")
  public ResponseEntity<List<ApprovalAuditLog>> auditLogs(
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestParam(defaultValue = "100") int limit) {
    Long tid = requireTenant(tenantHeader);
    int capped = Math.max(1, Math.min(limit, 500));
    List<ApprovalAuditLog> logs = auditRepo.findByTenantIdOrderByCreatedAtDesc(tid);
    return ResponseEntity.ok(logs.size() > capped ? logs.subList(0, capped) : logs);
  }

  @Operation(summary = "通过（PENDING → APPROVED · 仅审批人）")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Approved"),
    @ApiResponse(responseCode = "403", description = "FORBIDDEN（非审批人）"),
    @ApiResponse(responseCode = "404", description = "Not found (or cross-tenant)"),
    @ApiResponse(responseCode = "409", description = "ALREADY_DECIDED（非待审批）"),
    @ApiResponse(responseCode = "401", description = "TENANT_CONTEXT_REQUIRED")
  })
  @PostMapping("/{id}/approve")
  public ResponseEntity<ApprovalRequest> approve(
      @Parameter(description = "Approval id", example = "1") @PathVariable Long id,
      @Valid @RequestBody(required = false) DecideRequest body,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userHeader,
      HttpServletRequest req) {
    Long tid = requireTenant(tenantHeader);
    Long userId = requireUserId(userHeader);
    ApprovalRequest a = findTenantRequest(id, tid);
    requirePending(a, "approve");
    requireApprover(a, userId);
    a.setStatus(ApprovalRequest.Status.APPROVED);
    a.setComment(blankToNull(body == null ? null : body.comment()));
    decide(a, userId);
    ApprovalRequest saved = repo.save(a);
    auditService.recordSuccess(
        tid,
        userId,
        ApprovalAuditLog.Action.APPROVE,
        saved.getId(),
        "通过审批 " + saved.getTitle(),
        clientIp(req),
        userAgent(req),
        req.getHeader("X-Request-Id"));
    return ResponseEntity.ok(saved);
  }

  @Operation(summary = "驳回（PENDING → REJECTED · 仅审批人 · comment 必填）")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Rejected"),
    @ApiResponse(responseCode = "403", description = "FORBIDDEN（非审批人）"),
    @ApiResponse(responseCode = "404", description = "Not found (or cross-tenant)"),
    @ApiResponse(responseCode = "409", description = "ALREADY_DECIDED（非待审批）"),
    @ApiResponse(responseCode = "401", description = "TENANT_CONTEXT_REQUIRED")
  })
  @PostMapping("/{id}/reject")
  public ResponseEntity<ApprovalRequest> reject(
      @Parameter(description = "Approval id", example = "1") @PathVariable Long id,
      @Valid @RequestBody RejectRequest body,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userHeader,
      HttpServletRequest req) {
    Long tid = requireTenant(tenantHeader);
    Long userId = requireUserId(userHeader);
    ApprovalRequest a = findTenantRequest(id, tid);
    requirePending(a, "reject");
    requireApprover(a, userId);
    a.setStatus(ApprovalRequest.Status.REJECTED);
    a.setComment(body.comment().trim());
    decide(a, userId);
    ApprovalRequest saved = repo.save(a);
    auditService.recordSuccess(
        tid,
        userId,
        ApprovalAuditLog.Action.REJECT,
        saved.getId(),
        "驳回审批 " + saved.getTitle() + "（" + body.comment().trim() + "）",
        clientIp(req),
        userAgent(req),
        req.getHeader("X-Request-Id"));
    return ResponseEntity.ok(saved);
  }

  @Operation(summary = "撤销（PENDING → CANCELLED · 仅发起人）")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cancelled"),
    @ApiResponse(responseCode = "403", description = "FORBIDDEN（非发起人）"),
    @ApiResponse(responseCode = "404", description = "Not found (or cross-tenant)"),
    @ApiResponse(responseCode = "409", description = "ALREADY_DECIDED（非待审批）"),
    @ApiResponse(responseCode = "401", description = "TENANT_CONTEXT_REQUIRED")
  })
  @PostMapping("/{id}/cancel")
  public ResponseEntity<ApprovalRequest> cancel(
      @Parameter(description = "Approval id", example = "1") @PathVariable Long id,
      @Valid @RequestBody(required = false) DecideRequest body,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userHeader,
      HttpServletRequest req) {
    Long tid = requireTenant(tenantHeader);
    Long userId = requireUserId(userHeader);
    ApprovalRequest a = findTenantRequest(id, tid);
    requirePending(a, "cancel");
    if (!a.getRequesterId().equals(userId)) {
      throw new ApprovalForbiddenException("只有发起人才能撤销审批");
    }
    a.setStatus(ApprovalRequest.Status.CANCELLED);
    a.setComment(blankToNull(body == null ? null : body.comment()));
    decide(a, userId);
    ApprovalRequest saved = repo.save(a);
    auditService.recordSuccess(
        tid,
        userId,
        ApprovalAuditLog.Action.CANCEL,
        saved.getId(),
        "撤销审批 " + saved.getTitle(),
        clientIp(req),
        userAgent(req),
        req.getHeader("X-Request-Id"));
    return ResponseEntity.ok(saved);
  }

  @Operation(summary = "Health probe")
  @GetMapping("/_health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "approval");
  }

  // ============================================================
  // 工具（ADR-0025 安全关键 + ADR-0032 状态机）
  // ============================================================

  private ApprovalRequest findTenantRequest(Long id, Long tenantId) {
    Optional<ApprovalRequest> opt = repo.findById(id);
    if (opt.isEmpty() || !tenantMatches(opt.get(), tenantId)) {
      throw new NotFoundException("审批单不存在");
    }
    return opt.get();
  }

  private void requirePending(ApprovalRequest a, String action) {
    if (a.getStatus() != ApprovalRequest.Status.PENDING) {
      throw new AlreadyDecidedException("审批单已" + statusText(a.getStatus()) + "，无法" + action);
    }
  }

  private void requireApprover(ApprovalRequest a, Long userId) {
    if (!a.getApproverId().equals(userId)) {
      throw new ApprovalForbiddenException("只有被指定的审批人才能审批该单据");
    }
  }

  private void decide(ApprovalRequest a, Long userId) {
    a.setDecidedBy(userId);
    a.setDecidedAt(Instant.now());
    a.setUpdatedAt(Instant.now());
  }

  private String statusText(ApprovalRequest.Status s) {
    return switch (s) {
      case APPROVED -> "通过";
      case REJECTED -> "驳回";
      case CANCELLED -> "撤销";
      default -> "处理";
    };
  }

  private Long requireTenant(String header) {
    if (header == null || header.isBlank()) {
      throw new TenantContextRequiredException();
    }
    try {
      return Long.parseLong(header);
    } catch (NumberFormatException e) {
      throw new TenantContextRequiredException();
    }
  }

  private boolean tenantMatches(ApprovalRequest a, Long tenantId) {
    return a.getTenantId().equals(tenantId);
  }

  private Long requireUserId(String header) {
    Long userId = parseUserId(header);
    if (userId == null) {
      throw new ApprovalForbiddenException("X-User-Id 缺失");
    }
    return userId;
  }

  private ApprovalRequest.Type parseType(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return ApprovalRequest.Type.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private ApprovalRequest.Type parseTypeRequired(String value) {
    ApprovalRequest.Type t = parseType(value);
    if (t == null) throw new InvalidTypeException(value);
    return t;
  }

  private ApprovalRequest.Status parseStatus(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return ApprovalRequest.Status.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null; // 非法状态按不过滤处理（宽松）
    }
  }

  private Long parseUserId(String header) {
    if (header == null || header.isBlank()) return null;
    try {
      return Long.parseLong(header);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value.trim();
  }

  /** 阶段 2：审批人解析 —— 显式指定优先；否则自动选租户管理员（业务挂接 · ADR-0032） */
  private Long resolveApprover(Long tenantId, Long requested) {
    if (requested != null) return requested;
    try {
      List<UserView> users = userClient.listTenantUsers(String.valueOf(tenantId));
      if (users == null || users.isEmpty()) return null;
      return users.stream()
          .filter(u -> u.roles() != null && u.roles().contains("TENANT_ADMIN"))
          .findFirst()
          .map(UserView::id)
          .orElseGet(
              () ->
                  users.stream()
                      .filter(u -> u.roles() != null && u.roles().contains("PLATFORM_ADMIN"))
                      .findFirst()
                      .map(UserView::id)
                      .orElseGet(() -> users.stream().findFirst().map(UserView::id).orElse(null)));
    } catch (Exception e) {
      return null; // user 服务不可用 → 降级为无法解析（调用方可不阻塞业务）
    }
  }

  private String clientIp(HttpServletRequest req) {
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
    return req.getRemoteAddr();
  }

  private String userAgent(HttpServletRequest req) {
    String ua = req.getHeader("User-Agent");
    return (ua == null || ua.isBlank()) ? null : (ua.length() > 255 ? ua.substring(0, 255) : ua);
  }

  /** 404（跨租户或不存在，统一走 404 防枚举） */
  static class NotFoundException extends RuntimeException {
    NotFoundException(String message) {
      super(message);
    }
  }

  /** 400：审批人无法解析（业务挂接自动触发时租户无用户 / user 服务不可用） */
  static class ApproverResolveException extends RuntimeException {
    ApproverResolveException(String message) {
      super(message);
    }
  }

  /** 发起审批请求 DTO */
  public record CreateApprovalRequest(
      @NotBlank @Size(max = 16) String type,
      @NotBlank @Size(max = 128) String title,
      @DecimalMin("0.01") BigDecimal amount,
      @Size(max = 2000) String detail,
      @Min(1) Long approverId) {}

  /** 审批/撤销请求 DTO（approve/cancel 时 comment 可选） */
  public record DecideRequest(@Size(max = 500) String comment) {}

  /** 驳回请求 DTO（comment 必填） */
  public record RejectRequest(@NotBlank @Size(max = 500) String comment) {}
}
