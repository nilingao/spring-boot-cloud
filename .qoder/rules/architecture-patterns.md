---
trigger: model_decision
description: 架构设计原则与已使用的设计模式清单；明确禁止或已否决的反模式（一表一 CRUD 机械暴露、元数据通用 CRUD、Controller 注入 Mapper、事务内刷新缓存、业务模块直接依赖、quartz 新任务、子模块写 version 等）；模块归属决策表（业务功能 / Web 聚合 / 小程序 / Feign 契约 / 共享 Entity / 工具 / starter 封装 / 建表 SQL 各放哪里）；依赖复用优先级与已有工具类清单；新旧模块隔离边界（bean、video、sms、oa、face、activiti 旧模式 vs fs 新管理端规范）；Nacos 配置管理实践；Git 实践；Docker 与 Jenkins 部署实践。加载场景：决定新功能落在哪个模块、判断设计是否属于反模式、新增 Maven 依赖、改配置、提交代码、部署、在旧模块与新管理端风格之间做选择。
---

# 架构原则、设计模式、反模式与开发实践约束

> 规范分册之一（原 `project-conventions.md` 按章拆分而来，结论均基于 `5.0.0-dev` 分支代码实证）。常驻铁律与全部分册索引见 `.qoder/rules/project-baseline.md`；查不到的机制见 `gaps-and-fs-tables.md`，禁止编造。

## 二、架构设计原则与设计模式

### 2.1 架构原则

1. **严格分层单向依赖**：`client → feign → business → starter → common`；`common` 不依赖上层；业务模块间禁止直接依赖，只能经 feign 契约
2. **契约与实现分离**：feign 模块只放 `@FeignClient` 接口无实现；entity 模块只放数据对象无逻辑
3. **技术封装下沉 starter**：第三方技术接入必须封装为 `spring-boot-starter-{tech}`，业务模块只依赖 starter，不直连原始 SDK
4. **starter 二段式拆分**：`{tech}-basic`（注解/枚举/POJO，无 Spring 依赖）+ `{tech}-core`（配置/AOP/实现）。实证四组：`logs`、`sms`、`security-oauth`、`video`
5. **配置外置**：环境配置全进 Nacos，代码库零环境密钥（compose 中密码以 `*******` 占位）
6. **Controller 零业务**：只做参数接收 + `@Validated` 校验 + 转发
7. **管理语义与运行时语义隔离**：`service/manage/*` 与运行时 Service 分离，禁止混用
8. **client 层薄聚合**：`web-api`/`app` 的 Service 仅转发 Feign，无业务逻辑

### 2.2 已使用的设计模式

| 模式 | 实现位置 |
|---|---|
| 模板方法 | `ApiController`（继承获统一异常处理）；`ServiceImpl<M,E>`；`AbstractSentinelExceptionHandler.handle(Throwable)` |
| 建造者 | `@SuperBuilder(toBuilder=true)`（Entity/Param 继承链）、`@Builder`（`RestResult`、`FsOptionVo`） |
| 适配器/转换器 | MapStruct `@Mapper` + `INSTANCE = Mappers.getMapper(X.class)` |
| 观察者/事件驱动 | `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` |
| AOP 切面 | `LoggerAspect`、`LogsAspect`、`TenantIgnoreAspect` |
| 责任链 | Security FilterChain：`JWTAuthenticationFilter`、`SmsCodeAuthenticationFilter`、`WxMiniAuthenticationFilter` + 各 `AuthenticationProvider` + `SecurityConfigurerAdapter` |
| 策略 | Ribbon `NacosRule` 权重（`MyRibbonConfig`）；短信多渠道；支付多渠道（8 个独立 pay starter） |
| 拦截器 | `TenantLineInnerInterceptor` + `PaginationInnerInterceptor`（租户插件必须 index 0）；`SecurityGlobalFilter` |
| 装饰/扩展 | `LambdaQueryWrapperX`、`QueryWrapperX`、`NotNullMap extends HashMap` |
| 单例/服务定位 | `Mappers.getMapper()`、`SpringUtil.getBean(LogApi.class)` |
| 工厂方法 | `RestResult.result(...)`、`PageResult.result(...)` |

