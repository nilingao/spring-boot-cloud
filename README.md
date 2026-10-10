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
> - 短信模块已迁入；face 人员、video 12 表、callcenter 41 表数据层已就绪，当前全工程 **49/49 模块编译通过**
> - 人脸识别、视频协议业务、呼叫中心等仍待迁移，支付暂缓；阶段进度与剩余验收以迁移台账为准
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
（`spring-boot-sms` 模块就同时承载了短信、netty-socket、job、redis 订阅四种职责）。
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
│   ├── nla-message                 短信渠道 / 模板 / 发送记录
│   ├── nla-face                    人脸人员数据层（未接入 admin）
│   ├── nla-video                   视频监控 12 表数据层（未接入 admin）
│   ├── nla-callcenter              呼叫中心 41 表数据层（未接入 admin）
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
| MySQL | 8.0+ | 库名默认 `nla-cloud`；也支持 Oracle / PostgreSQL / SQLServer |
| Redis | 见下 | 缓存、分布式锁、限流均依赖；compose 中用的是 `redis:8.6.3` |

> 工程没有 `maven-enforcer-plugin`，不会在构建时校验上述版本，版本不对只会在运行期报错。

## 快速开始

### 1. 建库

```sql
CREATE DATABASE `nla-cloud` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_cs_0900_ai_ci;
```

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
> ⚠️ 重生 SQL（`.migration/regen-sql.ps1`）会把这 22 处改回 `utf8mb4_unicode_ci`，
> 重生后必须重跑 `.migration/fix-ai-collation.ps1 -Apply`。

按顺序导入 `script/sql/`（MySQL 方言）：

| 脚本 | 内容 |
|---|---|
| `nla_system.sql` | 系统管理全部表 + 初始化数据（用户 / 角色 / 菜单 / 字典 / 租户） |
| `nla_job.sql` | SnailJob 调度相关表 |
| `nla_workflow.sql` | WarmFlow 工作流表 |
| `nla_ai.sql` | AI 会话与知识库表 |
| `nla_message.sql` | 短信模块三张表与菜单初始化 |

`nla_face.sql` / `nla_video.sql` / `nla_callcenter.sql` 为已交付数据层的新库初始化脚本，模块尚未接入 admin；按后续业务启用计划建表。脚本分别见 [face 模块说明](nla-modules/nla-face/README.md)、[video 模块说明](nla-modules/nla-video/README.md) 和 [callcenter 模块说明](nla-modules/nla-callcenter/README.md)。

基线四个脚本另有 Oracle / PostgreSQL / SQLServer 子目录版本；新增 message / face / video / callcenter 脚本目前仅交付 MySQL 方言。

### 2. 改配置

编辑 `nla-admin/src/main/resources/application-dev.yml`：

- 数据源 `url` / `username` / `password`
- Redis 连接信息
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

接口文档：`http://localhost:8080/swagger-ui/index.html`

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

## 旧代码

以下目录是 `5.0.0-dev` 微服务版本的遗留代码，**尚未迁移，计划在本分支删除**
（迁移完成前保留以便对照）：

| 目录 | 内容 |
|---|---|
| `spring-boot-business/` | 7 个业务服务：activiti / bean / face / fs / oa / sms / video |
| `spring-boot-client/` | app（小程序端）/ web-api（Web 聚合端） |
| `spring-boot-common/` | comm（公共底层）/ entity（实体） |
| `spring-boot-feign/` | 7 个 Feign 契约模块 |
| `spring-boot-service/` | nacos / seata / sentinel / xxl-job 本地服务端 |
| `spring-boot-starter/` | 21 个技术封装 starter |
| `spring-boot-system/` | gateway / pay / sso |
| `sql/` | 10 份旧建表脚本 |
| `.lib-service/` | allatori / rabbitmq-plugins / skywalking-agent |

共 46 个子模块、1847 个 Java 文件。各业务域到 `nla-modules/*` 的映射关系见
[mapping.md 第 6 节](docs/migration/mapping.md#6-业务模块映射)。

## 迁移进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| 0 | 前置环境（JDK 21 / Maven / 依赖可拉取） | ✅ |
| 1 | 工程骨架（根 POM / 全部模块 POM / 启动类 / 配置 / script） | ✅ |
| 2 | 通用基座 25 个 `nla-common-*` | ✅ |
| 3 | 自建技术封装 6 个（freeswitch / gb28181 / pay / facesdk / socketio / mq） | ⬜ |
| 4 | 数据层重写（face / sms / video / callcenter 已落地；参考数据导入/搜索评估与 MySQL 验收待推进） | 🟡 |
| 5 | 认证鉴权与租户（OAuth2 → Sa-Token；[5.1 客户端契约](docs/migration/authentication.md)、[5.2 登录策略验证与映射](docs/migration/login-strategies.md)、[5.3 表驱动短信登录](docs/migration/sms-login.md)、[5.4 小程序登录与绑定](docs/migration/xcx-login.md)、[5.5 扫码网页登录](docs/migration/qr-login.md)已交付） | 🟡 进行中 |
| 6 | 业务模块迁移（7 批次） | ⬜ |
| 7 | 工作流与调度替换（Activiti → WarmFlow） | ⬜ |
| 8 | client 聚合层扁平化（废弃 43 个 Feign） | ⬜ |
| 9 | 部署与全量验证 | 🟡 部分完成 |

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
