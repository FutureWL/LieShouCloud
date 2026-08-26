package cn.huntercat.lieshoucloudpro.admin.port;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cn.huntercat.lieshoucloudpro.admin.feign.dto.UserDTO;
import cn.huntercat.lieshoucloudpro.user.domain.User;
import cn.huntercat.lieshoucloudpro.user.service.UserService;

/**
 * monolith 模式适配器（ARCHITECTURE.md §4.2 ②）：进程内直接调用 user 领域服务 {@link UserService}，零网络开销。
 *
 * <p>激活条件：{@code app.deploy-mode=monolith}。查询失败返回 {@code null}（等价 msa 下 Feign fallback 语义）； count
 * 无租户上下文需平台管理员（等价 msa 下 user-service 403 → 熔断 fallback 路径）。
 */
@Component
@ConditionalOnProperty(name = "app.deploy-mode", havingValue = "monolith")
public class UserQueryLocalAdapter implements UserQueryPort {

  private final UserService users;

  public UserQueryLocalAdapter(UserService users) {
    this.users = users;
  }

  @Override
  public Long count() {
    return users.count(null, UserService.isPlatformAdmin(null));
  }

  @Override
  public UserDTO findById(Long id) {
    return users.findById(id, null).map(UserQueryLocalAdapter::toDto).orElse(null);
  }

  @Override
  public UserDTO findByUsername(String username) {
    return users.findByUsername(username).map(UserQueryLocalAdapter::toDto).orElse(null);
  }

  private static UserDTO toDto(User u) {
    return new UserDTO(u.getId(), u.getUsername(), u.getDisplayName(), u.getCreatedAt());
  }
}
