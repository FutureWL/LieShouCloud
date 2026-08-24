# 猎手云 Pro · 总体架构规范（一码多产）

> 本文件定义**一套代码库 → 多产品 / 多行业 / 多交付形态**的总体架构模型：能力域划分、四维正交装配、单体↔微服务演进、数据库可移植、客户交付档案与防分叉纪律。
> 与 [PRODUCT.md](../PRODUCT.md)（产品定位）+ [BUSINESS.md](../BUSINESS.md)（商业模式）+ [DATA_SECURITY.md](../DATA_SECURITY.md)（数据安全）+ [OPERATIONS.md](../OPERATIONS.md)（运维）配套使用。
> **开源版说明**：本文档描述完整的一码多产架构模型（含行业能力域与客户交付档案）。
> 本开源仓库仅包含 **core 底座**（gateway / auth / user / admin / approval）；行业能力域
> （crm / erp / mes / iot / legal / edu…）与客户交付档案属于商业版 **LieShouCloud Pro**，
> 通过稳定接口 API 在本仓库之上扩展。能力域边界即开源/闭源边界。
> 本文档是**架构决策的定稿依据**：后续所有新客户、新模块、新形态，先对照本文档套组合，再动代码。

---

## 0. 一句话定位

**一套代码库 = core 底座 + 有限能力域 + 四维正交装配。**
产品（SaaS / 物联网 / 律所 / 教培 / 零售…）是能力域的组合，交付形态（单体 / 微服务 / 私有化 / 云端）由配置驱动——**绝不 fork、绝不复制、绝不重写**。

---

## 1. 核心模型：四维正交

产品差异不是"多套东西"，而是四个**互相独立（正交）**的维度取值。任意组合都合法。

| 维度         | 取值                                            | 说明                                  |
| ------------ | ----------------------------------------------- | ------------------------------------- |
| ① 能力域组合 | core + crm/erp/oa/wms/mes/iot/legal/edu…        | 行业领域能力，可插拔                  |
| ② 部署形态   | monolith / msa                                  | 单体 or 微服务，运行时可切换          |
| ③ 数据库     | mysql / postgres                                | 纯配置差异，代码只写 JPA              |
| ④ 部署位置   | cloud（平台托管）/ on-premise（客户机房私有化） | 私有化 = 挂 license / 升级 / 运维组件 |

**正交的意义**：组合 = 一个 profile group + 一份客户档案，永远不会产生分支代码。

---

## 2. 能力域目录（代码层）

能力域平铺于 `services/`，每个域 = 可独立装配的 Maven 模块（jar）。**建域只建能力，不建行业。**

```
services/                          # 能力域平铺（每个域 = 可独立装配的 jar）
├── core/          # ★ 平台底座：gateway·auth·user·admin·approval·file·notify·audit
├── crm/           # 客户域：客户·联系人·商机·合同·会员
├── erp/           # 资源域：采购·库存·销售·财务·计费（含行业开关，如 gsp）
├── oa/            # 办公域：考勤·公告·日程·流程门户（审批引擎在 core）
├── wms/           # 仓储域：库位·拣货·盘点·出入库（erp 的深化域，依赖 erp 物料主数据）
├── mes/           # 制造执行域：工单·工序·排程·质量·追溯·报工
├── iot/           # 物联域：采集·设备·时序·告警·组态监控（含 SCADA 能力）
├── legal/         # 律所域：案件·卷宗·文书·计时计费
└── edu/           # 教培域：课程·排课·课时·教务
```

### 2.1 能力域边界约定

