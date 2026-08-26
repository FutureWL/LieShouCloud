package cn.huntercat.lieshoucloudpro.approval.service;

/**
 * 400：审批人无法解析（业务挂接自动触发时租户无用户 / user 服务不可用）。
 *
 * <p>由 {@link ApprovalRequestService#createRequest} 抛出，web 层 {@code ApprovalExceptionHandler} 统一转为
 * {@code APPROVER_RESOLVE_FAILED}。
 */
public class ApproverResolveException extends RuntimeException {

  public ApproverResolveException(String message) {
    super(message);
  }
}