### 2.3 反模式清单（明确禁止/已否决）

| 反模式 | 依据 |
|---|---|
| Entity 直接接收管理端 `@RequestBody` | fs 设计文档 §11 验收标准 |
| 一表一 CRUD 机械暴露 | fs §3 方案 A 否决（41 组接口，前端需理解关联表，易部分保存成功） |
| 元数据通用 CRUD | fs §3 方案 C 否决（类型安全/Swagger/校验/聚合事务/缓存刷新不足） |
| 日常保存调用 Redis `delAll()` | fs §7 |
| 管理接口执行 FreeSWITCH CLI / 系统命令 / 文件删除 | fs §8 |
| Controller 注入 Mapper 或基础 Service | fs 计划 Task6 Step7 |
| 事务内刷新缓存 | fs §7（回滚时不得刷新 Redis） |
| 密码/密钥/会议密码明文回显或写日志 | fs §8 |
| 管理语义混入运行时 Service | fs §7 |
| quartz 写新定时任务 | README L90 已弃用，改 `@XxlJob("handlerName")` |
| 修改根 POM 的 `surefire skipTests` | fs 计划 Task1 Step1 |
| 子模块 `<dependency>` 写 `<version>` | 根 POM dependencyManagement 约定 |
| 硬编码日期格式串 | 必须用 `Constant.*_FORMAT` |
| `System.out.println` | `ApiController` L60 历史遗留，勿模仿 |
| "修正"历史拼写 | 见 `naming-conventions.md` 1.2 影响面 |
| 未经用户明确要求 `git commit` | fs 计划 Task11 Step4 |

---

## 七、开发实践与架构约束

### 7.1 模块归属决策表

| 要写的东西 | 放这里 |
|---|---|
| 新业务功能（Controller/Service/Mapper） | `spring-boot-business/spring-boot-{module}` |
| Web 前端聚合接口（`/webapi/**`） | `spring-boot-client/spring-boot-web-api`，`controller/{group}` + `service/{group}`（Service 仅转发 Feign） |
| 小程序接口（`/app/**`） | `spring-boot-client/spring-boot-app` |
| 跨服务调用契约 | `spring-boot-feign/spring-boot-feign-{module}`，包 `springbootfeign{module}.api.{group}` |
| 跨模块共享 Entity/Param/Vo | `spring-boot-common/spring-boot-entity`（禁止在业务模块内定义共享实体） |
| 纯工具/响应/异常/常量/枚举 | `spring-boot-common/spring-boot-comm` |
| 第三方技术封装 | `spring-boot-starter/spring-boot-starter-{tech}`（复杂时拆 `-basic` + `-core`） |
| 本地起 Nacos/Seata/Sentinel/XXL-JOB | `spring-boot-service/*`（**禁写业务代码**） |
| 网关路由/鉴权/断言 | `spring-boot-system/spring-boot-gateway`（`config`/`predicate`/`error`/`excption`/`utils`） |
| 认证授权服务 | `spring-boot-system/spring-boot-sso` |
| 建表 SQL | 根 `sql/{module}.sql` 或 `sql/sys_{module}-基类sql.sql` |
| 设计文档/实施计划 | `docs/superpowers/{specs,plans}/YYYY-MM-DD-{主题}.md`（已有 fs 两份，规范类文档统一放 `.qoder/rules/`） |

### 7.2 依赖复用规则

1. 版本号全部在根 `pom.xml` `<properties>` 定义为 `{xxx.version}` 变量，并在 `<dependencyManagement>` 声明；
   **子模块 `<dependency>` 一律不写 `<version>`**
