package cn.huntercat.lieshoucloudpro.auth.feign;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.cloud.openfeign.FeignClient;

import cn.huntercat.lieshoucloudpro.auth.feign.dto.UserAuthView;
import java.util.Map;

/**
 * auth-service 调 user-service 拉 user 视图（含 passwordHash）.
 *
 * <p>Spring Cloud OpenFeign 自动从 Nacos 发现 user-service; 若 Nacos 未起, 可用 fallback 替身.
 */
@FeignClient(name = "lieshoucloud-user", path = "/api/users")
public interface UserAuthClient {

  /**
   * 按租户 + username 查询（Phase 8 · ADR-0022）. user-service 暴露 {@code
   * /api/users/auth/by-tenant/{code}/{username}}.
   */
  @GetMapping("/auth/by-tenant/{tenantCode}/{username}")
  UserAuthView findByTenantAndUsername(
      @PathVariable String tenantCode, @PathVariable String username);

  /** Phase 6: 登录成功后回写 last_login_at（失败由调用方吞掉，不影响登录主流程）. */
  @PostMapping("/{id}/login-marker")
  void markLastLogin(@PathVariable Long id);

  // ============================================================
  // Phase 8 · 认证体系扩展（ADR-0023）
  // ============================================================

  /** 发送验证码 */
  @PostMapping("/verification/send")
  void sendVerificationCode(@RequestBody Map<String, String> body);

  /** 校验验证码（一次性，校验后作废） */
  @PostMapping("/verification/verify")
  void verifyVerificationCode(@RequestBody Map<String, String> body);

  /** 按手机号查鉴权视图 */
  @GetMapping("/auth/by-phone/{phone}")
  UserAuthView findByPhone(@PathVariable String phone);

  /** 按邮箱查鉴权视图 */
  @GetMapping("/auth/by-email/{email}")
  UserAuthView findByEmail(@PathVariable String email);

  /** 创建用户（注册用；body 对齐 user-service CreateUserRequest） */
  @PostMapping("")
  Map<String, Object> createUser(@RequestBody Map<String, String> body);

  /** 重置密码（body: {password}，对齐 user-service UpdateUserRequest 部分字段） */
  @PutMapping("/{id}")
  void updateUserPassword(@PathVariable Long id, @RequestBody Map<String, String> body);
}