| 能力域 | 边界                                      | 与相邻域的关系                                         |
| ------ | ----------------------------------------- | ------------------------------------------------------ |
| core   | 全行业共用的平台能力                      | 唯一被所有域依赖的地基，API 必须稳定                   |
| crm    | 客户关系全生命周期                        | 会员 / 营销开关化扩展                                  |
| erp    | 进销存 + 财务 + 计费                      | 行业增强（如药店 GSP 批号/效期）用功能开关，不建独立域 |
| oa     | 考勤 / 公告 / 日程 / 流程门户             | 审批引擎不重复建设，复用 core.approval                 |
| wms    | 库位 / 拣货 / 盘点 / 出入库单             | 深化 erp.inventory，不重复物料主数据                   |
| mes    | 车间执行（工单 / 工序 / 报工 / 质量追溯） | 消费 iot 时序数据；报工回写 erp                        |
| iot    | 设备接入 / 采集 / 时序 / 告警 / 组态监控  | **SCADA 并入本域**，避免两套采集系统                   |
| legal  | 案件 / 卷宗 / 文书 / 计时计费             | 客户管理复用 crm 模式                                  |
| edu    | 课程 / 排课 / 课时 / 教务                 | 收费 / 退款复用 erp 计费                               |

---

## 3. 行业客户 × 能力域组合矩阵

**行业 = 组合，永远不是代码单元。** 新行业客户到来时，先查下表套组合；组合不够再加能力域（见 §8 沉淀规则）。

| 行业客户       | 能力域组合                   | 主要新开发                   | 典型形态                             |
| -------------- | ---------------------------- | ---------------------------- | ------------------------------------ |
| 通用企业 SaaS  | core + crm + erp             | 无（现有）                   | msa · pg · cloud                     |
| 物联网采集平台 | core + iot（可选 crm）       | iot 域                       | monolith/msa · mysql/pg · on-premise |
| 智能制造       | core + crm + erp + mes + iot | mes 域                       | msa · pg · on-premise                |
| MES 单点交付   | core + mes + iot             | mes 域                       | monolith · mysql · on-premise        |
| 律所           | core + crm + legal           | legal 域                     | monolith · pg · on-premise           |
| 教培           | core + crm + erp + edu       | edu 域                       | monolith · mysql · on-premise        |
| 商场           | core + crm + erp + oa + iot  | 低（组合为主）               | monolith · pg · on-premise           |
| 零售           | core + crm + erp + iot       | 低（组合为主）               | monolith/msa · pg · cloud            |
| 药店           | core + crm + erp + iot + oa  | erp 的 gsp 开关 + iot 温湿度 | monolith · mysql · on-premise        |
| 制衣厂         | core + crm + erp + mes + iot | mes 域行业适配               | msa · pg · on-premise                |

---

## 4. 交付形态：模块化单体 → 微服务演进（不回头路）

### 4.1 原则

**模块边界 = 未来服务边界。** 每个能力域的业务代码（Controller / Service / Entity）不关心自己跑在单体还是微服务里；交付形态由**装配层 + 配置文件**决定。

### 4.2 服务间调用：接口 + 双实现（关键机制）

现有 Feign 跨服务调用改为**调用抽象层**——业务代码只依赖接口（= 未来服务契约）：

```java
// ① 接口（放域模块的 api 包）
public interface UserQueryPort { UserDTO findById(Long id); }

// ② 本地实现（单体模式：进程内直接注入对方 Service）
@Service
@ConditionalOnProperty(name = "app.deploy-mode", havingValue = "monolith")
public class UserQueryLocalAdapter implements UserQueryPort { ... }

// ③ 远程实现（微服务模式：Feign 适配器）
@FeignClient("user-service")
@ConditionalOnProperty(name = "app.deploy-mode", havingValue = "msa")
public interface UserQueryFeignAdapter extends UserQueryPort { ... }
```

后期拆微服务 = 把 `app.deploy-mode` 从 monolith 切到 msa，本地适配器自动换成 Feign 适配器，**业务代码一行不改**。

### 4.3 单体装配模块

```
services/app/
└── app-iot-monolith/    # 示例：一个 main 扫全部包，产出"物联网单体版"可执行 jar
```

```java
@SpringBootApplication(scanBasePackages = "cn.huntercat.lieshoucloudpro")
public class IotMonolithApplication { ... }
```

各能力域自带的 main（如 AuthApplication）在单体里只是未被使用的类，无副作用；拆出后直接 `java -jar` 即可运行。

### 4.4 演进路径