2. 自研模块统一 `${project.version}`（当前 `1.0.0`）
3. 新增第三方依赖三步：加 `<properties>` 版本变量 → 加 `<dependencyManagement>` 条目 → 子模块引用
4. 仓库固定阿里云 `https://maven.aliyun.com/repository/public` + `/central`，`snapshots.enabled=false`
5. 业务模块**按需**引 starter（`spring-boot-fs` 仅引 feign-bean、feign-sso、redis、rabbitmq、
   freeswitch、socket-io），禁止引全量
6. 复用优先级：`spring-boot-comm` 工具 > `spring-boot-starter-*` 封装 > Hutool > 自写。
   已有勿重造：`AppUtils`、`JwtUtils`、`TreeUtil`、`MyBatisUtils`、`RedisUtils`、`MinioUtils`、
   `IPUtil`、`NotNullMap`、`DateEditor`

### 7.3 新旧模块隔离边界

| 维度 | 旧模块（bean/video/sms/oa/face/activiti） | 新模块（fs manage 规范） |
|---|---|---|
| Service 位置 | `service/api` + `service/api/impl` | `service/manage/{group}` + `impl` |
| Service 基类 | `extends ServiceImpl<M,E>` | 纯接口 + 实现，**不继承 IService**，注入基础 Service/Mapper |
| 注入方式 | `@Autowired` 字段注入 | 构造器注入 `private final` + 显式构造方法 |
| 请求对象 | 单一 `XxxParam`（含校验分组） | `XxxPageParam`/`XxxSaveParam`/`XxxStatusParam` 分离 |
| 响应对象 | 直接返回 Entity 或 `NotNullMap` | `XxxDetailVo`/`XxxOptionVo` |
| 对象转换 | 手动 setter / `AppUtils` | MapStruct `XxxManageConvert.INSTANCE` |
| 删除 | `@GetMapping("remove")` | `@DeleteMapping("remove")` |
| 测试 | 几乎无（根 POM `skipTests=true`） | TDD，模块 POM 覆盖 `skipTests=false` |
| 缓存刷新 | 直接操作 Redis | 事务提交后事件驱动定向刷新 |

**铁律**：改旧模块遵循旧模式，不做无关重构；新建管理端遵循 fs 新规范；
禁止把管理语义写入运行时 Service；旧模块 `@Autowired` 字段注入不强制改造。

### 7.4 配置管理实践

- 环境配置**全部**进 Nacos，本地 `bootstrap.yml` 极简
  （`spring.application.name={module}-server`、`profiles.active`、`multipart`、`elasticsearch.clusterNodes`）
- `shared-configs` 复用 8 份公共配置（见 `project-overview.md` 0.3）
- 多环境 `bootstrap-dev.yml` / `bootstrap-prod.yml`；本地 `profiles.active=dev`，
  Docker 传 `--spring.profiles.active=prod`
- 敏感信息不进仓库（compose 中账号密码以 `*******` 占位）

### 7.5 Git 实践

远程 `origin https://github.com/NiLongAo/spring-boot-cloud.git`，开发分支 `5.0.0-dev`；
提交前 `git status --short`（只有计划内文件变更）+ `git diff --check`（无空白错误）；
**不主动 commit**（未经用户明确要求禁止）。

### 7.6 部署实践

新增可运行模块需同步四处：

1. 模块 `Dockerfile`（`EXPOSE` 端口、`WORKDIR /spring-boot-{module}`、`ENTRYPOINT` JVM 参数）
2. `docker-image-build.sh`（镜像名 `ccr.ccs.tencentyun.com/spring_boot_cloud/spring_cloud_{module}:latest`）
3. `.run/{module}.run.xml`（`imageTag`/`containerName`/`portBindings`/`volumeBindings`/`sourceFilePath`/`envVars`）
4. `start/{logback.xml, start.sh}`

Docker 标准 `ENTRYPOINT` JVM 参数见 `project-overview.md` 0.3。模块 POM 需指定 `<mainClass>` 与 `<includes>non-exists</includes>`。

