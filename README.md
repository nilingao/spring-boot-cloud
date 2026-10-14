<div align="center">

# 逆龙傲 NLA Cloud

**单体多模块后台管理系统**

[![license](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![jdk](https://img.shields.io/badge/JDK-21-green.svg)](#)
[![spring-boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](#)

</div>

---

> ## ⚠️ 当前分支状态
>
> 你正在看的是 **`6.0.0-nla`** 分支 —— 一次**架构升级进行中**的分支。
>
> - 工程骨架、通用基座（30 个 common 子模块）、支撑脚本**已完成并通过全量编译**
> - 阶段 6 的 7 个业务批次**全部收口**：短信、人脸（识别/特征/检索 + 双 Controller）、
>   视频（12 表 CRUD + GB28181 实时指令）、呼叫中心（41 表 + 协议 SPI + 坐席 SIP/Socket.IO）
>   均已交付并接入 `nla-admin`；真实 MySQL 8 分库 139 表已验收
> - 全工程 `mvn -B clean test` **50/50 模块 BUILD SUCCESS**
>   （reactor 自批次 6.7 新增 `nla-common-pay` 起为 50 个模块，早于本分支其它记录里的 49）
> - 支付只交付封装层 `nla-common-pay`，业务模块**有意不建**（旧 27 文件全为 demo）；
>   阶段 5 后端登录策略已收口（5.1~5.7），剩余主要是**真实启动冒烟与外部供应商/设备联调**；
>   阶段进度与剩余验收以迁移台账为准
> - 旧微服务版本请切到 **`5.0.0-dev`** 分支查看
>
> 迁移的完整映射关系、进度台账与有意偏离上游的每一处改动，见
> **[docs/migration/mapping.md](docs/migration/mapping.md)**。

## 简介

`逆龙傲`（NLA Cloud）是一个免费、开源的后台管理系统，可用于架构搭建与学习参考。

`6.0.0-nla` 是它的架构升级版本：从 Spring Cloud 微服务集群（Nacos + Gateway + Feign + Seata + Sentinel）
收敛为**单体多模块**架构，认证从 Spring Security OAuth2 + JWT 换为 Sa-Token，
工作流从 Activiti 换为 WarmFlow，定时任务从 XXL-JOB / Quartz 换为 SnailJob。

### 为什么改单体

旧版 46 个子模块拆成 11 个可独立部署的服务（`.run/` 下原有 11 份部署配置），
但实际因服务器资源不足被迫合并运行
（旧版的 `spring-boot-sms` 模块就同时承载了短信、netty-socket、job、redis 订阅四种职责，
该代码现仅存于 `5.0.0-dev` 分支）。
微服务带来的注册中心、网关、分布式事务、链路追踪成本远高于收益，因此本版收敛为单体。

## 技术栈

| 分类 | 选型 | 版本 |
|---|---|---|
| 基础框架 | Spring Boot | 4.1.1 |
| JDK | — | 21 |
| 认证授权 | Sa-Token + JWT | 1.46.0 |
| 持久层 | MyBatis-Plus / MyBatis-Plus-Join | 3.5.17 / 1.5.9 |
| 数据库 | MySQL | 8.0+ |
| 多数据源 | dynamic-datasource | 4.5.0 |
| 缓存与分布式锁 | Redisson / Lock4j | 4.7.0 / 2.2.7 |
| 对象存储 | AWS S3 SDK（兼容 MinIO / 阿里云 OSS / 腾讯云 COS） | 2.55.4 |
| 工作流 | Warm-Flow | 1.8.9 |
| 规则编排 | LiteFlow | 2.16.2.1 |
| 定时任务 | SnailJob | 2.0.2 |
| AI | Spring AI / SnailAI | 2.0.1 / 1.1.1 |
| 搜索引擎 | Easy-ES + Elasticsearch | 3.0.2 / 7.17.28 |
| MQTT | mica-mqtt | 2.6.12 |
| 短信 | SMS4J | 3.3.5 |
| Excel | Apache Fesod | 2.0.2-incubating |
| 对象映射 | MapStruct-Plus | 1.5.3 |
| 接口文档 | SpringDoc OpenAPI + therapi（JavaDoc 零注解生成） | 3.1.1 / 0.15.0 |
| 服务监控 | Spring Boot Admin | 4.1.2 |
| 工具库 | Hutool | 5.8.47 |
| 社交登录 | JustAuth | 3.0.1 |
| 序列化 | Fory | 1.7.4 |
| IP 定位 | ip2region（离线库） | 3.3.7 |
| 加密 | BouncyCastle | 1.86 |

## 模块结构

```
nla-cloud/
├── nla-admin/                  启动入口 cn.com.nla.NlaApplication
│   └── src/main/resources/     application*.yml / logback-nla.xml / banner.txt / i18n / ip2region
├── nla-api/                    跨模块契约：cn.com.nla.{module}.api + api.domain(DTO) + api.model
├── nla-common/                 通用基座（30 个子模块）
│   ├── nla-common-bom              全部 common 坐标清单，供根 POM import
│   ├── nla-common-core             核心工具 / 常量 / 异常 / 统一响应 R
│   ├── nla-common-web              Web 基座、全局异常处理器
│   ├── nla-common-mybatis          持久层、分页 PageQuery、多租户、数据权限
│   ├── nla-common-redis            Redisson 缓存与分布式锁
│   ├── nla-common-json             Jackson 增强
│   ├── nla-common-doc              SpringDoc + therapi 接口文档
│   ├── nla-common-log              操作日志切面 @Log
│   ├── nla-common-satoken          Sa-Token 认证
│   ├── nla-common-security         权限注解 @SaCheckPermission
│   ├── nla-common-encrypt          字段加密
│   ├── nla-common-sensitive        数据脱敏 @Sensitive
│   ├── nla-common-excel            Fesod 导入导出
│   ├── nla-common-oss              S3 协议对象存储
│   ├── nla-common-sms              SMS4J 短信
│   ├── nla-common-mail             邮件
│   ├── nla-common-social           JustAuth 社交登录
│   ├── nla-common-translation      字段翻译
│   ├── nla-common-job              SnailJob 客户端
│   ├── nla-common-ai               Spring AI 基座
│   ├── nla-common-mcp              MCP 协议
│   ├── nla-common-mqtt             mica-mqtt
│   ├── nla-common-freeswitch       FreeSWITCH ESL / SIP 封装
│   ├── nla-common-video            GB28181 / ZLM / ONVIF 封装
│   ├── nla-common-facesdk          人脸 SDK 封装
│   ├── nla-common-socketio         Socket.IO 封装
│   ├── nla-common-mq               RabbitMQ 封装
│   ├── nla-common-elasticsearch    Easy-ES
│   ├── nla-common-liteflow         LiteFlow 规则编排
│   └── nla-common-push             消息推送
├── nla-modules/                业务模块
│   ├── nla-system                  系统管理：用户 / 角色 / 菜单 / 部门 / 岗位 / 字典 / 租户 / 客户端
│   ├── nla-workflow                WarmFlow 工作流
│   ├── nla-gen                     代码生成
│   ├── nla-job                     定时任务业务
│   ├── nla-ai                      AI 业务
│   ├── nla-message                 短信渠道 / 模板 / 发送记录 + 表驱动发送
│   ├── nla-face                    人脸 1 表 + 检测/比对/检索业务与双 Controller（已接入 admin）
│   ├── nla-video                   视频监控 12 表 + 管理 CRUD + SIP 实时指令（已接入 admin）
│   ├── nla-callcenter              呼叫中心 41 表 + 协议 SPI + 坐席事件推送（已接入 admin）
│   └── nla-demo                    功能示例（Excel / 加解密 / 脱敏 / 幂等 / 限流等用法参考）
├── nla-extend/                 独立运行的服务端
│   ├── nla-monitor-admin           Spring Boot Admin 监控中心
│   ├── nla-snailjob-server         SnailJob 调度中心
│   └── nla-snailai-server          SnailAI 服务端
├── script/
│   ├── bin/                        nla.sh / nla.bat 启停脚本
│   ├── docker/                     docker-compose.yml / database.yml / nginx / redis
│   ├── sql/                        基线 4 方言 14 份 + message / face / video 的 MySQL 脚本
│   └── leave/                      工作流请假示例流程定义（6 份 JSON）
├── docs/migration/             迁移台账
├── .run/                       IDEA Docker 镜像构建配置（4 份，buildOnly）
└── .qoder/                     工程规范：rules/（6 份）+ skills/（nla-plus-ai-coding 与 ponytail 系列）
```

## 环境要求

| 依赖 | 版本 | 说明 |
|---|---|---|
| JDK | **21** | 必须。根 POM `java.version=21` + `maven.compiler.release=21`，用了虚拟线程等 21 特性 |
| Maven | 3.9+ | 实测于 3.9.9；`flatten-maven-plugin` 需 Maven 3.x。工程自带 `mvnw` / `mvnw.cmd` 兜底 |
| MySQL | 8.0+ | **分库部署**：`nla_system`（主库）+ `nla_message` / `nla_face` / `nla_video` / `nla_callcenter` + `nla_workflow` / `nla_job` / `nla_ai`；也支持 Oracle / PostgreSQL / SQLServer（仅基线四库） |
| Redis | 见下 | 缓存、分布式锁、限流均依赖；compose 中用的是 `redis:8.6.3` |

> 工程没有 `maven-enforcer-plugin`，不会在构建时校验上述版本，版本不对只会在运行期报错。

## 快速开始

### 1. 建库

本工程按**分库**部署，8 个库同一个实例即可。建库语句的唯一事实来源是
[`script/sql/nla_databases.sql`](script/sql/nla_databases.sql)（8 条 `CREATE DATABASE IF NOT EXISTS`，
统一 `utf8mb4` + `utf8mb4_cs_0900_ai_ci`，可重复执行）：

```bash
mysql -h <host> -P <port> -u root -p < script/sql/nla_databases.sql
```

库名清单（必须与 `nla-admin/src/main/resources/application-{dev,prod}.yml` 的数据源 URL 一致，
一致性由 `SplitDatabaseRoutingContractTest` 在 `mvn test` 里校验）：

`nla_system` `nla_message` `nla_face` `nla_video` `nla_callcenter` `nla_workflow` `nla_job` `nla_ai`

路由方式（谁访问哪个库）：

| 库 | 访问方式 |
|---|---|
| `nla_system` | 默认数据源 `master`（`spring.datasource.dynamic.primary: master`），所有未标 `@DS` 的 Mapper |
| `nla_message` / `nla_face` / `nla_video` / `nla_callcenter` | 同名数据源 + Mapper 接口上的 `@DS("库名")`（`strict: true`，键名写错直接报错）|
| `nla_workflow` | 仍走 `master` 连接，由 `nla.workflow.schema` + 动态表名拦截器把 11 张 `flow_*`/`test_leave` 限定到该库（引擎表 `flow_form` 本工程不建表）|
| `nla_job` / `nla_ai` | 由独立进程 `nla-snailjob-server` / `nla-snailai-server` 自带数据源访问，`nla-admin` 不直连 |

菜单/字典等 `sys_*` 种子全部落 `nla_system`（各业务脚本的菜单部分也一样）。

> **全库排序规则必须统一为 `utf8mb4_cs_0900_ai_ci`。**
> `nla_system.sql` / `nla_job.sql` / `nla_workflow.sql` 建表时**不写** `COLLATE`，直接继承库默认值；
> `nla_ai.sql` 上游原本每张表都**显式**写 `COLLATE=utf8mb4_unicode_ci`，本工程已把 22 处统一改为
> `utf8mb4_cs_0900_ai_ci`。两批表排序规则不一致时，跨表比较会直接报
> `ERROR 1267 Illegal mix of collations`（已在 MySQL 8.0.23 实测复现）。
>
> 名字里的 `cs` 是**捷克语语言代码**，不是 case-sensitive；大小写敏感性由结尾的 `_ci` 决定，
> 所以它和 `utf8mb4_unicode_ci` 一样**不区分大小写**（实测 `'a' = 'A'` 返回 1），
> 不影响登录、字典查询这类逻辑。详见 [mapping.md 7.7](docs/migration/mapping.md#77-排序规则统一到-utf8mb4_cs_0900_ai_ci)。
>
> 重生 SQL 的唯一事实来源是 `.migration/regen-sql.ps1`，上述口径已内建为它的**规则 9**
> （`utf8mb4_unicode_ci` → `utf8mb4_cs_0900_ai_ci`，`min=22`），**重生不会把 22 处改回旧值**。
> `.migration/fix-ai-collation.ps1` 保留作二次校验（已一致时输出 `ALREADY-CONSISTENT`）。

按顺序导入 `script/sql/`（MySQL 方言，注意**每份脚本落到哪个库**）：

| 脚本 | 目标库 | 内容 |
|---|---|---|
| `nla_databases.sql` | 实例级 | 8 个库的 `CREATE DATABASE IF NOT EXISTS`，**在导入任何表脚本之前执行** |
| `nla_system.sql` | `nla_system` | 系统管理全部表 + 初始化数据（用户 / 角色 / 菜单 / 字典 / 租户） |
| `nla_system_ext.sql` | `nla_system` | `sys_area` 只读参照表 DDL |
| `nla_system_area_data.sql` | `nla_system` | `sys_area` 参考数据 3,256 行 |
| `nla_message.sql` | 表→`nla_message`，菜单→`nla_system` | 短信三张表与菜单初始化 |
| `nla_face.sql` | 表→`nla_face`，菜单→`nla_system` | 人脸 1 表与菜单初始化 |
| `nla_video.sql` | 表→`nla_video`，菜单→`nla_system` | 视频 12 表与菜单初始化 |
| `nla_callcenter.sql` | 表→`nla_callcenter`，菜单→`nla_system` | 呼叫中心 41 表与菜单初始化 |
| `nla_workflow.sql` | `nla_workflow` | WarmFlow 工作流表 |
| `nla_job.sql` | `nla_job` | SnailJob 调度相关表 |
| `nla_ai.sql` | `nla_ai` | AI 会话与知识库表 |

四个业务模块（message / face / video / callcenter）**已接入 `nla-admin`**，运行期由 Mapper 接口上的
`@DS("库名")` 路由到各自分库。模块说明分别见
[face 模块说明](nla-modules/nla-face/README.md)、[video 模块说明](nla-modules/nla-video/README.md) 和 [callcenter 模块说明](nla-modules/nla-callcenter/README.md)。

基线四个脚本另有 Oracle / PostgreSQL / SQLServer 子目录版本；新增 message / face / video / callcenter 脚本与 `nla_databases.sql` 目前仅交付 MySQL 方言。

### 2. 改配置

编辑 `nla-admin/src/main/resources/application-dev.yml`：

- 数据源 `master` + `nla_message` / `nla_face` / `nla_video` / `nla_callcenter` 的 `url` / `username` / `password`
  （真实口令建议走环境变量 `NLA_DB_PASSWORD`，不要把密码写进仓库）
- Redis 连接信息
- 工作流分库 `nla.workflow.schema`（留空表示与主库同库）与 `nla.workflow.tables` 白名单
  （代码**不内置默认清单**：配了 `schema` 却留空 `tables` 等于不改写任何表名，工作流表会回到主库解析，启动日志会告警）
- OSS、短信、AI 等按需开启（默认注释或留空）

`application-prod.yml` 为生产环境同结构配置。默认激活 `dev`（根 POM `profiles.active`）。

### 3. 编译

```bash
mvn -B -DskipTests clean install
```

预期输出 **BUILD SUCCESS**，当前 reactor 共 49 个条目。

### 4. 启动

主服务：运行 `nla-admin` 模块的 `cn.com.nla.NlaApplication`。

可选的独立服务端（按需启动）：

| 模块 | 用途 |
|---|---|
| `nla-extend/nla-snailjob-server` | 定时任务调度中心，启用 `nla-common-job` 时必须启动 |
| `nla-extend/nla-monitor-admin` | 服务监控中心 |
| `nla-extend/nla-snailai-server` | AI 服务端，启用 `nla-ai` 时需要 |

### 5. 端口

| 服务 | 端口 | 说明 |
|---|---|---|
| `nla-admin` | **8080** | 主服务，`context-path: /` |
| `nla-snailjob-server` | 8800 | 调度中心 |
| `nla-snailai-server` | 8900 | AI 服务端（含 SSE / WebSocket） |
| `nla-monitor-admin` | 9090 | 监控中心，路径 `/admin` |

接口文档：SpringDoc **只输出 OpenAPI JSON，本工程无内置 UI**（上游 6.X 口径，未引入 knife4j / swagger-ui webjar），
请用 Postman / Apifox / swagger-editor 等外部工具导入：

- 聚合全量：`http://localhost:8080/v3/api-docs`
- 按分组：`http://localhost:8080/v3/api-docs/<分组名>`，分组名为中文，**必须 URL 编码**，
  例如系统模块 `http://localhost:8080/v3/api-docs/3.%E7%B3%BB%E7%BB%9F%E6%A8%A1%E5%9D%97`
- 分组清单见 `application.yml` 的 `springdoc.group-configs`（未匹配到的路由会返回 HTTP 200 +
  `{"code":404}` 信封，**判断端点存在性要看响应体的 `code`，不能只看状态码**）

### 6. 默认账号

主服务（`sys_user`，来自 `nla_system.sql`）：

| 账号 | 密码 | 数据权限 |
|---|---|---|
| `admin` | `admin123` | 全部数据 |
| `test` | `666666` | 本部门及以下 |
| `test1` | `666666` | 仅本人 |

SnailAI 服务端（`sai_user`，来自 `nla_ai.sql`）：`admin` / `admin123`

> 初始化密码仅供本地开发。**部署到任何可被外部访问的环境前必须修改**，
> 并同步更换 `application.yml` 中 `api-decrypt` 的 RSA 公私钥
> （当前是仓库里公开可见的示例密钥，不可用于生产）。

## Docker 部署

`script/docker/docker-compose.yml` 是完整编排，共 9 个服务：

| 服务 | 镜像 | 说明 |
|---|---|---|
| `mysql` | `mysql:8.4.9` | 数据库 |
| `redis` | `redis:8.6.3` | 缓存 |
| `minio` | `pgsty/minio` | 对象存储（S3 协议） |
| `nginx-web` | `nginx:1.31.1` | 反向代理 |
| `nla-server1` / `nla-server2` | `nla/nla-server:1.0.0` | 主服务，**双实例**，由 nginx 负载 |
| `nla-monitor-admin` | `nla/nla-monitor-admin:1.0.0` | 监控中心 |
| `nla-snailjob-server` | `nla/nla-snailjob-server:1.0.0` | 调度中心 |
| `nla-snailai-server` | `nla/nla-snailai-server:1.0.0` | AI 服务端 |

```bash
cd script/docker
docker compose up -d
```

注意主服务镜像名是 `nla-server`（而非 `nla-admin`）—— 它由 `nla-admin` 模块打包产出。
镜像 tag `1.0.0` 与工程 `revision` 保持一致，改版本号时需同步改 compose 与 `.run/`。

> 容器化部署的建库：compose 里的 `mysql` 服务只靠 `MYSQL_DATABASE` 建 primary 库 `nla_system`，
> 其余 7 个分库由挂载进去的 `script/sql/nla_databases.sql` 创建——把该文件放进宿主机的
> `/docker/mysql/init/` 即可，官方镜像会在数据目录首次初始化时自动执行（已有数据卷不会重跑）。
> 表结构与菜单种子仍需按上面的「目标库」逐份导入。

`nginx.conf` 已配好 `/snail-ai/` 的 SSE 与 WebSocket 代理
（`proxy_read_timeout 86400s`、`proxy_buffering off`、`Upgrade` / `Connection` 头透传）。

`script/docker/database.yml` 是**另一回事**：里面是 Oracle 12c / SQLServer 2017 /
PostgreSQL 14.2 / PostgreSQL 13.6 四个容器，供不用 MySQL 的部署按需启动，
对应的建表脚本在 `script/sql/` 的同名方言子目录。

`.run/` 下有 4 份 IDEA Docker 镜像构建配置（`nla-server` / `nla-monitor-admin` /
`nla-snailjob-server` / `nla-snailai-server`），均为 `buildOnly` 模式，只构建不推送、也不启动容器，
产出的镜像 tag 正好对应 compose 里的 `image:`。真正拉起服务仍靠上面的 `docker compose up -d`。

## 开发约定

以下是本工程强制执行的约定，新增代码请遵守。完整清单见
`.qoder/rules/` 下的 6 份规范文档（`backend-crud` / `backend-common-infrastructure` /
`backend-engineering` / `backend-javadoc` / `backend-module-enhancement` / `backend-query-permission`）。
`.qoder/skills/nla-plus-ai-coding/references/` 下另有后端、前端与典型场景三份细化文档，
`backend-engineering` 规则会按任务类型引导读取。

- **统一响应** `R<T>`，成功码 `200`；业务失败 `return R.fail("中文提示")`，不抛异常
- **分页**统一 `PageQuery` 入参 + `PageResult<T>` 出参（`total` + `rows`，用 `PageResult.build(list, total)` 构造）
- **构造器注入** `@RequiredArgsConstructor` + `private final`，不用字段 `@Autowired`
- **日志**用 `@Slf4j`
- **操作日志**方法加 `@Log(title=, businessType=)`
- **权限**用 `@SaCheckPermission("模块:资源:动作")` 注解式鉴权
- **对象转换**用 MapStruct-Plus 的 `@AutoMapper` + `MapstructUtils.convert()`，不写 `*Convert` 接口
- **时间类型**用 `LocalDateTime`，不用 `java.util.Date`
- **接口文档**靠 JavaDoc 自动生成（therapi），不写 `@ApiModelProperty`
- **写操作**必须 `@Transactional(rollbackFor = Exception.class)`
- **逻辑删除**：`mybatis-plus.enableLogicDelete: true` 已全局开启，实体加 `@TableLogic private String delFlag`
  （对应列 `del_flag`）。这是与 `5.0.0-dev` 的**重大差异**，旧版是物理删除 + 删除前引用校验
- **缓存刷新**用 `@TransactionalEventListener(phase = AFTER_COMMIT)`，定向刷新，禁止全量清空
- **命名**：实体无后缀、入参 `*Bo`、出参 `*Vo`、Service 接口 `I*Service`、Mapper `*Mapper`
- **索引前缀**必须 `idx_` / `un_` / `uni_idx_` / `fk_` —— 全局异常处理器靠正则解析这些前缀
  来生成唯一键冲突的中文提示
- **敏感字段**（密码 / 密钥）只写不读：列表不返回、详情返回 `null`、修改时空值保持原值、日志不打印明文
- **分库路由**：`nla_message` / `nla_face` / `nla_video` / `nla_callcenter` 下的每个 Mapper 接口必须带
  `@DS("库名")`（新增而不加会被静默路由到主库，那里根本没有这张表）；`application-dev.yml` 与
  `application-prod.yml` 必须**同步改**——数据源键名与 `nla.workflow.tables` 白名单都由
  `nla-admin` 的 `SplitDatabaseRoutingContractTest` 在 `mvn test` 里校验（见 mapping.md 7.9.1）。
  该测试需要读其他模块的字节码，**单独跑 `-pl nla-admin` 会拿到本地仓库里的旧 jar，必须带 `-am` 或走全量构建**

## 旧代码

`5.0.0-dev` 微服务版本的遗留目录**已在本分支删除**（阶段 9 物理清理，2026-10-14；
实测移除 2,315 个纳入版本管理的文件 ≈ 110 MB，逐目录体积、引用面取证与残留本机文件清单见
[mapping.md 9.2.1 / 8.11](docs/migration/mapping.md)）：

| 目录 | 内容 |
|---|---|
| `spring-boot-business/` | 7 个业务服务：activiti / bean / face / fs / oa / sms / video |
| `spring-boot-client/` | app（小程序端）/ web-api（Web 聚合端） |
| `spring-boot-common/` | comm（公共底层）/ entity（实体） |
| `spring-boot-feign/` | 7 个 Feign 契约模块 |
| `spring-boot-service/` | nacos / seata / sentinel / xxl-job 本地服务端 |
| `spring-boot-starter/` | 21 个技术封装 starter |
| `spring-boot-system/` | gateway / pay / sso |
| `sql/` | 10 份旧建表脚本；其中 `sys_area.sql` 的数据已交付为 `script/sql/nla_system_area_data.sql` |

需要对照旧实现请切到 `5.0.0-dev` 分支（共 46 个子模块、1847 个 Java 文件）。
各业务域到 `nla-modules/*` 的映射关系见
[mapping.md 第 6 节](docs/migration/mapping.md#6-业务模块映射)。

> `.lib-service/`（allatori / rabbitmq-plugins / skywalking-agent）是**本机外置大文件目录**，
> 自始未纳入版本管理（`.gitignore` 已忽略），不受上述清理影响。
> `.build/` 下的 5 份 Jenkins / Docker 脚本原本只服务这批已删除的微服务，
> 现行构建方式见「快速开始 → 编译」与 `nla-admin/Dockerfile`。

## 迁移进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| 0 | 前置环境（JDK 21 / Maven / 依赖可拉取） | ✅ |
| 1 | 工程骨架（根 POM / 全部模块 POM / 启动类 / 配置 / script） | ✅ |
| 2 | 通用基座 25 个 `nla-common-*` | ✅ |
| 3 | 自建技术封装 6 个（freeswitch / gb28181 / pay / facesdk / socketio / mq） | ✅ |
| 4 | 数据层重写（face / sms / video / callcenter 已落地并接入 admin；真实 MySQL 139 表已验收） | 🟡 |
| 5 | 认证鉴权与租户（OAuth2 → Sa-Token；[5.1 客户端契约](docs/migration/authentication.md)、[5.2 登录策略验证与映射](docs/migration/login-strategies.md)、[5.3 表驱动短信登录](docs/migration/sms-login.md)、[5.4 小程序登录与绑定](docs/migration/xcx-login.md)、[5.5 扫码网页登录](docs/migration/qr-login.md)、[5.6 手机号授权](docs/migration/phone-auth.md)、[5.7 资料授权与自动注册](docs/migration/profile-auth.md)已交付） | 🟡 后端编码已收口 |
| 6 | 业务模块迁移（7 批次全部收口） | ✅ |
| 7 | 工作流与调度替换（Activiti → WarmFlow） | ✅ 决策收口 |
| 8 | client 聚合层扁平化（废弃 43 个 Feign） | ✅ 决策收口 |
| 9 | 部署与全量验证（DB 层验收 + 分库 `@DS` 路由已落地；真启冒烟已通过；旧微服务目录与 `sql/` 已物理清理） | 🟡 剩余：前端适配、compose 外部机验收 |

详见 [mapping.md 第 8 节](docs/migration/mapping.md#8-阶段进度台账)。

## 前端

`5.0.0-dev` 配套的前端是 [vue-admin-cloud](https://github.com/NiLongAo/vue-admin-cloud)
（基于 Spring Cloud Alibaba）。

本分支的接口契约已全面变更（成功码 `0`→`200`、URL 前缀、分页结构、字段名），
前端需**另立改造任务线**同步适配，不在本仓库范围内。

## 备注

- 旧版因项目文件过大剔除了部分大文件（人脸模型、混淆工具、SkyWalking Agent）。
  这些文件随对应模块迁移时会重新评估处理方式，需要旧版大文件的请到 `5.0.0-dev` 分支查看说明。
- 旧版在线演示 <https://www.nilongao.cn/spring-cloud/>（测试账号 `nilongao` / `nilongao`）
  对应的是 `5.0.0-dev` 微服务版本，与本分支无关。

## 交流

`逆龙傲` 是完全开源免费的项目，旨在帮助开发者更方便地进行中大型管理系统开发。
使用问题欢迎在 QQ 群内提问：

- QQ 群 `715528092`

## 许可

[Apache License 2.0](LICENSE)
