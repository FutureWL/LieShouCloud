package cn.huntercat.lieshoucloudpro.approval.web;

/** 状态机冲突（非 PENDING 的单据尝试审批/撤销 → 409 ALREADY_DECIDED · ADR-0032）. */
public class AlreadyDecidedException extends RuntimeException {
  public AlreadyDecidedException(String message) {
    super(message);
  }
}
