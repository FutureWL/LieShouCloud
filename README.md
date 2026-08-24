# LieShouCloud · 猎手云（开源核心版）

> **一套代码库，装配出任何行业、任何形态的企业数字化平台。**
> 开源版是猎手云的技术底座；商业增强版（LieShouCloud Pro）在此基础上提供行业能力域（ERP / MES / IoT / 律所 / 教培…）。

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-blue" alt="Spring Boot 3.5"/>
  <img src="https://img.shields.io/badge/Spring%20Cloud-2023.0-purple" alt="Spring Cloud 2023.0"/>
  <img src="https://img.shields.io/badge/React-19-61dafb" alt="React 19"/>
  <img src="https://img.shields.io/badge/PostgreSQL%20%26%20MySQL-green" alt="PG & MySQL"/>
  <img src="https://img.shields.io/badge/License-Apache--2.0-brightgreen" alt="Apache-2.0"/>
</p>

---

## ✨ 为什么有这个项目

企业数字化软件有一个长期痛点：**不同行业（制造业 / 零售 / 律所 / 教培…）、不同规模（中小客户 / 集团客户）、不同交付要求（私有化 / 云端、单体 / 微服务、MySQL / PostgreSQL）需要完全不同的系统**。传统做法是为每个客户复制一套代码，最终形成几十个分叉，维护成本失控。

LieShouCloud 用一套架构解决这个问题——**能力域 + 四维正交装配**：

```
产品（行业方案）= core 底座 + 能力域组合（crm/erp/mes/iot/legal/edu…）
交付形态        = 单体 / 微服务        （配置驱动，零重写演进）
数据库          = MySQL / PostgreSQL   （代码只写 JPA，纯配置切换）
部署位置        = 云端 / 客户机房私有化  （私有化自动挂载 License / 离线升级）
```

任意组合 = 一个 profile group + 一份配置文件，**永远不产生分支代码**。

## 🧱 架构亮点

| 亮点                       | 说明                                                                                      |
| -------------------------- | ----------------------------------------------------------------------------------------- |
| 🏗 **一码多产**            | core 底座 + 可插拔能力域，同一套代码装配出不同行业方案                                    |
| 🔄 **模块化单体 → 微服务** | 服务间调用走"接口 + 双实现"（进程内 / Feign），`app.deploy-mode` 一键切换，业务代码零改动 |
| 🗄️ **多数据库可移植**      | 业务只写 JPA，Hibernate 6 自动适配方言；CI 用 Testcontainers 双库矩阵兜底                 |
| 🔐 **私有化交付体系**      | License 离线授权、离线升级包、可选心跳上报                                                |
| 🖥️ **多前端入口**          | 共享 packages（ui / api-client / types），一个后端 API 服务所有终端                       |

## 📦 包含内容（开源范围）

本仓库包含**技术底座与通用能力**：

```
services/           Spring Cloud 微服务（core 底座）
├── gateway/        统一入口：鉴权 · 限流 · 租户隔离
├── auth/           认证：JWT · 登录
├── user/           用户 / 组织 / 角色 / 权限（RBAC）
├── admin/          管理后台 API
└── approval/       审批流引擎（含 Feign 跨服务调用范例）

packages/           前端共享库：ui · api-client · config · types
deploy/             部署模板：docker-compose · Nacos 配置
docs/               架构文档（本仓库的"说明书"）
examples/           Demo 示例
```

> **不在本仓库内**（LieShouCloud Pro 商业版）：行业能力域（crm / erp / mes / iot / legal / edu…）、客户交付档案、私有化商业组件。能力域边界即开源/闭源边界，商业版通过稳定接口 API 在本仓库之上扩展。

## 🚀 快速开始

### 前置要求

- JDK 21、Maven 3.9+、Node.js 22+（pnpm）
- Docker（可选，用于数据库/微服务一键启动）

### 后端（微服务模式）

```bash
# 1. 启动基础设施（PostgreSQL + Nacos）
docker compose -f deploy/docker-compose.infra.yml up -d

# 2. 构建 core 服务
cd services && mvn clean package -DskipTests

# 3. 逐个启动（或 docker compose 一键启动）
java -jar gateway/target/*.jar
java -jar auth/target/*.jar
# ...
```

### 单体模式（模块化单体，即将支持）

```bash
# 装配成单体：一个进程跑全部 core 模块
java -jar app-monolith/target/*.jar --spring.profiles.active=monolith
```

### 前端

```bash
pnpm install
pnpm dev            # 启动 admin 开发入口
```

## 📖 文档

- [架构总纲（一码多产模型）](docs/ARCHITECTURE.md) —— 建议从这里读起
- PRODUCT.md / SECURITY.md / DATA_SECURITY.md / OPERATIONS.md —— 产品、安全、运维规范

## 🤝 贡献

欢迎贡献！请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)，提交前请确保通过 gitleaks 密钥扫描与 CI。

## 📄 许可证

Apache License 2.0。详见 [LICENSE](LICENSE)。

LieShouCloud Pro（商业版）包含行业能力域与商业组件，需商业授权，请联系 [huntercat.cn](https://huntercat.cn)。
