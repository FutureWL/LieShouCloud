package cn.huntercat.lieshoucloudpro.auth.port;

import cn.huntercat.lieshoucloudpro.auth.feign.dto.UserAuthView;
import java.util.Map;

/**
 * 服务间调用抽象层（ARCHITECTURE.md §4.2 · Port 模式）。
 *
 * <p>auth → user 鉴权契约（认证视图 / 验证码 / 注册 / 改密）。业务代码只依赖本接口（= 未来服务契约），不感知单体（monolith） 还是微服务（msa）形态：
 *
 * <ul>
 *   <li>{@code monolith}：进程内直接调用 user 领域服务（{@link UserAuthLocalAdapter}）
 *   <li>{@code msa}（缺省）：Feign 适配器跨进程调用（{@link UserAuthFeignAdapter}）
 * </ul>
 *
 * <p>错误语义与 msa 的 HTTP 状态一致，由适配器转译为 {@link feign.FeignException} 子类（404 / 403 / 400）——
 * 调用方（AuthService）现有 catch 逻辑零修改。
 */
public interface UserAuthPort {

  /** 按租户 + username 查鉴权视图（含 passwordHash）；租户停用 → 403 TENANT_DISABLED，不存在 → 404 */
  UserAuthView findByTenantAndUsername(String tenantCode, String username);

  /** 登录成功回写 last_login_at（幂等，失败不抛） */
  void markLastLogin(Long id);

  /** 发送验证码（失败 → 400/服务不可用） */
  void sendVerificationCode(Map<String, String> body);

  /** 校验验证码（一次性，校验后作废；失败 → 400） */
  void verifyVerificationCode(Map<String, String> body);

  /** 按手机号查鉴权视图；不存在 → 404 */
  UserAuthView findByPhone(String phone);

  /** 按邮箱查鉴权视图；不存在 → 404 */
  UserAuthView findByEmail(String email);

  /** 创建用户（注册用）；业务失败 → 400（INVALID_INVITE / USERNAME_TAKEN / TENANT_NOT_FOUND 等透传） */
  Map<String, Object> createUser(Map<String, String> body);

  /** 重置密码（body: {password}）；不存在 → 404 */
  void updateUserPassword(Long id, Map<String, String> body);
}
