package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.huntercat.lieshoucloudpro.user.service.TenantAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

/**
 * 租户访问上下文（集团版 · Phase 1 §3.2 子公司切换器数据源）.
 *
 * <p>返回用户可访问的租户列表：主属租户（users.tenant_id，天然可访问）+ 跨公司授权 （user_tenant_grants）。前端子公司切换器与 auth-service
 * 切换租户都依赖此端点。
 *
 * <p>auth-service 内部调用 {@code /api/tenant-access/user/{userId}} 时透传 X-User-Id（同用户） 放行；外部调用须
 * PLATFORM_ADMIN 或本人。业务组装逻辑见 {@link TenantAccessService}（自本 Controller 下沉，同 UserService 先例）。
 */
@RestController
@RequestMapping("/api/tenant-access")
@Tag(name = "TenantAccess", description = "用户可访问租户上下文（集团版子公司切换器）")
public class TenantAccessController {

  private final TenantAccessService tenantAccessService;

  public TenantAccessController(TenantAccessService tenantAccessService) {
    this.tenantAccessService = tenantAccessService;
  }

  @Operation(summary = "当前用户可访问租户列表（子公司切换器）")
  @GetMapping("/me")
  public ResponseEntity<?> me(
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
    Long uid = TenantContext.parseLong(userIdHeader);
    if (uid == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "UNAUTHORIZED", "message", "missing X-User-Id"));
    }
    return build(uid);
  }

  @Operation(summary = "指定用户可访问租户列表（本人或 PLATFORM_ADMIN；auth 内部透传本人）")
  @GetMapping("/user/{userId}")
  public ResponseEntity<?> forUser(
      @PathVariable Long userId,
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
      @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {
    Long caller = TenantContext.parseLong(userIdHeader);
    boolean self = caller != null && caller.equals(userId);
    if (!self && !AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "FORBIDDEN", "message", "requires PLATFORM_ADMIN or self"));
    }
    return build(userId);
  }

  /** 组装可访问租户列表（业务异常由 {@link UserBizExceptionAdvice} 统一转译 HTTP 状态 + 错误码） */
  private ResponseEntity<?> build(Long userId) {
    return ResponseEntity.ok(tenantAccessService.buildAccess(userId));
  }
}
