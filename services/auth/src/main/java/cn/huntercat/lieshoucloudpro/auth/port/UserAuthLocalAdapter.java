package cn.huntercat.lieshoucloudpro.auth.port;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cn.huntercat.lieshoucloudpro.auth.feign.dto.UserAuthView;
import cn.huntercat.lieshoucloudpro.user.domain.VerificationCode;
import cn.huntercat.lieshoucloudpro.user.service.UserBizException;
import cn.huntercat.lieshoucloudpro.user.service.UserService;
import cn.huntercat.lieshoucloudpro.user.service.VerificationService;
import cn.huntercat.lieshoucloudpro.user.service.dto.UserDtos.CreateUserRequest;
import cn.huntercat.lieshoucloudpro.user.service.dto.UserDtos.UpdateUserRequest;
import feign.FeignException;
import java.util.Map;

/**
 * monolith 模式适配器（ARCHITECTURE.md §4.2 ②）：进程内直接调用 user 领域服务（{@link UserService} + {@link
 * VerificationService}），零网络开销。
 *
 * <p>激活条件：{@code app.deploy-mode=monolith}。错误语义转译为 {@link FeignException} 子类（404 / 403 / 400， body
 * 带标准化错误码）——AuthService 现有 {@code catch (FeignException e) { e.status() ... }} 逻辑零修改。
 */
@Component
@ConditionalOnProperty(name = "app.deploy-mode", havingValue = "monolith")
public class UserAuthLocalAdapter implements UserAuthPort {

  private final UserService users;
  private final VerificationService verification;

  public UserAuthLocalAdapter(UserService users, VerificationService verification) {
    this.users = users;
    this.verification = verification;
  }

  @Override
  public UserAuthView findByTenantAndUsername(String tenantCode, String username) {
    try {
      return toAuthView(users.authByTenantAndUsername(tenantCode, username));
    } catch (UserBizException e) {
      throw FeignErrors.from(e.getStatus(), e.getError());
    }
  }

  @Override
  public void markLastLogin(Long id) {
    users.markLastLogin(id); // 幂等，失败不抛（与 AuthService 静默降级一致）
  }

  @Override
  public void sendVerificationCode(Map<String, String> body) {
    try {
      verification.send(
          VerificationCode.Channel.valueOf(body.get("channel")),
          body.get("target"),
          VerificationCode.Purpose.valueOf(body.get("purpose")));
    } catch (IllegalArgumentException e) {
      throw FeignErrors.from(400, "INVALID_CHANNEL_OR_PURPOSE");
    } catch (IllegalStateException e) {
      throw FeignErrors.from(400, e.getMessage() == null ? "SEND_TOO_FREQUENT" : e.getMessage());
    }
  }

  @Override
  public void verifyVerificationCode(Map<String, String> body) {
    try {
      verification.verify(
          VerificationCode.Channel.valueOf(body.get("channel")),
          body.get("target"),
          VerificationCode.Purpose.valueOf(body.get("purpose")),
          body.get("code"));
    } catch (IllegalArgumentException e) {
      throw FeignErrors.from(400, e.getMessage() == null ? "INVALID_CODE" : e.getMessage());
    }
  }

  @Override
  public UserAuthView findByPhone(String phone) {
    try {
      return toAuthView(users.authByPhone(phone));
    } catch (UserBizException e) {
      throw FeignErrors.from(e.getStatus(), e.getError());
    }
  }

  @Override
  public UserAuthView findByEmail(String email) {
    try {
      return toAuthView(users.authByEmail(email));
    } catch (UserBizException e) {
      throw FeignErrors.from(e.getStatus(), e.getError());
    }
  }

  @Override
  public Map<String, Object> createUser(Map<String, String> body) {
    if (body.get("username") == null
        || body.get("displayName") == null
        || body.get("password") == null) {
      throw FeignErrors.from(400, "VALIDATION_FAILED");
    }
    try {
      return users.create(
          new CreateUserRequest(
              body.get("username"),
              body.get("displayName"),
              body.get("password"),
              body.get("email"),
              body.get("phone"),
              body.get("tenantCode"),
              body.get("inviteCode")),
          null,
          null,
          null);
    } catch (UserBizException e) {
      throw FeignErrors.from(e.getStatus(), e.getError());
    }
  }

  @Override
  public void updateUserPassword(Long id, Map<String, String> body) {
    try {
      users.update(
          id,
          new UpdateUserRequest(null, null, null, null, null, body.get("password")),
          null,
          null,
          null);
    } catch (UserBizException e) {
      throw FeignErrors.from(e.getStatus(), e.getError());
    }
  }

  private static UserAuthView toAuthView(cn.huntercat.lieshoucloudpro.user.web.dto.UserAuthView v) {
    return new UserAuthView(
        v.id(),
        v.tenantId(),
        v.tenantCode(),
        v.tenantName(),
        v.tenantEdition(),
        v.username(),
        v.displayName(),
        v.passwordHash(),
        v.roles(),
        v.status(),
        v.permissions());
  }
}
