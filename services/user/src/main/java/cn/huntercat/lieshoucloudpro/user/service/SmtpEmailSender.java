package cn.huntercat.lieshoucloudpro.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * SMTP 邮件发送器（生产验证码通道 · ADR-0023 Phase 2 · 飞书邮箱 smtp.feishu.cn）.
 *
 * <p>安全约定（与 {@link AliyunSmsSender} 一致）：
 *
 * <ul>
 *   <li>成功 / 失败均不把验证码打进日志；目标邮箱仅记脱敏前缀；
 *   <li>发送异常 → {@link IllegalStateException}（调用方按业务失败处理，验证码不落库）。
 * </ul>
 *
 * <p>仅 prod profile 生效；dev/docker/test 走 {@link DevCodeSender}（日志旁路）。
 */
@Component
@Profile("prod")
public class SmtpEmailSender {

  private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

  private static final String SUBJECT = "【猎手云 Pro】验证码";
  private static final String BODY_TEMPLATE = "您的验证码是：%s\n\n5 分钟内有效。若非本人操作，请忽略本邮件。";

  private final JavaMailSender mailSender;
  private final String fromName;
  private final String fromAddr;

  public SmtpEmailSender(
      JavaMailSender mailSender,
      @Value("${EMAIL_FROM_NAME:猎手云服务}") String fromName,
      @Value("${EMAIL_FROM_ADDR:}") String fromAddr) {
    this.mailSender = mailSender;
    this.fromName = fromName;
    this.fromAddr = fromAddr;
  }

  /**
   * 发送邮箱验证码。
   *
   * @param target 收件邮箱地址
   * @param code 6 位数字验证码
   * @throws IllegalStateException 发件人未配置 / SMTP 发送失败
   */
  public void sendVerificationEmail(String target, String code) {
    if (fromAddr.isBlank()) {
      throw new IllegalStateException("邮件发件人未配置：需要 EMAIL_FROM_ADDR（与 SMTP 账号同域）");
    }
    MimeMessage message = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
      helper.setFrom(fromAddr, fromName);
      helper.setTo(target);
      helper.setSubject(SUBJECT);
      helper.setText(String.format(BODY_TEMPLATE, code));
      mailSender.send(message);
      log.info("SMTP 邮件发送成功 to={} from={}", maskEmail(target), fromAddr);
    } catch (MessagingException | UnsupportedEncodingException e) {
      log.error("SMTP 邮件构造失败 to={}", maskEmail(target), e);
      throw new IllegalStateException("邮件构造失败: " + e.getMessage(), e);
    } catch (MailException e) {
      log.error("SMTP 邮件发送失败 to={}", maskEmail(target), e);
      throw new IllegalStateException("邮件发送失败: " + e.getMessage(), e);
    }
  }

  /** 邮箱脱敏：li***@example.com（本地部分保留首字符）。 */
  static String maskEmail(String email) {
    if (email == null || !email.contains("@")) {
      return "***";
    }
    String local = email.substring(0, email.indexOf('@'));
    String domain = email.substring(email.indexOf('@'));
    if (local.length() <= 1) {
      return local + "***" + domain;
    }
    return local.substring(0, 1) + "***" + domain;
  }
}