| 阶段      | 形态       | 说明                                                                                                    |
| --------- | ---------- | ------------------------------------------------------------------------------------------------------- |
| 物联网 V1 | 单体       | 一个进程 = core + iot，本地调用，单库，无 Nacos                                                         |
| 物联网 V2 | 微服务     | profile 切 msa → Feign 适配器；collector 先拆出独立伸缩；开 Nacos + gateway；数据拆库（迁移工具一次性） |
| SaaS      | 始终微服务 | 现有 8 服务 + gateway + Nacos                                                                           |

---

## 5. 数据库可移植（PG / MySQL 共存）

### 5.1 原则

**业务代码只写 JPA，数据库差异全部收进配置文件 + 迁移脚本 + CI 测试。**

```yaml
# application-db-postgres.yml   （SaaS / 微服务版用）
spring:
  datasource:
    url: jdbc:postgresql://db/lieshoucloud
    driver-class-name: org.postgresql.Driver

# application-db-mysql.yml      （IoT / 单体版用）
spring:
  datasource:
    url: jdbc:mysql://db/iot?useSSL=false&serverTimezone=UTC
    driver-class-name: com.mysql.cj.jdbc.Driver
```

pom 中两个驱动均以 `runtime` 依赖引入，Hibernate 6 按 JDBC 驱动自动识别方言。

### 5.2 可移植性纪律

| 容易踩的坑               | 正确写法                                                                    |
| ------------------------ | --------------------------------------------------------------------------- |
| 原生 SQL（nativeQuery）  | 一律 JPQL / Criteria；必须原生 SQL 时禁用方言专属函数（ILIKE、PG 专属函数） |
| JSON 列（jsonb vs json） | `@JdbcTypeCode(SqlTypes.JSON)`，Hibernate 6 自动映射                        |
| 主键生成                 | `GenerationType.IDENTITY`，两库均支持                                       |
| 布尔                     | Boolean 自动映射（PG boolean / MySQL tinyint(1)）                           |
| 时间                     | `Instant` / `OffsetDateTime`                                                |
| 生产建表                 | 禁止 ddl-auto，走迁移工具（见 §5.3）                                        |

### 5.3 数据库迁移

| 方案      | 做法                                                                                                                                   | 适用               |
| --------- | -------------------------------------------------------------------------------------------------------------------------------------- | ------------------ |
| Flyway    | 按库分目录：`db/migration`（通用）+ `db/migration-mysql`（MySQL 专属）+ `db/migration-postgresql`（PG 专属），profile 指不同 locations | 已用 Flyway 的项目 |
| Liquibase | 变更集写中立 YAML/XML，自动按目标库生成方言 SQL，一份 changelog 管所有库                                                               | 新项目推荐         |

### 5.4 CI 双库矩阵（质量兜底）

Testcontainers 在 CI 跑矩阵 `[postgres:16, mysql:8]`，同一套集成测试双库各跑一遍——**哪条 SQL 不兼容，CI 立刻红，不允许"上线当天炸在客户现场"。**

---

## 6. 配置驱动：profile groups + 客户配置层

### 6.1 profile groups 一键组合

```yaml
# application.yml（代码库内，通用）
spring:
  profiles:
    group:
      saas: [core, crm, erp, msa, db-postgres, cloud]
      iot-monolith-mysql: [core, iot, monolith, db-mysql, on-premise]
      iot-monolith-pg: [core, iot, monolith, db-postgres, on-premise]
      iot-msa-pg: [core, iot, msa, db-postgres, on-premise]
      iot-msa-pg-cloud: [core, iot, msa, db-postgres, cloud]
      smart-mfg-msa: [core, crm, erp, mes, iot, msa, db-postgres, on-premise]
      mes-monolith: [core, mes, iot, monolith, db-mysql, on-premise]
      legal-monolith: [core, crm, legal, monolith, db-postgres, on-premise]
      edu-monolith: [core, crm, erp, edu, monolith, db-mysql, on-premise]
      mall-monolith: [core, crm, erp, oa, iot, monolith, db-postgres, on-premise]
      retail-msa-cloud: [core, crm, erp, iot, msa, db-postgres, cloud]
      pharmacy-monolith: [core, crm, erp, iot, oa, monolith, db-mysql, on-premise]
```

