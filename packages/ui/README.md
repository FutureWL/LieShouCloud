# @lieshoucloud/ui

跨 app 共享 React 组件 + antd 主题。

## 当前

- `HealthBadge`（健康徽章）—— 从 apps/admin 提取的 demo 组件
- 类型从 `@lieshoucloud/types` 复用（HealthStatus）

## 演进

- **Phase 5+** —— 通用布局、ConfigProvider、antd 包装组件逐渐抽到这里
- 跨 app（admin / mobile / mini-program / desktop）通过 `@lieshoucloud/ui` import

参见 `.ai/decisions/0012-monorepo-upgrade.md`。
