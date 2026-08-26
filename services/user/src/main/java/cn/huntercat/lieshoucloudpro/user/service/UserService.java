package cn.huntercat.lieshoucloudpro.user.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import cn.huntercat.lieshoucloudpro.user.domain.AuditLog;
import cn.huntercat.lieshoucloudpro.user.domain.Role;
import cn.huntercat.lieshoucloudpro.user.domain.RoleRepository;
import cn.huntercat.lieshoucloudpro.user.domain.Tenant;
import cn.huntercat.lieshoucloudpro.user.domain.TenantInvite;
import cn.huntercat.lieshoucloudpro.user.domain.TenantInviteRepository;
import cn.huntercat.lieshoucloudpro.user.domain.TenantRepository;
import cn.huntercat.lieshoucloudpro.user.domain.User;
import cn.huntercat.lieshoucloudpro.user.domain.UserRepository;
import cn.huntercat.lieshoucloudpro.user.service.dto.UserDtos.CreateUserRequest;
import cn.huntercat.lieshoucloudpro.user.service.dto.UserDtos.UpdateUserRequest;
import cn.huntercat.lieshoucloudpro.user.web.AuthRoles;
import cn.huntercat.lieshoucloudpro.user.web.dto.UserAuthView;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户域业务服务（ARCHITECTURE.md §4.2 Port 抽象前置：业务逻辑自 UserController 下沉）.
 *
 * <p>承载用户 CRUD / 鉴权视图查询 / 登录回写等全部业务规则（多租户行级过滤、邀请码、租户状态、审计六要素）， Controller 只保留 HTTP 适配（header 解析 +
 * 状态码转译），monolith Local 适配器（admin/auth/approval → user） 进程内直接注入本服务。
 *
 * <p>业务失败抛 {@link UserBizException}（status + 标准化错误码），由 Controller 转 HTTP、Local 适配器转 Feign
 * 语义（404/403/400），调用方 catch 逻辑不变。
 */
@Service
public class UserService {

  /** 默认租户编码（兼容未显式传租户的调用） · ADR-0022 */
  public static final String DEFAULT_TENANT_CODE = "huntercat";

  private final UserRepository repo;
  private final TenantRepository tenantRepo;
  private final TenantInviteRepository inviteRepo;
  private final RoleRepository roleRepo;
  private final AuditService audit;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public UserService(
      UserRepository repo,
      TenantRepository tenantRepo,
      TenantInviteRepository inviteRepo,
      RoleRepository roleRepo,
      AuditService audit) {
    this.repo = repo;
    this.tenantRepo = tenantRepo;
    this.inviteRepo = inviteRepo;
    this.roleRepo = roleRepo;
    this.audit = audit;
  }

  // ============================================================
  // 用户 CRUD（多租户行级过滤 · ADR-0022）
  // ============================================================

  /** 租户用户列表：tid 非空 → 该租户；否则需平台管理员。 */
  public List<User> list(Long tid, boolean platformAdmin) {
    if (tid != null) {
      return repo.findByTenantId(tid);
    }
    if (!platformAdmin) {
      throw new UserBizException(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "FORBIDDEN");
    }
    return repo.findAll();
  }

  /** 用户计数：租户内或平台管理员。 */
  public Long count(Long tid, boolean platformAdmin) {
    if (tid != null) {
      return repo.countByTenantId(tid);
    }
    if (!platformAdmin) {
      throw new UserBizException(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "FORBIDDEN");
    }
    return repo.count();
  }

  /** 按 id 查用户（跨租户 → 不泄露存在性，返回 empty）。 */
  public Optional<User> findById(Long id, Long tid) {
    return repo.findById(id).filter(u -> tenantMatches(u, tid));
  }

  /** 按 username 查用户（admin-service 跨服务用；无租户过滤）。 */
  public Optional<User> findByUsername(String username) {
    return repo.findByUsername(username);
  }

