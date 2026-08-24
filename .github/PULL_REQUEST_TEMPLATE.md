<!-- 提 PR 前：PR ≤ 400 行、必挂关联 Issue、CI 全绿、≥1 reviewer（作者以外）· PROJECT_MANAGEMENT.md §2/§4 -->

## 动机

- 为什么做这个改动？（关联 Issue：#xxx）

## 变更清单

- 改了哪些文件 / 模块
- 关键设计决策（如有 ADR，给出链接）

## 影响面

- 性能 / 安全 / 数据 schema 是否有变化
- 是否需要 migration / 灰度

## 测试

- 跑过哪些测试（本地 + CI）
- 新增 / 修改测试用例

## Checklist

- [ ] commit message 符合 Conventional Commits（`<type>(<scope>): <desc>`）
- [ ] 分支前缀与 commit type 一致（`feat:` → `feature/` · 见 ADR-0033）
- [ ] CI 全绿
- [ ] `.ai/CONVENTIONS.md` §5 坏味道 12 条 review 通过
- [ ] （如 schema 变）数据库 migration 脚本就位
- [ ] （如 config 变）`.env.example` 已更新
- [ ] CHANGELOG.md 已更新（如对用户可见）
