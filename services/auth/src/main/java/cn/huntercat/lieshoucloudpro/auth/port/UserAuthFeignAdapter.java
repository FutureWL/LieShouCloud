package cn.huntercat.lieshoucloudpro.auth.port;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cn.huntercat.lieshoucloudpro.auth.feign.UserAuthClient;
import cn.huntercat.lieshoucloudpro.auth.feign.dto.UserAuthView;
import java.util.Map;

/**
 * msa 模式适配器（ARCHITECTURE.md §4.2 ③）：委托 {@link UserAuthClient} 跨进程 HTTP 调用 user-service。
 *
 * <p>激活条件：{@code app.deploy-mode=msa}（缺省即 msa —— 当前 SaaS 形态默认，未配置时保持现状行为）。
 */
@Component
@ConditionalOnProperty(name = "app.deploy-mode", havingValue = "msa", matchIfMissing = true)
public class UserAuthFeignAdapter implements UserAuthPort {

  private final UserAuthClient delegate;

  public UserAuthFeignAdapter(UserAuthClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public UserAuthView findByTenantAndUsername(String tenantCode, String username) {
    return delegate.findByTenantAndUsername(tenantCode, username);
  }

  @Override
  public void markLastLogin(Long id) {
    delegate.markLastLogin(id);
  }

  @Override
  public void sendVerificationCode(Map<String, String> body) {
    delegate.sendVerificationCode(body);
  }

  @Override
  public void verifyVerificationCode(Map<String, String> body) {
    delegate.verifyVerificationCode(body);
  }

  @Override
  public UserAuthView findByPhone(String phone) {
    return delegate.findByPhone(phone);
  }

  @Override
  public UserAuthView findByEmail(String email) {
    return delegate.findByEmail(email);
  }

  @Override
  public Map<String, Object> createUser(Map<String, String> body) {
    return delegate.createUser(body);
  }

  @Override
  public void updateUserPassword(Long id, Map<String, String> body) {
    delegate.updateUserPassword(id, body);
  }
}
