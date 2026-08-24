package cn.huntercat.lieshoucloudpro.approval.feign;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.cloud.openfeign.FeignClient;

import java.util.List;

/**
 * approval-service → user-service 查询（阶段 2 · 审批人用户下拉 + 自动选审批人 · ADR-0032）.
 *
 * <p>只有读端点（租户用户列表）；写操作（创建用户等）由 user 服务独占。调用时透传 {@code X-Tenant-Id}，user-service 端按 ADR-0022
 * 只返回该租户用户。
 */
@FeignClient(name = "lieshoucloud-user", path = "/api/users")
public interface UserQueryClient {

  /** 租户用户列表（含 roles code 数组；approval 服务自动选审批人时用） */
  @GetMapping
  List<UserView> listTenantUsers(
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId);
}
