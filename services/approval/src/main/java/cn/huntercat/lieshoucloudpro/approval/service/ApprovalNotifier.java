package cn.huntercat.lieshoucloudpro.approval.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.huntercat.lieshoucloudpro.approval.domain.ApprovalRequest;
import cn.huntercat.lieshoucloudpro.approval.feign.UserQueryClient;
import cn.huntercat.lieshoucloudpro.approval.feign.UserView;
import java.util.Optional;

/**
 * 审批邮件通知（ADR-0032 · 待办提醒 + 结果通知 · 异步 · 失败降级）.
 *
 * <p>通知是审批主流程的附属能力：任何失败（SMTP 未配置 / 用户无邮箱 / 查询失败 / 发送失败）一律降级日志， <b>绝不阻塞或影响审批状态机</b>。SMTP
 * 未配置（dev/docker）→ 旁路并记 info 日志。
 */
@Component
public class ApprovalNotifier {

  private static final Logger log = LoggerFactory.getLogger(ApprovalNotifier.class);

  private final Optional<JavaMailSender> mailSender;
  private final UserQueryClient users;

  @Value("${EMAIL_FROM_ADDR:lieshoucloud@huntercat.cn}")
  private String fromAddr;

  public ApprovalNotifier(Optional<JavaMailSender> mailSender, UserQueryClient users) {
    this.mailSender = mailSender;
    this.users = users;
  }

  /** 待审批提醒（发给审批人） */
  @Async
  public void notifyApprover(Long tenantId, ApprovalRequest request) {
    UserView approver = findUser(tenantId, request.getApproverId());
    if (approver == null) return;
    send(approver.email(), "【审批待办】" + request.getTitle(), buildApproverBody(request));
  }

  /** 审批结果通知（发给发起人 · result = 通过/驳回） */
  @Async
  public void notifyRequester(Long tenantId, ApprovalRequest request, String result) {
    UserView requester = findUser(tenantId, request.getRequesterId());
    if (requester == null) return;
    send(
        requester.email(),
        "【审批结果】" + request.getTitle() + " · " + result,
        buildResultBody(request, result));
  }

  private UserView findUser(Long tenantId, Long userId) {
    if (userId == null) return null;
    try {
      return users.getUserById(userId, String.valueOf(tenantId));
    } catch (Exception e) {
      log.warn("审批通知：查询用户 {} 邮箱失败（跳过通知）", userId, e);
      return null;
    }
  }

  private void send(String to, String subject, String text) {
    if (mailSender.isEmpty()) {
      if (to != null && !to.isBlank()) {
        log.info("审批通知旁路（SMTP 未配置）：to={} subject={}", to, subject);
      }
      return;
    }
    if (to == null || to.isBlank()) {
      return;
    }
    try {
      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setFrom(fromAddr);
      msg.setTo(to);
      msg.setSubject(subject);
      msg.setText(text);
      mailSender.get().send(msg);
      log.info("审批通知已发送: to={} subject={}", to, subject);
    } catch (Exception e) {
      log.warn("审批通知发送失败（不影响审批主流程）: to={} subject={}", to, subject, e);
    }
  }

  private String buildApproverBody(ApprovalRequest r) {
    return "您有一条待审批事项：\n\n"
        + "标题："
        + r.getTitle()
        + "\n"
        + "类型："
        + r.getType()
        + "\n"
        + (r.getAmount() != null ? "金额：¥" + r.getAmount() + "\n" : "")
        + (r.getDetail() != null ? "说明：" + r.getDetail() + "\n" : "")
        + "\n请登录猎手云 Pro 处理。";
  }

  private String buildResultBody(ApprovalRequest r, String result) {
    return "您发起的审批已有结果：\n\n"
        + "标题："
        + r.getTitle()
        + "\n"
        + "结果："
        + result
        + "\n"
        + (r.getComment() != null ? "审批意见：" + r.getComment() + "\n" : "")
        + "\n请登录猎手云 Pro 查看。";
  }
}
