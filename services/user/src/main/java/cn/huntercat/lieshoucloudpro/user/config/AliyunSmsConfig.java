package cn.huntercat.lieshoucloudpro.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;

/**
 * 阿里云短信客户端（生产验证码通道 · ADR-0023 Phase 2）.
 *
 * <p>读取环境变量：
 *
 * <ul>
 *   <li>{@code ALIYUN_SMS_ACCESS_KEY_ID} / {@code ALIYUN_SMS_ACCESS_KEY_SECRET} —— 必填，缺失即启动失败
 *       （fail-fast，与 {@code application-prod.yml} "缺失即启动失败" 原则一致，避免 prod 短信静默不可用）；
 *   <li>{@code ALIYUN_SMS_ENDPOINT} —— 可选，默认国内版 {@code dysmsapi.aliyuncs.com}。
 * </ul>
 *
 * <p>仅 prod profile 生效；dev/docker/test 走 {@code DevCodeSender}（日志旁路）。
 */
@Configuration
@Profile("prod")
public class AliyunSmsConfig {

  @Bean
  public Client aliyunSmsClient(
      @Value("${ALIYUN_SMS_ACCESS_KEY_ID:}") String accessKeyId,
      @Value("${ALIYUN_SMS_ACCESS_KEY_SECRET:}") String accessKeySecret,
      @Value("${ALIYUN_SMS_ENDPOINT:dysmsapi.aliyuncs.com}") String endpoint) {
    if (accessKeyId.isBlank() || accessKeySecret.isBlank()) {
      throw new IllegalStateException(
          "阿里云短信未配置：需要环境变量 ALIYUN_SMS_ACCESS_KEY_ID + ALIYUN_SMS_ACCESS_KEY_SECRET");
    }
    Config config =
        new Config()
            .setAccessKeyId(accessKeyId)
            .setAccessKeySecret(accessKeySecret)
            .setEndpoint(endpoint);
    try {
      return new Client(config);
    } catch (Exception e) {
      throw new IllegalStateException("初始化阿里云短信客户端失败: " + e.getMessage(), e);
    }
  }
}
