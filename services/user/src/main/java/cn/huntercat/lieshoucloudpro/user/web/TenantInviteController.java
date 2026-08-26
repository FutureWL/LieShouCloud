package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import cn.huntercat.lieshoucloudpro.user.domain.TenantInvite;
import cn.huntercat.lieshoucloudpro.user.domain.TenantInviteRepository;
import cn.huntercat.lieshoucloudpro.user.domain.TenantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 邀请码端点（ADR-0023 Phase 2 · 租户管理员生成邀请码）.
 *
 * <p>完整路径：{@code /api/tenants/{tenantId}/invites/**}（由 gateway 转发，需 JWT——平台运营操作）。
 */
@RestController
@RequestMapping("/api/tenants/{tenantId}/invites")
@Tag(name = "TenantInvite", description = "Invite codes for tenant self-registration")
public class TenantInviteController {

  /** 邀请码字符集（去易混淆 I/O/0/1） */
  private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

  private static final int CODE_LENGTH = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final TenantRepository tenantRepo;
  private final TenantInviteRepository inviteRepo;

  public TenantInviteController(TenantRepository tenantRepo, TenantInviteRepository inviteRepo) {
    this.tenantRepo = tenantRepo;
    this.inviteRepo = inviteRepo;
  }

  /** RBAC（ADR-0024）：平台管理员 或 本租户管理员；否则 403 */
  private boolean authorized(String rolesHeader, String tenantHeader, Long tenantId) {
    if (AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) return true;
    Long headerTenant = TenantContext.parseLong(tenantHeader);
    return AuthRoles.hasAny(rolesHeader, AuthRoles.TENANT_ADMIN)
        && headerTenant != null
        && headerTenant.equals(tenantId);
  }

  private ResponseEntity<Object> forbidden() {
    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
        .body(
            Map.of(
                "error", "FORBIDDEN", "message", "requires PLATFORM_ADMIN or tenant TENANT_ADMIN"));
  }

  @Operation(summary = "Generate invite code for tenant")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Invite created with code"),
    @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  @PostMapping
  public ResponseEntity<?> create(
      @Parameter(description = "Tenant id", example = "1") @PathVariable Long tenantId,
      @Valid @RequestBody CreateInviteRequest body,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader) {
    if (!authorized(rolesHeader, tenantHeader, tenantId)) {
      return forbidden();
    }
    if (tenantRepo.findById(tenantId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    String role = (body.role() == null || body.role().isBlank()) ? "USER" : body.role();
    if (!List.of("USER", "ADMIN").contains(role)) {
      return ResponseEntity.badRequest().body(Map.of("error", "INVALID_ROLE"));
    }
    Instant expiresAt =
        (body.expiresInDays() == null || body.expiresInDays() <= 0)
            ? null
            : Instant.now().plusSeconds(body.expiresInDays() * 86400L);
    String code = generateUniqueCode();
    TenantInvite invite = new TenantInvite(tenantId, code, role, expiresAt, body.maxUses(), null);
    return ResponseEntity.ok(inviteRepo.save(invite));
  }

  @Operation(summary = "List invites of tenant")
  @ApiResponse(responseCode = "200", description = "List of invites (newest first)")
  @GetMapping
  public ResponseEntity<?> list(
      @Parameter(description = "Tenant id", example = "1") @PathVariable Long tenantId,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader) {
    if (!authorized(rolesHeader, tenantHeader, tenantId)) {
      return forbidden();
    }
    if (tenantRepo.findById(tenantId).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(inviteRepo.findByTenantIdOrderByCreatedAtDesc(tenantId));
  }

  @Operation(summary = "Revoke invite code")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Revoked"),
    @ApiResponse(responseCode = "404", description = "Invite not found / belongs to other tenant")
  })
  @PostMapping("/{id}/revoke")
  public ResponseEntity<?> revoke(
      @Parameter(description = "Tenant id", example = "1") @PathVariable Long tenantId,
      @Parameter(description = "Invite id", example = "1") @PathVariable Long id,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader) {
    if (!authorized(rolesHeader, tenantHeader, tenantId)) {
      return forbidden();
    }
    java.util.Optional<TenantInvite> opt = inviteRepo.findById(id);
    if (opt.isEmpty() || !opt.get().getTenantId().equals(tenantId)) {
      return ResponseEntity.notFound().build();
    }
    TenantInvite inv = opt.get();
    inv.setRevokedAt(Instant.now());
    inviteRepo.save(inv);
    return ResponseEntity.noContent().build();
  }

  private String generateUniqueCode() {
    for (int attempt = 0; attempt < 20; attempt++) {
      StringBuilder sb = new StringBuilder(CODE_LENGTH);
      for (int i = 0; i < CODE_LENGTH; i++) {
        sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
      }
      String code = sb.toString();
      if (inviteRepo.findByCode(code).isEmpty()) {
        return code;
      }
    }
    throw new IllegalStateException("FAILED_TO_GENERATE_INVITE");
  }

  /** Phase 8: CreateInviteRequest DTO（内联；全部可选） */
  public record CreateInviteRequest(String role, Integer expiresInDays, Integer maxUses) {}
}
