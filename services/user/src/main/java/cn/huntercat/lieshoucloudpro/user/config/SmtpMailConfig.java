package cn.huntercat.lieshoucloudpro.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * SMTP 邮件客户端（生产验证码通道 · ADR-0023 Phase 2 · 飞书邮箱）.
 *
 * <p>读取环境变量：
 *
 * <ul>
 *   <li>{@code EMAIL_SMTP_HOST} / {@code EMAIL_SMTP_USER} / {@code EMAIL_SMTP_PASS} —— 必填，缺失即启动
 *       fail-fast（与 prod "缺失即启动失败" 原则一致）；
 *   <li>{@code EMAIL_SMTP_PORT} —— 默认 {@code 465}（SSL）；STARTTLS（587）场景需另行调整 {@code
 *       mail.smtp.ssl.enable}；
 *   <li>{@code EMAIL_FROM_NAME} / {@code EMAIL_FROM_ADDR} —— 发件人显示名 / 地址（必须与 SMTP 账号同域，否则拒信）。
 * </ul>
 *
 * <p>仅 prod profile 生效；dev/docker/test 走 {@code DevCodeSender}（日志旁路）。
 */
@Configuration
@Profile("prod")
public class SmtpMailConfig {

  @Bean
  public JavaMailSender javaMailSender(
      @Value("${EMAIL_SMTP_HOST:}") String host,
      @Value("${EMAIL_SMTP_PORT:465}") int port,
      @Value("${EMAIL_SMTP_USER:}") String username,
      @Value("${EMAIL_SMTP_PASS:}") String password) {
    if (host.isBlank() || username.isBlank() || password.isBlank()) {
      throw new IllegalStateException(
          "SMTP 未配置：需要环境变量 EMAIL_SMTP_HOST + EMAIL_SMTP_USER + EMAIL_SMTP_PASS");
    }
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(host);
    sender.setPort(port);
    sender.setUsername(username);
    sender.setPassword(password);
    Properties props = sender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.ssl.enable", "true");
    props.put("mail.smtp.connectiontimeout", 10000);
    props.put("mail.smtp.timeout", 10000);
    props.put("mail.smtp.writetimeout", 10000);
    return sender;
  }
}
