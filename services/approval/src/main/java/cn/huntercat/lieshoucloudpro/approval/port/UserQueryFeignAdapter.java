package cn.huntercat.lieshoucloudpro.approval.port;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cn.huntercat.lieshoucloudpro.approval.feign.UserQueryClient;
import cn.huntercat.lieshoucloudpro.approval.feign.UserView;
import java.util.List;

/**
 * msa 模式适配器（ARCHITECTURE.md §4.2 ③）：委托 {@link UserQueryClient} 跨进程 HTTP 调用 user-service。
 *
 * <p>激活条件：{@code app.deploy-mode=msa}（缺省即 msa —— 当前 SaaS 形态默认，未配置时保持现状行为）。
 */
@Component
@ConditionalOnProperty(name = "app.deploy-mode", havingValue = "msa", matchIfMissing = true)
public class UserQueryFeignAdapter implements UserQueryPort {

  private final UserQueryClient delegate;

  public UserQueryFeignAdapter(UserQueryClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public List<UserView> listTenantUsers(String tenantId) {
    return delegate.listTenantUsers(tenantId);
  }

  @Override
  public UserView getUserById(Long id, String tenantId) {
    return delegate.getUserById(id, tenantId);
  }
}