启动只选一个：`--spring.profiles.active=iot-monolith-mysql`，产品、形态、数据库、位置一次全定。

### 6.2 客户配置层（优先级最高，不进 git）

Spring Boot 配置优先级：**客户档案 > 产品 profile > 基础配置**。客户差异放独立文件，通过 `additional-location` 指向交付目录：

```yaml
# customer-a/config/application-customer-a.yml（交付目录，不在代码库）
app:
  brand: 某某集团工业物联网平台 # 品牌定制
  domain: iot.corp-a.com
  modules: # 功能开关
    alarm: true
    data-export: false
spring:
  datasource: # 客户数据库（密码绝不进 git）
    url: jdbc:mysql://10.0.0.5:3306/iot_a
    username: ${DB_USER}
    password: ${DB_PASSWORD} # 从交付环境注入 / Vault
```

```bash
java -jar app-iot-monolith.jar \
  --spring.profiles.active=iot-monolith-mysql \
  --spring.config.additional-location=file:/etc/lieshoucloud/customer-a/
```

**同一个 jar 服务所有客户**——jar 只认"产品形态"，客户差异全在配置与部署拓扑。

---

## 7. 客户交付档案（部署层）

代码库内只放产品模板，每个客户一份交付档案（私有仓库 / 不进 git）：

```
deploy/
├── products/                    # ★ 产品装配模板（代码库内）
│   ├── iot-monolith/            # 单体拓扑：1 jar + 1 DB
│   ├── iot-msa/                 # 微服务拓扑：gateway + 各域服务 + Nacos + DB
│   └── ...
└── customers/                   # ★ 客户交付档案（私有 / 不进 git）
    ├── customer-a/              # 引用 iot-monolith 模板
    │   ├── docker-compose.override.yml
    │   └── config/application-customer-a.yml
    └── customer-b/              # 引用 iot-msa 模板
        ├── docker-compose.override.yml
        └── config/application-customer-b.yml
```

部署模板按**拓扑**分（monolith / msa），**不按数据库分**——DB 是配置文件的事。私有化与云端共用同一拓扑，私有化额外挂载 §10 交付组件。

### 7.1 私有化（on-premise）交付组件

所有私有化交付（IoT / 律所 / 药店…）统一挂载，代码只写一份：

| 组件      | 能力                           | 说明                    |
| --------- | ------------------------------ | ----------------------- |
| license   | 离线激活、机器码绑定、授权期限 | 按年付费的行业惯例      |
| upgrade   | 离线升级包、版本校验、数据迁移 | 迁移用 Flyway/Liquibase |
| heartbeat | 可选心跳上报 / 远程支持        | 隐私开关，默认关        |

---

## 8. 防分叉纪律与能力域沉淀规则

### 8.1 防分叉三铁律（最重要）

1. **功能差异一律走开关**：`app.modules.xxx: true/false` + `@ConditionalOnProperty`，不写 `if (customer == A)`。
2. **深度差异走模块裁剪**：某功能模块客户完全不要时，装配层裁掉模块即可，代码库里模块仍存在、仍共用。
3. **客户独有逻辑走扩展点**：写在客户定制模块，通过接口 / SPI 挂载，**禁止修改 core 代码**。谁把 core fork 出去单独改，谁就亲手毁掉一码多产。

### 8.2 能力域沉淀规则（防域爆炸）

1. **先组合，后沉淀**：新客户先用现有域组合 + 功能开关满足；**只有当一个能力被 ≥ 2 个行业稳定复用且逻辑足够复杂，才上提为独立能力域**。
2. **建域只建能力，不建行业**：没有"药店域""商场域"，只有能力域。
3. **行业特有逻辑 = 配置开关 + 行业子模块**（如 erp 下的 `gsp` 子模块），通过开关裁剪。

模型即**"有限能力域（~10 个）× 无限行业组合"**。

---

## 9. 设备凭据安全（IoT 特有，平台功能级）

