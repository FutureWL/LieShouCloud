package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.huntercat.lieshoucloudpro.user.domain.AuditLog;
import cn.huntercat.lieshoucloudpro.user.domain.AuditLog.Action;
import cn.huntercat.lieshoucloudpro.user.domain.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 审计日志查询（append-only，只读端点）.
 *
 * <p>租户模型与 user-service 一致：有 X-Tenant-Id → 只返回该租户日志；无租户上下文 （平台管理）→ 需要 PLATFORM_ADMIN，返回全部。
 */
@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit", description = "操作审计查询（append-only）")
public class AuditController {

  private final AuditLogRepository repo;

  public AuditController(AuditLogRepository repo) {
    this.repo = repo;
  }

  @Operation(
      summary = "List audit logs",
      description = "Tenant-scoped; platform admin without tenant sees all.")
  @ApiResponse(responseCode = "200", description = "Audit logs (newest first)")
  @GetMapping
  public ResponseEntity<?> list(
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
      @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String resourceType,
      @RequestParam(required = false) String outcome,
      @RequestParam(defaultValue = "100") int limit) {
    Long tid = TenantContext.parseLong(tenantHeader);
    if (tid == null && !AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "FORBIDDEN"));
    }

    Action actionFilter = parseAction(action);
    AuditLog.Outcome outcomeFilter = parseOutcome(outcome);
    int capped = Math.min(Math.max(limit, 1), 500);

    Stream<AuditLog> stream =
        tid != null
            ? repo.findByTenantIdOrderByCreatedAtDesc(tid).stream()
            : repo.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
    if (actionFilter != null) stream = stream.filter(l -> l.getAction() == actionFilter);
    if (outcomeFilter != null) stream = stream.filter(l -> l.getOutcome() == outcomeFilter);
    if (resourceType != null && !resourceType.isBlank()) {
      stream =
          stream.filter(
              l -> Objects.equals(l.getResourceType(), resourceType.trim().toUpperCase()));
    }
    List<AuditLog> rows = stream.limit(capped).toList();
    return ResponseEntity.ok(rows);
  }

  @Operation(summary = "Count audit logs")
  @GetMapping("/count")
  public ResponseEntity<?> count(
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
      @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {
    Long tid = TenantContext.parseLong(tenantHeader);
    if (tid == null && !AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "FORBIDDEN"));
    }
    return ResponseEntity.ok(tid != null ? repo.countByTenantId(tid) : repo.count());
  }

  private static Action parseAction(String s) {
    if (s == null || s.isBlank()) return null;
    try {
      return Action.valueOf(s.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static AuditLog.Outcome parseOutcome(String s) {
    if (s == null || s.isBlank()) return null;
    try {
      return AuditLog.Outcome.valueOf(s.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
