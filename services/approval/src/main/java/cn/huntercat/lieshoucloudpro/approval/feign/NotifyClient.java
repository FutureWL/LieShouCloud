package cn.huntercat.lieshoucloudpro.approval.feign;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * approval-service → notify-service 投递站内信（core.notify 横向能力 · ADR-0032 阶段 2）.
 *
 * <p>与邮件通知并行：站内信落库可查、可标已读，不依赖 SMTP。调用失败由调用方降级日志，<b>绝不阻塞审批状态机</b>。
 */
@FeignClient(name = "lieshoucloud-notify", path = "/api/notifications")
public interface NotifyClient {

  @PostMapping
  void create(
      @RequestBody CreateNotificationRequest body,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
      @RequestHeader(value = "X-User-Id", required = false) String userId);
}
