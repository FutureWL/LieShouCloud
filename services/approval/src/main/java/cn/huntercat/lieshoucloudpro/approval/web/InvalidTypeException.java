package cn.huntercat.lieshoucloudpro.approval.web;

/** 审批类型非法（→ 400 INVALID_TYPE）. */
public class InvalidTypeException extends RuntimeException {
  public InvalidTypeException(String value) {
    super("非法审批类型: " + value + "（仅支持 EXPENSE / PURCHASE / SALE / OTHER）");
  }
}
