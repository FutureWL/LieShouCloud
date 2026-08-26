package cn.huntercat.lieshoucloudpro.approval.port;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cn.huntercat.lieshoucloudpro.approval.feign.UserView;
import cn.huntercat.lieshoucloudpro.user.domain.Role;
import cn.huntercat.lieshoucloudpro.user.domain.User;
import cn.huntercat.lieshoucloudpro.user.service.UserService;
import feign.FeignException;
import java.util.List;
import java.util.Map;

/**
 * monolith 模式适配器（ARCHITECTURE.md §4.2 ②）：进程内直接调用 user 领域服务 {@link UserService}，零网络开销。
 *
 * <p>激活条件：{@code app.deploy-mode=monolith}。查询失败转译 {@link FeignException} 子类（404）——调用方现有 try-catch
 * 零修改。
 */
@Component("approvalUserQueryLocalAdapter")
@ConditionalOnProperty(name = "app.deploy-mode", havingValue = "monolith")
public class UserQueryLocalAdapter implements UserQueryPort {

  private final UserService users;

  public UserQueryLocalAdapter(UserService users) {
    this.users = users;
  }

  @Override
  public List<UserView> listTenantUsers(String tenantId) {
    Long tid = tenantId == null ? null : Long.valueOf(tenantId);
    return users.list(tid, true).stream().map(UserQueryLocalAdapter::toView).toList();
  }

  @Override
  public UserView getUserById(Long id, String tenantId) {
    Long tid = tenantId == null ? null : Long.valueOf(tenantId);
    return users
        .findById(id, tid)
        .map(UserQueryLocalAdapter::toView)
        .orElseThrow(
            () -> new FeignException.NotFound("USER_NOT_FOUND", null, new byte[0], Map.of()));
  }

  private static UserView toView(User u) {
    List<String> roles =
        u.getRoles() == null || u.getRoles().isEmpty()
            ? List.of("USER")
            : u.getRoles().stream().map(Role::getCode).toList();
    return new UserView(
        u.getId(),
        u.getUsername(),
        u.getDisplayName(),
        u.getEmail(),
        u.getStatus() == null ? "ACTIVE" : u.getStatus().name(),
        roles);
  }
}
