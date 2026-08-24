package cn.huntercat.lieshoucloudpro.approval.feign;

/** 站内信投递请求（core.notify · 与 notify-service 的 CreateNotificationRequest 字段对齐）. */
public record CreateNotificationRequest(
    Long recipientId, String type, String title, String content, String refType, Long refId) {}
