# 基础设施一键启动（本地开发）

启动 PostgreSQL 16 + Nacos 2.x，供 core 微服务本地联调。

```bash
docker compose -f deploy/docker-compose.infra.yml up -d
```

| 组件 | 端口 | 账号 |
| --- | --- | --- |
| PostgreSQL | 5432 | postgres / postgres（本地开发默认，生产走环境变量） |
| Nacos | 8848 | nacos / nacos |

> 生产环境请通过环境变量注入凭据，切勿沿用默认密码。
