# 贡献指南

感谢你愿意为 LieShouCloud（猎手云开源核心版）贡献力量！请花两分钟读完本指南。

## 项目定位（重要）

本仓库是 **LieShouCloud 的技术底座**（core：gateway / auth / user / admin / approval + 前端共享 packages）。

- ✅ **欢迎贡献**：bug 修复、测试、文档、性能优化、架构改进、新前端入口、通用能力（如新的 RBAC 策略、审批节点类型）
- ❌ **不在本仓库范围**：行业业务模块（ERP/MES/IoT/律所等）属于商业版 LieShouCloud Pro。如果你有行业需求想法，欢迎在 Discussion 讨论

## 开发环境

| 依赖       | 版本        |
| ---------- | ----------- |
| JDK        | 21          |
| Maven      | 3.9+        |
| Node.js    | 22+（pnpm） |
| PostgreSQL | 16          |
| Nacos      | 2.x         |

## 提交规范

- 分支：`fix/xxx`、`feat/xxx`、`docs/xxx`、`refactor/xxx`
- Commit message 遵循 Conventional Commits：`feat(user): 支持批量导入用户`
- 中文或英文 message 均可，但请保持一致性

## 代码纪律

1. **数据库可移植**：业务代码只写 JPA / JPQL，禁止方言专属 SQL（ILIKE、PG 专属函数等）；JSON 用 `@JdbcTypeCode(SqlTypes.JSON)`；主键用 `GenerationType.IDENTITY`
2. **跨服务调用走接口抽象**：新增跨服务调用时，用"接口 + 双实现"（本地 Bean / Feign），禁止业务代码直接依赖 FeignClient
3. **凭据不进仓库**：所有密钥走环境变量，commit 前本地跑一遍 `gitleaks detect`
4. 保持现有代码风格（Spotless 格式化，`mvn spotless:apply`）

## 测试要求

- 新增业务逻辑需配套测试
- 数据库相关改动需通过 Testcontainers 在 **PostgreSQL 和 MySQL 双库** 上验证（CI 会自动跑双库矩阵）

## 提交流程

1. Fork 本仓库并创建分支
2. 提交代码（通过 gitleaks + spotless + 单测）
3. 发起 Pull Request，描述改动动机与验证方式
4. 维护者 review 后会尽快合入

## 安全

发现安全漏洞请**不要**公开提交 issue，通过 SECURITY.md 中的渠道私下报告。
