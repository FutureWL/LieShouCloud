# @lieshoucloud/types

跨 app 共享 TS 业务类型。

当前占位（`packages/types/src/index.ts`）：

- `ApiResponse<T>` —— 通用 API 响应包装
- `HealthStatus` —— 健康状态（admin / mobile / desktop 共享）
- `UserDTO` —— 用户视图，与 user-service 字段对齐（占位）

## 演进

- **Phase 5+** —— 由 SpringDoc OpenAPI 自动生成 TS 类型，替换本文件的占位
- 跨 app（admin / mobile / mini-program / desktop）通过 `@lieshoucloud/types` import 共享

参见 `.ai/decisions/0012-monorepo-upgrade.md`。