平台保存采集目标 / 目标服务器的 IP、账号、密码是**标准产品功能**（工业现场不可能每次连接手输密码），但必须按安全工程实现：

| 要求     | 实现                                                                          |
| -------- | ----------------------------------------------------------------------------- |
| 加密存储 | 密码 AES-GCM 加密存密文；主密钥放环境变量 / KMS / Vault，不进数据库、不进代码 |
| 展示脱敏 | 列表页只显示 IP / 账号，密码只显示"已配置"，仅允许重置，永不回显明文          |
| 权限隔离 | 仅运维角色 / 指定部门可查看修改，走现有 RBAC                                  |
| 审计日志 | 记录谁在何时修改了哪个目标的凭据                                              |
| 传输加密 | 采集器 ↔ 平台之间 TLS                                                        |
| 禁止入库 | 任何情况下凭据明文不进入 git 历史（仓库已有 gitleaks 扫描，保持启用）         |

生产更高标准（二期可选）：HashiCorp Vault 动态拉取凭据；采集器侧非对称加密握手。

---

## 10. 前端多入口（复用现有模式）

现有 `apps/`（admin / desktop / mini-program / mobile）+ `packages/{ui, api-client, config, types}` 的多入口模式直接套用：**一个后端 API 服务所有前端入口，入口差异 = 路由 / 菜单 / 主题，数据权限由 RBAC 控制。**

| 行业        | 前端入口                                                                 |
| ----------- | ------------------------------------------------------------------------ |
| SaaS        | admin（现有）                                                            |
| 物联网      | iot-console（设备管理 / 采集监控 / 告警大屏）                            |
| 律所        | law-office-admin + lawyer-workbench + client-portal + client-portal-mini |
| 教培        | 机构管理端 + 家长小程序                                                  |
| 商场        | 运营端 + 商户自助端（查账单缴费）                                        |
| 零售 / 药店 | 门店 POS + 管理端                                                        |
| 制衣厂      | 车间工位看板 + 管理端                                                    |

---

## 11. 落地优先级

| 优先级 | 事项                                                      | 说明                           |
| ------ | --------------------------------------------------------- | ------------------------------ |
| P0     | core 上提通用能力（approval / file / notify / audit）     | 三线共用地基，动一次以后都受益 |
| P0     | 定 profile groups（§6.1）                                 | 半小时立骨架                   |
| P1     | 建 deploy/products/ 拓扑模板 + deploy/customers/ 档案结构 | 交付层立起来                   |
| P1     | 客户配置层 + 功能开关机制（§6.2 / §8.1）                  | 防分叉的根基                   |
| P1     | 服务间调用改造为接口 + 双实现（§4.2）                     | 单体↔微服务演进的前提         |
| P2     | 建各行业能力域（mes / iot / legal / edu…）                | 属于新开发大头                 |
| P2     | CI 双库矩阵（Testcontainers）                             | 可移植性自动化兜底             |
| P3     | 私有化交付组件（license / upgrade / heartbeat）           | 接 on-premise 客户前完成       |

---

## 12. 决策记录

| 日期       | 决策                                                          | 理由                                                                     |
| ---------- | ------------------------------------------------------------- | ------------------------------------------------------------------------ |
| 2026-xx-xx | 采用"core + 能力域 + 四维正交装配"模型                        | 覆盖 SaaS / IoT / 律所 / 教培 / 零售 / 药店 / 制衣厂等多行业，最大化复用 |
| 2026-xx-xx | 模块化单体先行，配置驱动演进微服务                            | 中小客户单体交付，大客户微服务，业务代码不重写                           |
| 2026-xx-xx | 数据库差异纯配置化（PG/MySQL），代码只写 JPA                  | 不同客户要求不同数据库，CI 双库矩阵兜底                                  |
| 2026-xx-xx | SCADA 并入 iot 域；WMS 作为 erp 深化域；OA 复用 core 审批引擎 | 避免能力重叠导致两套系统                                                 |
| 2026-xx-xx | 行业 ≠ 能力域；建域只建能力                                   | 防止能力域随行业数量爆炸                                                 |