  /** 创建用户（邀请码优先 → 租户内强制 → 常规注册）。 */
  public Map<String, Object> create(
      CreateUserRequest body, Long forcedTenantId, Long userId, HttpServletRequest http) {
    Tenant tenant;
    String role = "USER";
    if (body.inviteCode() != null && !body.inviteCode().isBlank()) {
      // —— 邀请码优先（ADR-0023 Phase 2）：租户/角色来自邀请码 ——
      TenantInvite invite = inviteRepo.findByCode(body.inviteCode()).orElse(null);
      if (invite == null || !invite.isValid()) {
        throw new UserBizException(
            HttpStatus.BAD_REQUEST.value(), "INVALID_INVITE", "INVALID_INVITE");
      }
      // 租户内请求强制：邀请码租户必须与请求租户一致，否则拒绝
      if (forcedTenantId != null && !invite.getTenantId().equals(forcedTenantId)) {
        throw new UserBizException(
            HttpStatus.FORBIDDEN.value(), "INVITE_TENANT_MISMATCH", "INVITE_TENANT_MISMATCH");
      }
      tenant = tenantRepo.findById(invite.getTenantId()).orElse(null);
      if (tenant == null || tenant.getStatus() != Tenant.Status.ACTIVE) {
        throw new UserBizException(
            HttpStatus.BAD_REQUEST.value(), "TENANT_NOT_ACTIVE", "TENANT_NOT_ACTIVE");
      }
      role = invite.getRole();
      invite.consume();
      inviteRepo.save(invite);
    } else if (forcedTenantId != null) {
      // —— 租户内请求强制：只能用请求的租户创建（忽略 tenantCode）——
      tenant = tenantRepo.findById(forcedTenantId).orElse(null);
      if (tenant == null || tenant.getStatus() != Tenant.Status.ACTIVE) {
        throw new UserBizException(
            HttpStatus.BAD_REQUEST.value(), "TENANT_NOT_ACTIVE", "TENANT_NOT_ACTIVE");
      }
    } else {
      // —— 常规注册：tenantCode 指定租户（默认 huntercat）——
      String code =
          (body.tenantCode() == null || body.tenantCode().isBlank())
              ? DEFAULT_TENANT_CODE
              : body.tenantCode();
      tenant = tenantRepo.findByCode(code).orElse(null);
      if (tenant == null) {
        throw new UserBizException(HttpStatus.BAD_REQUEST.value(), "TENANT_NOT_FOUND", code);
      }
    }
    if (repo.existsByTenantIdAndUsername(tenant.getId(), body.username())) {
      throw new UserBizException(HttpStatus.BAD_REQUEST.value(), "USERNAME_TAKEN", body.username());
    }
    User u = new User();
    u.setTenantId(tenant.getId());
    u.setUsername(body.username());
    u.setDisplayName(body.displayName());
    u.setEmail(body.email());
    u.setPhone(body.phone());
    u.setPasswordHash(encoder.encode(body.password()));
    u.setRoles(List.of(roleByCode(role)));
    User saved = repo.save(u);
    audit.recordSuccess(
        tenant.getId(),
        userId,
        AuditLog.Action.CREATE,
        "USER",
        saved.getId(),
        "创建用户 " + saved.getUsername(),
        http);
    // 返回带租户信息（tenantCode/tenantName/tenantEdition）——auth-service 注册后直接签发 JWT 需要；
    // 不返回 passwordHash（User 实体的敏感字段不外泄）。
    return Map.of(
        "id", saved.getId(),
        "tenantId", saved.getTenantId(),
        "tenantCode", tenant.getCode(),
        "tenantName", tenant.getName(),
        "tenantEdition", tenant.getEdition() == null ? "GENERIC" : tenant.getEdition().name(),
        "username", saved.getUsername(),
        "displayName", saved.getDisplayName());
  }

  /** 更新用户（部分更新；跨租户/不存在 → empty）。 */
  public Optional<User> update(
      Long id, UpdateUserRequest body, Long tid, Long userId, HttpServletRequest http) {
    Optional<User> opt = repo.findById(id);
    if (opt.isEmpty() || !tenantMatches(opt.get(), tid)) {
      return Optional.empty();
    }
    User u = opt.get();
    if (body.displayName() != null && !body.displayName().isBlank()) {
      u.setDisplayName(body.displayName());
    }
    if (body.email() != null && !body.email().isBlank()) {
      u.setEmail(body.email());
    }
    if (body.phone() != null && !body.phone().isBlank()) {
      u.setPhone(body.phone());
    }
    if (body.status() != null && !body.status().isBlank()) {
      try {
        u.setStatus(User.Status.valueOf(body.status()));
      } catch (IllegalArgumentException e) {
        throw new UserBizException(
            HttpStatus.BAD_REQUEST.value(), "INVALID_STATUS", "INVALID_STATUS");
      }
    }
    if (body.roles() != null && body.roles().length > 0) {
      List<Role> newRoles =
          Arrays.stream(body.roles())
              .map(this::roleByCode)
              .filter(java.util.Objects::nonNull)
              .toList();
      if (!newRoles.isEmpty()) {
        u.setRoles(newRoles);
      }
    }
    if (body.password() != null && !body.password().isBlank()) {
      u.setPasswordHash(encoder.encode(body.password()));
    }
    User saved = repo.save(u);
    audit.recordSuccess(
        u.getTenantId(),
        userId,
        AuditLog.Action.UPDATE,
        "USER",
        saved.getId(),
        "更新用户 " + saved.getUsername(),
        http);
    return Optional.of(saved);
  }

