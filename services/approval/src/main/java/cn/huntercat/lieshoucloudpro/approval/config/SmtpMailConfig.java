package cn.huntercat.lieshoucloudpro.approval.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * SMTP 邮件客户端（审批通知通道 · 与 user-service 同一套 {@code EMAIL_*} 环境变量 · ADR-0032）.
 *
 * <p>与 user-service 的 {@code SmtpMailConfig} 差异：<b>条件创建</b>——只有配置了 {@code EMAIL_SMTP_HOST} 才创建
 * bean（通知是审批的附属能力，缺失时审批主流程照常工作，通知自动跳过并记日志）。
 *
 * <p>仅 prod profile 生效；dev/docker 环境不配置 SMTP → 通知旁路（日志可见）。
 */
@Configuration
@Profile("prod")
@ConditionalOnProperty(name = "EMAIL_SMTP_HOST")
public class SmtpMailConfig {

  @Bean
  public JavaMailSender javaMailSender(
      @Value("${EMAIL_SMTP_HOST}") String host,
      @Value("${EMAIL_SMTP_PORT:465}") int port,
      @Value("${EMAIL_SMTP_USER}") String username,
      @Value("${EMAIL_SMTP_PASS}") String password) {
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
