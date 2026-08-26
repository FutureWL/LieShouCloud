package cn.huntercat.lieshoucloudpro.approval.port;

import cn.huntercat.lieshoucloudpro.approval.feign.UserView;
import java.util.List;

/**
 * 服务间调用抽象层（ARCHITECTURE.md §4.2 · Port 模式）。
 *
 * <p>approval → user 用户查询契约（租户用户列表 / 单用户，审批人下拉 + 通知收件人邮箱用）。业务代码只依赖本接口，不感知 单体（monolith）还是微服务（msa）形态：
 *
 * <ul>
 *   <li>{@code monolith}：进程内直接调用 user 领域服务（{@link UserQueryLocalAdapter}）
 *   <li>{@code msa}（缺省）：Feign 适配器跨进程调用（{@link UserQueryFeignAdapter}）
 * </ul>
 *
 * <p>查询失败（用户不存在）抛 {@link feign.FeignException} 子类（404）——调用方现有 try-catch 零修改。
 */
public interface UserQueryPort {

  /** 租户用户列表（含 roles code 数组；自动选审批人用） */
  List<UserView> listTenantUsers(String tenantId);

  /** 单个用户（通知收件人邮箱用；不存在 → 404） */
  UserView getUserById(Long id, String tenantId);
}