  /** 删除用户（跨租户/不存在 → empty）。 */
  public Optional<User> delete(Long id, Long tid, Long userId, HttpServletRequest http) {
    Optional<User> opt = repo.findById(id);
    if (opt.isEmpty() || !tenantMatches(opt.get(), tid)) {
      return Optional.empty();
    }
    User u = opt.get();
    repo.deleteById(id);
    audit.recordSuccess(
        u.getTenantId(),
        userId,
        AuditLog.Action.DELETE,
        "USER",
        id,
        "删除用户 " + u.getUsername(),
        http);
    return Optional.of(u);
  }

  // ============================================================
  // 鉴权视图（service-to-service · auth-service 用）
  // ============================================================

  /** 按租户编码 + username 查鉴权视图（含 passwordHash；租户停用 → 403）。 */
  public UserAuthView authByTenantAndUsername(String tenantCode, String username) {
    Tenant tenant = tenantRepo.findByCode(tenantCode).orElse(null);
    if (tenant == null) {
      throw new UserBizException(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", username);
    }
    // Phase 8: 租户被停用 → 阻断该租户所有登录
    if (tenant.getStatus() != Tenant.Status.ACTIVE) {
      throw new UserBizException(HttpStatus.FORBIDDEN.value(), "TENANT_DISABLED", tenantCode);
    }
    return repo.findByTenantIdAndUsername(tenant.getId(), username)
        .map(u -> toAuthView(u))
        .orElseThrow(
            () -> new UserBizException(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", username));
  }

  /** 按手机号查鉴权视图（验证码登录 · ADR-0023）。 */
  public UserAuthView authByPhone(String phone) {
    return repo.findByPhone(phone)
        .map(this::toAuthView)
        .orElseThrow(
            () -> new UserBizException(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", phone));
  }

  /** 按邮箱查鉴权视图（验证码登录 · ADR-0023）。 */
  public UserAuthView authByEmail(String email) {
    return repo.findByEmail(email)
        .map(this::toAuthView)
        .orElseThrow(
            () -> new UserBizException(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", email));
  }

  /** 登录成功回写最近登录时间（幂等：用户不存在静默忽略，返回 false）。 */
  public boolean markLastLogin(Long id) {
    java.util.Optional<User> opt = repo.findById(id);
    opt.ifPresent(
        u -> {
          u.setLastLoginAt(Instant.now());
          repo.save(u);
        });
    return opt.isPresent();
  }

  /** 组装鉴权视图（含租户编码 + 角色 codes）。 */
  public UserAuthView toAuthView(User u) {
    Tenant tenant = tenantRepo.findById(u.getTenantId()).orElse(null);
    List<String> roleCodes =
        u.getRoles() == null || u.getRoles().isEmpty()
            ? List.of("USER")
            : u.getRoles().stream().map(Role::getCode).toList();
    return new UserAuthView(
        u.getId(),
        u.getTenantId(),
        tenant == null ? null : tenant.getCode(),
        tenant == null ? null : tenant.getName(),
        tenant == null || tenant.getEdition() == null ? null : tenant.getEdition().name(),
        u.getUsername(),
        u.getDisplayName(),
        u.getPasswordHash(),
        roleCodes,
        u.getStatus() == null ? "ACTIVE" : u.getStatus().name());
  }

  // ============================================================
  // 工具
  // ============================================================

  /** 资源是否属于当前租户（无租户上下文 = 平台管理，放行） */
  public boolean tenantMatches(User u, Long tenantHeader) {
    return tenantHeader == null || u.getTenantId().equals(tenantHeader);
  }

  /** 按角色 code 查 Role 实体（不存在返回 null） */
  private Role roleByCode(String code) {
    return roleRepo.findByCode(code).orElse(null);
  }

  /** 平台管理员判定（复用 AuthRoles，null 角色头 = 非平台） */
  public static boolean isPlatformAdmin(String rolesHeader) {
    return AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN);
  }
}
