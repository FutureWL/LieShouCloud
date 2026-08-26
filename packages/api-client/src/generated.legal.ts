/**
 * legal 服务 OpenAPI 类型（宽松桥接版，**入库提交**——CI 无后端可 typecheck/build）。
 *
 * 背景：legalClient（openapi-fetch）硬依赖本文件，但真实产物需后端 /v3/api-docs/legal 生成，
 * 而 CI 前端 job 无后端 → 干净环境 typecheck 必红。本版以宽松结构解耦该依赖。
 *
 * 恢复完整类型化：起后端后执行
 *   pnpm --filter @lieshoucloud/api-client gen:api:legal
 * 生成真实 openapi-typescript 产物覆盖本文件（提交即可，向后兼容）。
 *
 * @see .ai/decisions/0016-springdoc-openapi.md
 */
export type paths = Record<string, any>;
