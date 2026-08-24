package cn.huntercat.lieshoucloudpro.approval.web;

/** 无权操作（非审批人审批 / 非发起人撤销 → 403 FORBIDDEN · ADR-0032）. */
public class ApprovalForbiddenException extends RuntimeException {
  public ApprovalForbiddenException(String message) {
    super(message);
  }
}
