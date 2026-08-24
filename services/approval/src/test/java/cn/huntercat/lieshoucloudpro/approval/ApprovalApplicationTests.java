package cn.huntercat.lieshoucloudpro.approval;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

/** Context 冒烟：能启动 + Flyway V1 迁移成功。 */
@SpringBootTest
@ActiveProfiles("test")
class ApprovalApplicationTests extends PostgresTestSupport {

  @Test
  void contextLoads() {
    // 能走到这里即 context 启动成功（含 Flyway 迁移 + JPA validate）
  }
}
