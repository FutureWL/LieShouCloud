# @lieshoucloud/api-client

跨 app 共享 HTTP API 客户端. **Phase 5+ 由 SpringDoc OpenAPI 自动生成 typed client.**

## 当前

- `src/index.ts` 占位 `request<T>()` (fetch wrapper)
- 生成后 `src/generated.ts` (typed schema, 不入库)

## 生成流程

### 前置

1. 后端服务起起来：
   ```bash
   cd services && docker compose up -d
   # nacos + postgres + gateway:9000 + user:8081 + admin:8082
   ```
2. SpringDoc 自动暴露:
   - <http://localhost:9000/v3/api-docs/user>
   - <http://localhost:9000/v3/api-docs/admin>
   - Swagger UI: <http://localhost:9000/swagger-ui/user/index.html>

### 拉 spec 生成 TS

```bash
cd packages/api-client
pnpm gen:api              # user-service spec → src/generated.ts
pnpm gen:api:admin        # admin-service spec → src/generated.admin.ts
pnpm gen:api:crm          # crm-service → generated.crm.ts
pnpm gen:api:legal        # legal-service（ADR-0036/0045）→ generated.legal.ts
pnpm gen:api:file         # file-service（ADR-0046）→ generated.file.ts
pnpm gen:api:all          # 全部 8 个服务

# 非默认 gateway（如 legalmind 栈 9011 / 容器内 gateway:9000）：
API_BASE=http://localhost:9011 pnpm gen:api:all
```

## 用法 (Phase 5+ typed client)

```ts
// 1. 安装 openapi-fetch 到具体 app
//    pnpm --filter @lieshoucloud/admin add openapi-fetch

// 2. 调用
import createClient from 'openapi-fetch';
import type { paths } from '@lieshoucloud/api-client/generated';

const client = createClient<paths>({ baseUrl: 'http://localhost:9000' });

// GET /api/users/{id}
const { data, error } = await client.GET('/api/users/{id}', {
  params: { path: { id: 1 } }
});

// POST /api/users
const { data: created } = await client.POST('/api/users', {
  body: { username: 'futurewl', displayName: 'Future Wang' }
});
```

## 跨端共享

| App | 怎么用 |
|---|---|
| `apps/admin` (web) | `import { request } from '@lieshoucloud/api-client'` (占位) → Phase 5+ 改 `import createClient from 'openapi-fetch'; import type { paths } from '@lieshoucloud/api-client/generated'` |
| `apps/mobile` (RN) | 同上 |
| `apps/mini-program` (Taro) | 同上（小程序 fetch 受域名白名单限制） |
| `apps/desktop` (Tauri) | 同上 + `import { invoke } from '@tauri-apps/api/core'` (Rust IPC) |

## CI

- Phase 1: `pnpm gen:api` 不在 CI 跑（需要后端服务起来）
- Phase 2+: 可选加 CI step —— 后端起 docker compose + 跑 gen:api + commit 生成文件

## 关联文档

- `.ai/decisions/0016-springdoc-openapi.md`
- `.ai/conversations/2026-08-22-springdoc-openapi.md`
- ADR-0012 (monorepo)