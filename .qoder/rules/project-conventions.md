---
trigger: model_decision
description: 逆龙傲 spring-boot-cloud 项目概况与完整开发规范（10 类）。两类场景加载：(a) 询问项目技术栈、框架版本、模块划分、目录结构、运行环境、部署方式、业务定位、服务端口；(b) 编写或修改 Java 代码、Controller/Service/Mapper/Entity/Param/Vo、Feign 契约、建表 SQL、日志与异常处理、参数校验与权限、单元测试、Maven 依赖或模块结构、Docker 部署、FreeSWITCH(fs) 管理端功能。
---

# 逆龙傲 spring-boot-cloud 项目概况与开发规范（唯一权威副本）

配套：`.qoder/rules/project-baseline.md`（always_on 常驻铁律）。
本文件含：项目概况（〇）+ 9 类规范（一～九）+ 注释文档规范（十）+ 缺失清单（十一）+ fs 表归属附录。
所有结论基于 `5.0.0-dev` 分支代码与配置实证，**未使用通用经验推断**；查不到的在第十一节标注缺失，禁止编造。建立日期 2026-09-28。

---

## 〇、项目概况

### 0.1 技术栈

**类型**：Spring Cloud Alibaba 微服务中后台脚手架（多租户、多业务域）。

| 维度 | 结论 | 实证 |
|---|---|---|
| 语言/JDK | Java **1.8** | 根 `pom.xml` `<java.version>`；IDE SDK `Oracle OpenJDK 1.8.0_341` |
| 框架 | Spring Boot **2.3.2.RELEASE** | 根 POM `<parent>` |
| 微服务 | Spring Cloud **Hoxton.SR9** + Alibaba **2.2.6.RELEASE** | 根 POM `<properties>` |
| 构建 | Maven 多模块（7 个一级，约 45 个子模块） | 根 POM `<modules>` |
| 注册/配置 | Nacos **2.1.0**（shared-configs 8 份） | 根 POM + `bootstrap.yml` |
| 数据库 | MySQL **5.7**，每模块独立库 | `.idea/dataSources.xml`、`docker-compose-server.yml`、`sql/*.sql` |
| ORM | MyBatis-Plus **3.4.3** + dynamic-datasource **3.2.0** + sharding-jdbc **4.1.0** | 根 POM |
| 搜索 | Elasticsearch **7.14.0** + Easy-ES **1.1.1** | 根 POM、`@EsMapperScan` |
| 缓存 | Redis（`RedisUtils`/`RedisService`） | `starter-redis` |
| 消息 | RabbitMQ（`RabbitAdmin` + `ContentTypeDelegatingMessageConverter`） | `MQConfig.java` |
| 对象存储 | MinIO **8.0.3** | 根 POM、`MinioTest.java` |
| 认证 | Spring Security + OAuth2 + JWT | `starter-security-oauth-{basic,core}` |
| 熔断限流 | Sentinel **1.8.3** | `AbstractSentinelExceptionHandler` |
| 分布式事务 | Seata **1.5.0**（AT，`@GlobalTransactional`） | `service-seata`、web-api Service |
| 链路追踪 | SkyWalking **9.1.0**（Agent + gRPC 日志上报） | `start/logback.xml`、根 POM |
| 定时任务 | XXL-JOB **2.3.1**（quartz 已弃用） | `README.md` L90 |
| 对象映射 | MapStruct **1.4.1.Final** + Lombok **1.18.20** | 根 POM `annotationProcessorPaths` |
| 工具库 | Hutool **5.5.8** | 根 POM |
| 工作流 | Activiti **7.1.0.M6** | `spring-boot-activiti` |
| 通信 | netty-socketio、FreeSWITCH ESL、GB/T 28181 | starter 目录、`spring-boot-video` |
| 人脸 | SeetaFace6 | `spring-boot-face` |
| API 文档 | Swagger | `starter-swagger` |
| 日志 | Log4j2 API + Logback 实现 | `@Log4j2`、`start/logback.xml` |
| 部署 | Docker + compose + Jenkins + 腾讯云镜像仓库 | `Dockerfile`、`docker-image-build.sh` |

### 0.2 模块划分与依赖流向

| 一级模块 | 职责 | 子模块 |
|---|---|---|
| `spring-boot-common` | 共享基础 | `comm`（工具/响应/异常/常量/枚举）、`entity`（Entity/Param/Vo/ES/MQ） |
| `spring-boot-starter` | 技术封装（21） | `autopoi`、`cloud`、`elasticsearch`、`feign`、`freeswitch`、`logs`、`minio`、`mybatis`、`nacos`、`netty`、`pay`(8 渠道)、`quartz`、`rabbitmq`、`redis`、`security-oauth`、`sentinel`、`sms`、`socket-io`、`swagger`、`video`、`xxl-job` |
| `spring-boot-feign` | 调用契约 | `feign-{activiti,bean,face,oa,sms,sso,video}` |
| `spring-boot-business` | 业务服务（7） | `activiti`、`bean`、`face`、`fs`、`oa`、`sms`、`video` |
| `spring-boot-client` | 客户端聚合（2） | `web-api`(`/webapi/**`)、`app`(`/app/**`) |
| `spring-boot-system` | 系统服务（3） | `gateway`、`sso`、`pay` |
| `spring-boot-service` | 本地起中间件（4，**禁写业务**） | `nacos`、`seata`、`sentinel`、`xxl-job` |

依赖单向：`client → feign → business → starter → common`；`common` 不依赖上层；**业务模块间禁止直接依赖**，只能经 feign 契约。

### 0.3 运行环境

| 项 | 值 | 实证 |
|---|---|---|
| JDK | `D:/develop/Jdk/jdk1.8.0_341` | IDE Context |
| Git / 分支 | `https://github.com/NiLongAo/spring-boot-cloud.git` / `5.0.0-dev` | IDE Context |
| 主数据源 | `jdbc:mysql://1.82.217.118:3401/sys_bean_dev`（root） | `.idea/dataSources.xml` |
| 中间件地址 | `1.82.217.118`；SkyWalking OAP gRPC `:12011` | `bootstrap-dev.yml`、`start/logback.xml`、compose |
| 环境切换 | `spring.profiles.active`（本地 `dev`，Docker `prod`） | `bootstrap.yml`、`Dockerfile` |
| Nacos shared-configs | `common-config`、`minio-config`、`ribbon-config`、`sentinel-config`、`mybatis-config`、`seate-config`（原文拼写）、`redis-config`、`nacos-config`，均带 `-{env}.yaml` | `bootstrap.yml` |
| 构建产物 | `spring-boot-maven-plugin layout=ZIP` + `<includes>non-exists</includes>`，依赖外置 `target/lib`；SkyWalking agent → `target/tmp` | 根 POM、`spring-boot-fs/pom.xml` |
| 全局测试开关 | `maven-surefire-plugin <skipTests>true</skipTests>` | 根 POM |
| 编码 | JVM `-Dfile.encoding=UTF-8` + resources 插件 UTF-8 | 根 POM |
| 基础镜像 | `anapsix/alpine-java:8_server-jre_unlimited` | `Dockerfile` |
| 镜像仓库 | `ccr.ccs.tencentyun.com/spring_boot_cloud/spring_cloud_{module}:latest` | `docker-image-build.sh` |
| 日志卷 | 宿主 `/work/spring-cloud/spring-boot-{module}/logs` | `.run/*.run.xml` |
| **端口/调试端口/容器名** | **以 `.run/{module}.run.xml` 为单一事实来源**（`portBindings`/`volumeBindings`/`imageTag`/`containerName`/`envVars`，共 11 个模块），本文不复述以防漂移 | `.run/` |
| Docker JVM 参数 | `-javaagent:skywalking-agent.jar` + `-Dskywalking.agent.service_name` + `-Dskywalking.collector.backend_service` + `agentlib:jdwp` + `MetaspaceSize=256M` + `Xms/Xmx=256m -Xss512k` + `UseG1GC G1HeapRegionSize=4M` + `-Dloader.path=lib` + `-Djava.security.egd=file:/dev/./urandom` + `--logging.config=start/logback.xml` | `spring-boot-bean/Dockerfile` |

### 0.4 业务定位（9 领域）

1. **统一认证与权限**（`sso`+`gateway`）：OAuth2+JWT，多登录方式（密码/短信/微信小程序），网关 URL-角色集中鉴权，多租户
2. **组织与用户主数据**（`bean`）：部门树、用户、角色、权限、字典、区域
3. **国标视频监控**（`video`+`starter-video`）：GB/T 28181 设备接入、通道、录像、云台
4. **呼叫中心/融合通信**（`fs`+`starter-freeswitch`）：平台、媒体服务器、网关、路由、企业资源、技能组、坐席、IVR、VDN、通话记录（**41 张表**，见附录）
5. **工作流**（`activiti`）：Activiti 7 流程定义与实例
6. **人脸识别**（`face`）：SeetaFace6
7. **短信与实时推送**（`sms`+`starter-sms`+`starter-socket-io`）：多渠道短信、RabbitMQ、Socket.IO
8. **支付**（`pay`+`starter-pay` 8 渠道）：⚠️ **暂未开发**（README 标注）→ 金额约定缺失，见第十一节
9. **OA**（`oa`）：办公自动化

---

## 一、代码命名规范

### 1.1 包结构

根包 `cn.com.tzy.springboot{module}`（模块名去连字符）。业务模块固定子包：

```
controller/api/{group}/          REST 入口
service/api/ + service/api/impl/ 接口 extends IService<E> / 实现 extends ServiceImpl<M,E>
service/manage/{group}/ + impl/  管理语义（fs 新规范，不继承 IService）
mapper/sql/  mapper/es/          MyBatis Mapper / Easy-ES Mapper
convert/{group}/                 MapStruct 转换器
config/  exception/  utils/
```

### 1.2 必须沿用的历史拼写（改动即破坏兼容）

| 实际 | 应为 | 影响面 |
|---|---|---|
| `springbootcomm.excption` | exception | 全项目异常引用（6 个异常类） |
| `springbootentity.dome` | domain | ⚠️ `ApiController` 的 `ClassScanner.scanPackage` **硬编码此包名**，改名导致唯一键提示失效 |
| `cn.com.tzy.spingbootstartermybatis` | springboot... | starter-mybatis 全部类（租户/分页/字段填充） |
| `cn.com.tzy.srpingbootstartersecurityoauthbasic`/`core` | springboot... | starter-security-oauth 全部类（网关鉴权、多登录） |
| `springbootstarterstreamrabbitmq` | — | starter-rabbitmq 包名含 `stream`（`MQConfig`/`MqClient`） |
| `springbootbean.hedel`（`MqHedel`/`MqHedelTwo`） | handler | bean 模块 MQ 消费处理 |
| `seate-config-{env}.yaml` | seata | Nacos shared-configs dataId，改名致 Seata 配置加载失败 |
| `ActivitiServcieImplTest` | Service | activiti 测试类名 |

### 1.3 类命名后缀

| 类型 | 规则 | 位置 | 示例 |
|---|---|---|---|
| Entity | 无后缀 + `@TableName` | `springbootentity.dome.{module}` | `Department`、`Platform` |
| 入参 | `*Param` | `springbootentity.param.{module}` | `DepartmentParam`、`PlatformSaveParam`、`PlatformPageParam`、`FsLongStatusParam` |
| 出参 | `*Vo` | `springbootentity.vo.{module}` | `PlatformDetailVo`、`FsOptionVo` |
| Mapper | `*Mapper` + `@Mapper` + `extends BaseMapper<E>` | `{module}.mapper.sql` | `DepartmentMapper` |
| Service | `*Service extends IService<E>` | `{module}.service.api` | `DepartmentService` |
| ServiceImpl | `*ServiceImpl extends ServiceImpl<M,E>` | `service.api.impl` | `DepartmentServiceImpl` |
| 管理 Service | `*ManageService`（不继承 IService） | `service.manage.{group}` | `PlatformManageService` |
| 转换器 | `*Convert` | `{module}.convert.{group}` | `UserConvert`、`PlatformManageConvert` |
| Controller | `*Controller extends ApiController` | `controller.api.{group}` | `DepartmentController` |
| Feign | `*ServiceFeign` | `springbootfeign{module}.api.{group}` | `DepartmentServiceFeign` |
| 启动类 | `SpringBoot{Module}Application` | 模块根包 | `SpringBootBeanApplication` |

### 1.4 Controller Bean 名（跨模块同类名会冲突，必须显式命名）

```java
@RestController("ApiBeanDepartmentController")        // 业务端
@RestController("WebApiBeanDepartmentController")     // web-api 聚合端
@RestController("ApiFsPlatformManageController")      // fs 管理端
@RestController("ApiConfigAreaController")            // 配置组
@RestController("ApiStaticUpLoadController")          // 静态文件
```

格式：`{层级}{模块}{资源}Controller`。

### 1.5 URL 与 HTTP 方法

- 业务服务端 `/api/{module}/{resource}/{action}`；Web 聚合端 `/webapi/...`；小程序端 `/app/...`
- action 词表：`page`、`detail`、`save`、`remove`、`select`、`tree`、`all`、`status`
- 多词 action 与资源名用**下划线**：`department_privilege_list`、`save_gateways`、`display_page`、
  `/api/fs/media_server`、`/api/fs/route_group`、`/app/config/dictionary_item`
- 方法映射：`page`/`save`/`status`/`save_*`/`default`/`selected` → POST；
  `detail`/`select`/`all`/`tree` → GET；`remove` → 新模块 DELETE，旧模块 GET（改旧模块保持一致）

### 1.6 数据库命名

- 表名 `{module}_` 全小写下划线：`bean_*`、`fs_*`、`sys_*`、`oa_*`、`face_*`、`video_*`、`sms_*`
- 第三方保留官方命名：`qrtz_*`（大写）、`act_*`
- 关联表 `{主}_connect_{从}`（`bean_user_connect_role`）或 `{主}_{从}`（`fs_agent_group`、`fs_skill_agent`）
- 列名下划线；MySQL 关键字加反引号 `` `name` ``/`` `enable` ``/`` `status` ``
- 索引前缀 `idx_`/`uni_idx_`/`un_`/`fk_` —— `ApiController` 正则强依赖，改名破坏唯一键提示

### 1.7 Java 与枚举命名

类/接口 PascalCase；方法/字段 camelCase；常量与枚举常量 UPPER_SNAKE。

通用枚举集中为 `ConstEnum`（`springbootcomm.common.enumcom`）的**静态内部枚举**，固定结构：

```java
public enum Flag {
    /**
     * 判断枚举
     */
    NO(0, "否"),
    YES(1, "是"),
    ;
    private final int value;
    private final String name;
    Flag(int value, String name) { this.value = value; this.name = name; }
    public int getValue() { return value; }
    public String getName() { return name; }
    public static String getName(Integer value) { return MAP.get(value); }
    private static final Map<Integer, String> MAP = new HashMap<>();
    static { for (Flag s : Flag.values()) MAP.put(s.getValue(), s.getName()); }
}
```

现有内部枚举：`Flag`、`Sex`、`ContentType`、`StaticPath`、`PasswordEncoderTypeEnum`、`ConfigEnum`、
`ReviewStateEnum`、`UserTypeEnum`、`TriggerType`、`LoginTypeEnum`。响应码独立为顶层枚举 `RespCode`。

---

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
| "修正"历史拼写 | 见 1.2 影响面 |
| 未经用户明确要求 `git commit` | fs 计划 Task11 Step4 |

---

## 三、核心编码规范

### 3.1 统一响应封装

`cn.com.tzy.springbootcomm.common.vo`：

```java
// RestResult<T>{ int code; String message; T data; String tid = TraceContext.traceId(); }
RestResult.result(RespCode.CODE_0.getValue(), null, data)     // 成功带数据
RestResult.result(RespCode.CODE_0.getValue(), "保存成功")       // 成功带消息
RestResult.result(RespCode.CODE_2.getValue(), "平台不存在")     // 业务失败
RestResult.result(RespCode.CODE_0)                             // 仅状态码
RestResult.result(respCode) / result(code, msg) / result(code, msg, data)
result.ok()                                                    // code == 0

// PageResult{ int code; String message; String tid; Page data{ Object items; int total; } }
PageResult.result(RespCode.CODE_0.getValue(), null, records, total)
MyBatisUtils.selectPage(IPage)          // IPage → PageResult
PageResult.SUCCESS                      // 空成功常量（测试用）
```

⚠️ **项目没有 `R.ok()/R.fail()`**。`tid` 自动注入 SkyWalking `TraceContext.traceId()`。

### 3.2 响应码 `RespCode`（不可随意扩展，需与网关/前端对齐）

```
CODE_0(0,成功)  CODE_1(1,服务器内部错误)  CODE_2(2,参数错误)
CODE_101 接口限流    CODE_102 服务降级    CODE_103 热点参数限流
CODE_104 系统规则不满足  CODE_105 授权规则不通过
CODE_310 密码未设置  CODE_311 用户名或密码错误  CODE_312 密码次数超限  CODE_313 客户端认证失败
CODE_314 token无效或已过期  CODE_315 token已被禁止访问  CODE_316 访问权限异常
CODE_317 访问未授权  CODE_318 访问重复请求  CODE_319 账号锁定
```

**成功 = `CODE_0`，一切业务拒绝 = `CODE_2`。**

### 3.3 主键策略

- 默认 `LongIdEntity` → `@TableId(value="id", type=IdType.AUTO)`，数据库自增 BIGINT
- 备选 `IntIdEntity`、`StringIdEntity`
- 特例：`fs_media_server` 用 `RandomUtil.randomNumbers(19)` 生成字符串 ID

### 3.4 审计字段与自动填充

```java
// Base 抽象类；继承链 Entity → LongIdEntity → Base
@TableField(value="create_user_id", fill=FieldFill.INSERT)        Long createUserId;
@TableField(value="create_time",    fill=FieldFill.INSERT)        Date createTime;
@TableField(value="update_user_id", fill=FieldFill.INSERT_UPDATE) Long updateUserId;
@TableField(value="update_time",    fill=FieldFill.INSERT_UPDATE) Date updateTime;
```

由 `DefaultDBFieldHandler implements MetaObjectHandler` 自动填充，**仅在字段为 null 时填充**，
用户 ID 取 `JwtUtils.getUserId()`。业务代码勿手动赋值。

### 3.5 时间字段

```java
@DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)   // 入参绑定
@JsonFormat(pattern = Constant.DATE_TIME_FORMAT)       // 出参序列化
private Date createTime;
```

类型统一 `java.util.Date`（非 `LocalDateTime`）。**必须复用 `Constant` 常量**：
`DATE_TIME_FORMAT="yyyy-MM-dd HH:mm:ss"`、`DATETIME_FORMAT="yyyyMMddHHmmss"`、
`DATE_FORMAT="yyyy-MM-dd"`、`HOUR_MINUTE="HH:mm"`、`MONTH_FORMAT="yyyy-MM"`、`YEAR_FORMAT="yyyy"`。

### 3.6 状态/布尔字段

`Integer` 取 0/1，语义查 `ConstEnum.Flag`（`NO(0,"否")`/`YES(1,"是")`）；命名 `isEnable`/`isEnabled`/`enable`/`status`；
合法性校验 `if (StringUtils.isEmpty(ConstEnum.Flag.getName(v))) return ...CODE_2...`；
`status` 常表运行时状态，save 时须 `entity.setStatus(null)` 禁止覆盖。

### 3.7 金额字段

❌ **缺失** —— `spring-boot-pay` 暂未开发，全项目无 `BigDecimal` 金额精度/单位约定。新增支付功能前须先与用户确认。

### 3.8 Lombok 固定组合

```java
// Entity（有继承）
@Data @EqualsAndHashCode(callSuper = true) @SuperBuilder(toBuilder = true)
@AllArgsConstructor @NoArgsConstructor
@TableName(value = "xxx") @ApiModel("中文")

// Param（继承 PageModel）
@ApiModel("中文") @SuperBuilder(toBuilder = true) @Data @NoArgsConstructor @AllArgsConstructor

// 简单 SaveParam（无继承，fs 新规范）
@Data @Builder @NoArgsConstructor @AllArgsConstructor @ApiModel("中文")

// 日志：@Log4j2（lombok.extern.log4j.Log4j2），不用 @Slf4j
// 受检异常：@SneakyThrows
// 注入：@RequiredArgsConstructor（工具/配置类）
```

### 3.9 轻量返回 `NotNullMap`

`springbootcomm.constant.NotNullMap extends HashMap`：
`putString/putByte/putShort/putInteger/putLong/putFloat/putDouble` 自动 null→默认值；
`putDate`（yyyy-MM-dd）、`putDateTime`（yyyy-MM-dd HH:mm:ss）、`putMobileMask`（脱敏）。
用于下拉/树等无需建 Vo 的场景。

### 3.10 分页

```java
Page<T> page = MyBatisUtils.buildPage(param);   // PageModel → MP Page，自动带 sort
mapper.selectPage(page, wrapper);
return MyBatisUtils.selectPage(page);           // → PageResult
```

`PageModel` 字段：`query`（模糊搜索）、`pageNumber`(默认 1)、`pageSize`(默认 10)、
`startRow`（计算属性）、`sort`（`PageSortModel{field,order}`，常量 `PageSortModel.ASC`）。

### 3.11 查询构造器

新代码用 `LambdaQueryWrapper`（类型安全），旧代码存在 `QueryWrapper`（字符串列名）。

```java
new LambdaQueryWrapper<Platform>()
    .and(StringUtils.isNotBlank(param.getQuery()), w -> w
            .like(Platform::getName, param.getQuery())
            .or().like(Platform::getLocalIp, param.getQuery()))
    .eq(param.getEnable() != null, Platform::getEnable, param.getEnable())
    .last("LIMIT " + limit);
```

扩展类 `LambdaQueryWrapperX` / `QueryWrapperX` 可用。

### 3.12 依赖注入

- 旧模块：`@Autowired` 字段注入（可省 private）或 `@Resource`
- 新模块（fs manage）：构造器注入 `private final` + 显式构造方法（不加 `@Autowired`）
- 工具/配置类：`@RequiredArgsConstructor`

### 3.13 树结构

```java
List<TreeNode<Map>> node = TreeUtil.getTree(list, "parentId", "id", null);
// 或方法引用版
List<TreeNode<Department>> node = TreeUtil.getTree(list, Department::getParentId,
        Department::getId, Arrays.asList(null, ""));
List<Map> maps = AppUtils.transformationTree("children", node);
// 需虚拟顶级节点时
Map root = new HashMap(); root.put("parentId",""); root.put("departmentName", topName);
AppUtils.transformationTree(root, "children", node);
```

---

## 四、分层架构与标准开发模式

### 4.1 业务模块目录

```
spring-boot-business/spring-boot-{module}/
├─ src/main/java/cn/com/tzy/springboot{module}/
│  ├─ SpringBoot{Module}Application.java
│  ├─ controller/api/{group}/
│  ├─ service/api/{Xxx}Service.java + service/api/impl/
│  ├─ service/manage/{group}/ + impl/          （新规范）
│  ├─ mapper/sql/{Xxx}Mapper.java、mapper/es/
│  ├─ convert/{group}/{Xxx}Convert.java
│  ├─ config/、exception/、utils/
├─ src/main/resources/
│  ├─ bootstrap.yml、bootstrap-dev.yml、bootstrap-prod.yml
│  ├─ logback-test.xml
│  └─ mapper/{Xxx}Mapper.xml
├─ src/test/java/cn/com/tzy/springboot{module}/
├─ start/{logback.xml, logback-mq.xml, start.sh}
└─ Dockerfile、docker-image-build.sh、pom.xml
```

注：`video` 模块 Service 直接在 `service/` + `service/impl/`（无 `api` 中间层），属历史差异。

### 4.2 启动类标准注解

```java
@EnableDiscoveryClient                                              // Nacos 注册发现
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) // 数据源由 sharding/dynamic 接管
@RibbonClients(defaultConfiguration = MyRibbonConfig.class)          // Nacos 权重负载
@EsMapperScan("cn.com.tzy.springboot{module}.mapper.es")             // 用 ES 时
@EnableCaching                                                       // Spring Cache
public class SpringBoot{Module}Application {
    public static void main(String[] args) { SpringApplication.run(...); }
}
```

### 4.3 共享模块结构

`spring-boot-entity`：`dome/{bean,fs,video,sms,oa,face,sys}`、`param/{bean,fs,video,sms,oa,sys,activiti}`、
`vo/{bean,fs,video}`、`es/`、`export/`、`mq/`、`common/`、`utils/`（`TreeUtil`）

`spring-boot-comm`：`common/bean`（`Base`、`LongIdEntity`、`IntIdEntity`、`StringIdEntity`、`TreeNode`）、
`common/model`（`BaseModel`、`PageModel`、`PageSortModel`、`DictModel`）、
`common/vo`（`RestResult`、`PageResult`、`RespCode`）、`common/enumcom`（`ConstEnum`）、
`common/jwt`（`JwtCommon`）、`common/mq`、`constant`（`Constant`、`NotNullMap`）、
`excption`（6 个异常类）、`interfaces`、`spring`（`DateEditor`）、`utils`（`AppUtils`、`JwtUtils`）

### 4.4 Service 实现标准骨架

```java
@Service
public class XxxServiceImpl extends ServiceImpl<XxxMapper, Xxx> implements XxxService {

    @SneakyThrows
    @Override
    public PageResult page(XxxParam param) {
        Page<Xxx> page = MyBatisUtils.buildPage(param);
        LambdaQueryWrapper<Xxx> w = new LambdaQueryWrapper<Xxx>()
                .and(StringUtils.isNotBlank(param.getQuery()), q -> q.like(Xxx::getName, param.getQuery()))
                .eq(param.getStatus() != null, Xxx::getStatus, param.getStatus());
        return MyBatisUtils.selectPage(baseMapper.selectPage(page, w));
    }

    @Override
    public RestResult<XxxDetailVo> detail(Long id) {
        Xxx e = getById(id);
        if (e == null) return RestResult.result(RespCode.CODE_2.getValue(), "xxx不存在");
        return RestResult.result(RespCode.CODE_0.getValue(), null, XxxConvert.INSTANCE.convert(e));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RestResult<Long> save(XxxSaveParam param) {
        boolean isUpdate = param.getId() != null;
        if (isUpdate && getById(param.getId()) == null)
            return RestResult.result(RespCode.CODE_2.getValue(), "xxx不存在");
        long cnt = count(new LambdaQueryWrapper<Xxx>().eq(Xxx::getName, param.getName())
                        .ne(isUpdate, Xxx::getId, param.getId()));     // 唯一性预校验，编辑排除自身
        if (cnt > 0) return RestResult.result(RespCode.CODE_2.getValue(), "名称已存在");
        Xxx entity = XxxConvert.INSTANCE.convert(param);
        if (!(isUpdate ? updateById(entity) : save(entity)))
            return RestResult.result(RespCode.CODE_2.getValue(), "保存失败");
        return RestResult.result(RespCode.CODE_0.getValue(), null, entity.getId());
    }

    @Override
    public RestResult<?> status(FsLongStatusParam param) {
        if (getById(param.getId()) == null)
            return RestResult.result(RespCode.CODE_2.getValue(), "xxx不存在");
        Xxx update = new Xxx();                 // 只更新目标字段
        update.setId(param.getId());
        update.setEnable(param.getStatus());
        if (!updateById(update)) return RestResult.result(RespCode.CODE_2.getValue(), "状态更新失败");
        return RestResult.result(RespCode.CODE_0.getValue(), "更新成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RestResult<?> remove(Long id) {
        Xxx e = getById(id);
        if (e == null) return RestResult.result(RespCode.CODE_2.getValue(), "xxx不存在");
        if (refService.count(new LambdaQueryWrapper<Ref>().eq(Ref::getXxxId, id)) > 0)
            return RestResult.result(RespCode.CODE_2.getValue(), "请先删除子级xxx");
        removeById(id);
        return RestResult.result(RespCode.CODE_0.getValue(), "删除成功");
    }

    @Override
    public RestResult<List<XxxOptionVo>> select(String keyword, Integer limit) {
        int n = limit != null ? limit : 20;
        List<Xxx> list = list(new LambdaQueryWrapper<Xxx>()
                .like(StringUtils.isNotBlank(keyword), Xxx::getName, keyword)
                .last("LIMIT " + n));
        return RestResult.result(RespCode.CODE_0.getValue(), null, XxxConvert.INSTANCE.convertOptions(list));
    }
}
```

管理 Service（fs 新规范）**不继承 `ServiceImpl`**，改为注入基础 Service + Mapper：

```java
@Service
public class PlatformManageServiceImpl implements PlatformManageService {
    private final PlatformService platformService;
    private final PlatformMapper platformMapper;
    public PlatformManageServiceImpl(PlatformService s, PlatformMapper m) {
        this.platformService = s; this.platformMapper = m;
    }
}
```

### 4.5 Controller 标准骨架

```java
@RestController("Api{Module}{Resource}Controller")
@RequestMapping("/api/{module}/{resource}")
public class XxxController extends ApiController {          // 必须继承

    private final XxxManageService xxxManageService;          // 新规范：构造器注入
    public XxxController(XxxManageService s) { this.xxxManageService = s; }

    @PostMapping("page")
    public PageResult page(@Validated @RequestBody XxxPageParam param) { return xxxManageService.page(param); }

    @GetMapping("detail")
    public RestResult<XxxDetailVo> detail(@RequestParam Long id) { return xxxManageService.detail(id); }

    @PostMapping("save")
    public RestResult<Long> save(@Validated @RequestBody XxxSaveParam param) { return xxxManageService.save(param); }

    @PostMapping("status")
    public RestResult<?> status(@Validated @RequestBody FsLongStatusParam param) { return xxxManageService.status(param); }

    @DeleteMapping("remove")
    public RestResult<?> remove(@RequestParam Long id) { return xxxManageService.remove(id); }

    @GetMapping("select")
    public RestResult<List<XxxOptionVo>> select(@RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "20") Integer limit) {
        return xxxManageService.select(keyword, limit);
    }
}
```

旧模块额外带冗余 `@ResponseBody`；client 层必写 Swagger 注解：

```java
@Api(tags = "部门信息相关接口", position = 1)
@ApiOperation(value = "部门信息分页查询", notes = "部门信息分页查询")
@ApiImplicitParams({
    @ApiImplicitParam(name = "id", value = "部门信息编号", required = true,
                      paramType = "query", dataType = "Long", example = "0")
})
```

### 4.6 Mapper XML 规范

```xml
<mapper namespace="cn.com.tzy.springboot{module}.mapper.sql.XxxMapper">
  <resultMap id="BaseResultMap" type="cn.com.tzy.springbootentity.dome.{module}.Xxx">
    <!--@mbg.generated-->
    <!--@Table bean_department-->
    <id column="id" property="id"/>
    <result column="parent_id" property="parentId"/>
  </resultMap>

  <sql id="Base_Column_List">
    <!--@mbg.generated-->
    id, parent_id, department_name
  </sql>

  <select id="findAvailableTree" resultType="map">
    select parent_id as 'parentId', id as 'id', department_name as 'departmentName'
    from bean_department
    <trim prefix="where" prefixOverrides="and">
      and is_enable = 1
      <if test="departmentName != null and departmentName != ''">
        and department_name like concat('%',#{departmentName},'%')
      </if>
    </trim>
  </select>

  <select id="selectNameLimit" resultType="cn.com.tzy.springbootentity.dome.bean.Department">
    select bu.* from bean_department bu
    <trim prefix="where" prefixOverrides="and">
      <if test="departmentIdList != null and departmentIdList.size() != 0">
        <foreach close=")" collection="departmentIdList" item="item" open="and bu.id not in(" separator=",">
          #{item}
        </foreach>
      </if>
    </trim>
    <if test="limit != null"> limit ${limit} </if>
  </select>
</mapper>
```

约定：`<trim prefix="where" prefixOverrides="and">` 包动态条件；模糊查询 `like concat('%',#{x},'%')`；
集合用 `<foreach>`；`limit ${limit}`（`$` 拼接）；简单查询 `resultType="map"` + 列别名驼峰。
根 POM `<resources>` 已配扫描 `src/main/java` 下 `**/*.xml`，但实际统一放 `resources/mapper`。

### 4.7 对象转换（MapStruct）

```java
@Mapper
public interface PlatformManageConvert {
    PlatformManageConvert INSTANCE = Mappers.getMapper(PlatformManageConvert.class);

    Platform convert(PlatformSaveParam param);
    PlatformDetailVo convert(Platform entity);
    List<FsOptionVo> convertOptions(List<Platform> entities);   // 需 default 方法处理 Long→String
}
```

方法名统一 `convert`（重载区分方向）+ `convertOptions`（列表转下拉）。
**禁止**手写 getter/setter 拷贝，**禁止**用 `BeanUtils.copyProperties` 做主转换。
根 POM `annotationProcessorPaths` 已配 lombok → mapstruct-processor，**顺序不可调**。

### 4.8 Feign 契约

```java
@FeignClient(value = "bean-server", contextId = "bean-server",
             path = "/api/bean/department", configuration = FeignConfiguration.class)
public interface DepartmentServiceFeign {

    @RequestMapping(value = "page", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    PageResult page(@Validated @RequestBody DepartmentParam param);

    @RequestMapping(value = "/detail", consumes = "application/json", method = RequestMethod.GET)
    RestResult<?> detail(@RequestParam("id") Long id);
}
```

- `value` = 目标应用名 `{module}-server`；`path` = 目标 Controller `@RequestMapping` 根路径
- 必须指定 `configuration = FeignConfiguration.class`
- 用 `@RequestMapping` 而非 `@GetMapping`（历史约定）
- 返回类型复用 `RestResult`/`PageResult`，入参复用 entity 模块 Param
- client 层 Service 仅做转发，可加 `@GlobalTransactional`（Seata）

---

## 五、安全权限与数据校验规范

### 5.1 认证框架

Spring Security + OAuth2 + JWT。

- `spring-boot-sso` 授权服务：`AuthorizationConfig extends AuthorizationServerConfigurerAdapter`、
  `SecurityConfig extends WebSecurityConfigurerAdapter`、`MyDaoAuthenticationProvider`、`WxConfig`
- `spring-boot-gateway` 统一鉴权：`ResourceServerConfig`、
  `ResourceServerManager implements ReactiveAuthorizationManager<AuthorizationContext>`、
  `SecurityGlobalFilter implements GlobalFilter, Ordered`
- 多登录方式扩展四件套：`{Xxx}AuthenticationFilter extends AbstractAuthenticationProcessingFilter` +
  `{Xxx}AuthenticationProvider implements AuthenticationProvider` +
  `{Xxx}AuthenticationSecurityConfig extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity>` +
  `{Xxx}AuthenticationToken extends AbstractAuthenticationToken`（实证：`SmsCode*`、`WxMini*`）
- Token：`TokenStore.readAuthentication(key)`；Redis 前缀 `Constant.AUTH_TOKEN_ACCESS_PREFIX="auth:token:access:"`；
  验证码前缀 `Constant.VERIFY_CODE_PREFIX="redis:verifyCode:"`
- JWT 工具：`JwtUtils.builder(JwtCommon.JWT_AUTHORIZATION_KEY, isSocketIo, request)`
  `.setPrefix(JwtCommon.AUTHORIZATION_PREFIX).builderJwtUser(null)`；`JwtUtils.getUserId()`

### 5.2 权限模型（URL-角色，网关集中鉴权）

```
Redis Hash: key = Constant.ALL_URL_KEY ("shiro:all:url:")
value: { "/api/v1/users/*" : ["ADMIN","TEST"], ... }
```

`ResourceServerManager.check()` 流程：

1. `HttpMethod.OPTIONS` 预检直接放行
2. 解析 JWT → `jwtUserMap`；为空则拒绝（路径含 `/socket.io/` 例外放行）
3. 读 Redis URL-角色规则，`AntPathMatcher` 匹配请求路径
4. **未命中任何规则 → 默认放行**（`requireCheck=false`）
5. 命中 → 比对 `tokenStore` 中 authorities 与允许角色集合，交集非空通过

⚠️ **方法级权限注解全项目 0 处**（`@PreAuthorize`/`@Secured`/`@RolesAllowed`），
权限只在网关层做，**勿在业务模块引入方法级注解**。
权限编码规划（尚未落地）：`fs:{resource}:query/save/remove/status`，与接口路径解耦。

### 5.3 参数校验

`javax.validation`（Bean Validation）+ Spring `@Validated`。

**校验分组**定义在 `BaseModel` 内部注解（**小写命名**，非 Java 常规大写）：

```java
public class BaseModel {
    public @interface page {}           public @interface list {}
    public @interface add {}            public @interface edit {}
    public @interface delete {}         public @interface detail {}
    public @interface export {}         public @interface tree {}
    public @interface updateStatus {}
}
```

用法：

```java
@NotNull(message = "id不能为空", groups = {edit.class, updateInfo.class})
public Long id;

@NotBlank(message = "人员名称不能为空", groups = {add.class, edit.class, updateInfo.class})
public String userName;

// 模块可扩展私有分组
public @interface updateInfo {}
```

Controller 指定分组：`@Validated({BaseModel.add.class}) @RequestBody XxxParam`；不指定则用默认组。

**fs 新规范**：`*SaveParam` 不用分组，直接平铺校验，message 中文：

```java
@NotBlank(message = "名称不能为空")                                     private String name;
@NotNull(message = "RTP起始端口不能为空")
@Min(value = 1, message = "端口不能小于1")
@Max(value = 65535, message = "端口不能大于65535")                       private Integer startRtpPort;
@Pattern(regexp = "[01]")                                              private String selected;
```

**Controller 侧**：JSON 体 `@Validated @RequestBody XxxParam`；
查询参数**显式** `@RequestParam(value="id", required=false)`，必填省略 `required`；
`@RequestParam(defaultValue="20") Integer limit` 给默认值。

**校验失败处理**（`ApiController.handleMethodArgumentNotValidException`）：
HTTP `setStatus(200)` + `code=2` + message = 所有 `ObjectError.getDefaultMessage()` 用 `", "` 拼接。
**不返回 400**。（fs 计划文档中 MockMvc 期望 400 是 `standaloneSetup` 未挂载 `ApiController` 的行为差异）

### 5.4 数据脱敏

- `AppUtils.getMobileMask(String mobile)`；`NotNullMap.putMobileMask(k, v)`
- 密码/密钥/会议密码：**只写不读** —— 列表不返回、详情固定 `null`、修改时空值保持数据库原值、日志不打印明文

### 5.5 防重提交

⚠️ **部分缺失** —— `RespCode.CODE_318(318,"访问重复请求")` 已定义，
但无 `@RepeatSubmit` 注解或 AOP 实现。需自行实现时**必须复用该响应码**。

### 5.6 限流（Sentinel）

`spring-boot-starter-sentinel` 提供两套处理器：

- `SentinelExceptionHandler implements BlockExceptionHandler`（Web MVC，`SentinelConfig` 注册）
- `SentinelGatewayExceptionHandler implements BlockRequestHandler`（网关，`SentinelGatewayConfig` 注册）
- 共同父类 `AbstractSentinelExceptionHandler.handle(Throwable)` 映射：
  `FlowException→101`、`DegradeException→102`、`ParamFlowException→103`、
  `SystemBlockException→104`、`AuthorityException→105`

规则在 Nacos `sentinel-config-{env}.yaml` 配置，**代码中无 `@SentinelResource`**。

### 5.7 分布式锁

⚠️ **仅工具方法，无注解式封装、无 Redisson**：

```java
RedisUtils.getLock(String key, Long time)   // setIfAbsent(key, key, time, TimeUnit.SECONDS)
RedisUtils.releaseLock(String key)
```

需手动 try-finally 调用。Redis 过期常量：`Constant.EXRP_MINUTE=60`、`EXRP_HOUR=3600`、`EXRP_DAY=86400`。

### 5.8 多租户

`spring-boot-starter-mybatis`：

- 开关 `@ConditionalOnProperty(prefix="mybatis.tenant", value="enable", matchIfMissing=true)`
  → **默认开启**，`mybatis.tenant.enable=false` 关闭
- `TenantLineInnerInterceptor` + `DefaultTenantDatabaseInterceptor implements TenantLineHandler`，
  必须 `MyBatisUtils.addInterceptor(interceptor, inner, 0)` **插到首位**（分页插件之前，MP 硬性规定）
- 上下文 `TenantContextHolder`（ThreadLocal）+ `TenantContextWebFilter extends OncePerRequestFilter`
  （`FilterRegistrationBean` 注册，从 JWT `Constant.SCHEMES_TENANT_ID="schemasTenantId"` 解析）
- 跳过租户：`@TenantIgnore` + `TenantIgnoreAspect`；或 `TenantUtils` 编程式

### 5.9 文件上传限制

`bootstrap.yml`：`spring.servlet.multipart.max-file-size=10MB`、`max-request-size=100MB`；
超限由 `ApiController` 捕获 `MaxUploadSizeExceededException` 返回 code=2。

### 5.10 其他安全约定

加密密钥 `Constant.SECRET_KEY` / `SECRET_IV`；
密码加密 `ConstEnum.PasswordEncoderTypeEnum`（`BCRYPT "{bcrypt}"` / `NOOP "{noop}"`）；
编码统一 `Constant.CHARSET_UTF_8`。

---

## 六、错误处理与日志规范

### 6.1 异常体系

`cn.com.tzy.springbootcomm.excption`（注意拼写）：

| 类 | 结构 | 全局处理器识别 |
|---|---|---|
| `RespException` | `extends RuntimeException`，**public 字段** `Integer code` + `String message`；构造器 `(RespCode)`/`(int,String)`/`(String)`/`()` | ✅ 唯一 |
| `BizException` | `extends RuntimeException` + `@Getter` | ❌ 走通用分支 |
| `ParamException` | `extends RuntimeException` | ❌ |
| `JwtException`/`TimingException`/`UserException` | `extends RuntimeException` | ❌ |

抛出方式：

```java
throw new RespException(RespCode.CODE_311);
throw new RespException(RespCode.CODE_2.getValue(), "自定义消息");
throw new RespException("消息");     // code 为 null 时降级 CODE_2
```

**优先 `return RestResult.result(CODE_2, msg)` 表达业务失败，不抛异常。**

### 6.2 全局异常处理器 `ApiController`

`spring-boot-starter-cloud`，`@RestControllerAdvice` + `@Log4j2`，所有 Controller 继承它。

```java
@InitBinder   // 注册 DateEditor 处理 Date 参数绑定
@ExceptionHandler(Exception.class)
public void handleException(Exception e, HttpServletResponse response) throws IOException
```

**核心约定：所有异常 HTTP 状态一律 `setStatus(200)`**，错误通过 body 的 `code` 表达；
序列化用 `cn.hutool.json.JSONUtil.toJsonStr(RestResult)` 直写 `response.getWriter()` + `flushBuffer()`；
`ContentType.APPLICATION_JSON`。**勿另建 `@ControllerAdvice`。**

分支映射（顺序即优先级）：

| 异常 | 处理 |
|---|---|
| `MethodArgumentNotValidException` | code=2，所有校验 `getDefaultMessage()` 用 `", "` 拼接 |
| `HttpRequestMethodNotSupportedException` | code=2，`"不支持{method}请求方法，支持以下{methods}、"` |
| `DuplicateKeyException` | 正则提取 值/表/键 → `ClassScanner.scanPackage("cn.com.tzy.springbootentity.dome")` 按 `@TableName` 匹配实体 → 按 `@TableField.value` 匹配列 → 取 `@ApiModelProperty.value` 作字段中文名 → `"{字段描述} {值} 字段已经被占用"` |
| `MaxUploadSizeExceededException` | code=2，`"文件大小超出10MB限制, 请压缩或降低文件质量!"` |
| `DataIntegrityViolationException` | code=2，`"字段太长,超出数据库字段的长度"` |
| `RespException` | 用其 `code`（null 则 CODE_2）+ `message` |
| `PoolException`（Redis） | code=2，`"Redis 连接异常!"` |
| `NoHandlerFoundException` | code=2，`"路径不存在，请检查路径是否正确"` |
| 其他 `Exception` | code=2，`e.getMessage()` |

最后统一 `log.error("错误日志:", exception)`。

⚠️ **`DuplicateKeyException` 分支强依赖**：实体必须在 `cn.com.tzy.springbootentity.dome` 包下、
必须有 `@TableName`、字段必须有 `@TableField(value=列名)` 与 `@ApiModelProperty`、
索引名必须 `idx_`/`un_`/`fk_` 前缀（正则 `"Duplicate entry '(.*?)' for key '(idx|un|fk)_(.*?)'"`）。
新增实体务必满足，否则唯一键冲突提示退化为"字段已经被占用"。

Sentinel 限流异常独立处理（不经 `ApiController`），返回 code 101-105。

### 6.3 请求日志切面 `LoggerAspect`

`spring-boot-starter-cloud`，`@Aspect @Log4j2`：

- 切点 `execution(public * cn.com.tzy.springboot*.controller..*.*(..))` **且**带
  `@PostMapping`/`@GetMapping`/`@PutMapping`/`@DeleteMapping`/`@RequestMapping`
  （五个 `@Pointcut` 用 `||` 组合）
- `@Around`，**仅在 `log.isDebugEnabled()` 时**打印，否则直接 `point.proceed()`（生产零开销）
- 打印 `class: {} method: {}`、`param: {index} {json}`、`response: {json}`
- 跳过参数类型：`HttpServletRequest`、`HttpServletResponse`、`BindingResult`、`MultipartFile`、`MultipartFile[]`
- 序列化 `AppUtils.encodeJson(obj)`，失败 `log.error("encodeJson error", e)` 不中断

**勿在业务代码中重复实现请求参数打印。**

### 6.4 操作日志（落库 `sys_logs`）

```java
// 注解（spring-boot-starter-logs-basic）
@Target({FIELD, METHOD}) @Retention(RUNTIME) @Inherited @Documented
public @interface ApiLog { LogsTypeEnum type(); }

// 使用
@ApiLog(type = LogsTypeEnum.XXX)
@PostMapping("save")
public RestResult<?> save(...) { ... }
```

`LogsAspect`（`logs-core`，`@Aspect @Log4j2`）：

- 切点 `execution(public * cn.com.tzy.springboot*..*.*(..)) && @annotation(...ApiLog)`
- 采集 `request.getMethod()`、`getRequestURI()`、`IPUtil.getIp(request)`、参数装入 `NotNullMap`
  （跳过 Servlet/BindingResult/MultipartFile）、`System.currentTimeMillis()` 前后差算 `duration`
- **异步落库**：

```java
ThreadUtil.execute(() -> {
    LogApi logApi = SpringUtil.getBean(LogApi.class);
    logApi.logs(type, method, url, ip, param, result, duration);
});
```

⚠️ 异步线程内**必须** `SpringUtil.getBean()` 取 Bean（RequestContext/ThreadLocal 不传递）。

### 6.5 异步事件驱动（缓存一致性核心模式）

```java
// 事件对象：@Getter + final 字段 + 构造器
@Getter
public class FsCompanyConfigChangedEvent {
    private final Long companyId;
    public FsCompanyConfigChangedEvent(Long companyId) { this.companyId = companyId; }
}

// 发布（事务内）
@Component @RequiredArgsConstructor
public class FsRuntimeConfigRefreshPublisher {
    private final ApplicationEventPublisher publisher;

    public void publishCompanyChanged(Long companyId) {
        if (companyId != null) publisher.publishEvent(new FsCompanyConfigChangedEvent(companyId));
    }
    public void publishCompaniesChanged(Collection<Long> ids) {   // 必须去重 + 过滤 null
        if (ids != null) ids.stream().filter(Objects::nonNull).distinct().forEach(this::publishCompanyChanged);
    }
}

// 监听（事务提交后）
@Component @RequiredArgsConstructor
public class FsRuntimeConfigRefreshListener {
    private final FsRuntimeConfigService runtimeConfigService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompanyChanged(FsCompanyConfigChangedEvent e) {
        runtimeConfigService.refreshCompany(e.getCompanyId());
    }
}
```

**铁律**：缓存刷新必须 `AFTER_COMMIT`，回滚不得刷新；定向刷新（按 companyId/agentId），
**禁止 `delAll()`**；启动装载与管理接口刷新共用同一套组装逻辑
（`FsRuntimeConfigService.refreshAll()` / `refreshCompany(id)`）。

完整调用链（fs 设计文档 §7）：

```
Controller -> Fs*ManageService -> 现有 fs IService/Mapper + FsRelationValidator
  -> FsRuntimeConfigRefreshPublisher -> @TransactionalEventListener(AFTER_COMMIT)
  -> FsRuntimeConfigService -> RedisService 各 Manager
```

### 6.6 MQ 异步

`spring-boot-starter-rabbitmq`：`MQConfig<T>` 注册 `RabbitAdmin`（`setIgnoreDeclarationExceptions(true)`）+
`ContentTypeDelegatingMessageConverter`（`"application/json"` → `Jackson2JsonMessageConverter`）+ `MqClient`。
消息模型放 `springbootentity.mq`（`QRDataModel`、`QrRoutingModel`）；
业务侧 Handler 放 `{module}.hedel`（bean 模块，拼写如此）。

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
- `shared-configs` 复用 8 份公共配置（见 0.3）
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

Docker 标准 `ENTRYPOINT` JVM 参数见 0.3。模块 POM 需指定 `<mainClass>` 与 `<includes>non-exists</includes>`。

---

## 八、测试规范与数据库表命名

### 8.1 测试框架与写法

JUnit **4**（`org.junit.Test`，非 JUnit5）+ Spring Test + Mockito + MockMvc。

```java
// 集成测试（旧模块）
@Log4j2
@RunWith(SpringRunner.class)
@SpringBootTest
public class MinioTest {
    @Resource private MinioUtils minioUtils;
    @Test public void imageToBean64() { ... }
}

// 单元测试（新规范，无 Spring 上下文）
@RunWith(MockitoJUnitRunner.class)
public class PlatformManageServiceTest {
    @Mock private PlatformService platformService;
    @InjectMocks private PlatformManageServiceImpl service;

    @Test public void saveRejectsDuplicateName() {
        RestResult<?> result = service.save(PlatformSaveParam.builder().name("fs-main").build());
        assertEquals(RespCode.CODE_2.getValue(), result.getCode());
        assertEquals("平台名称已存在", result.getMessage());
        verify(platformService, never()).save(any(Platform.class));
    }
}

// Controller 契约测试（不启动 Spring 上下文）
@RunWith(MockitoJUnitRunner.class)
public class PlatformManageControllerTest {
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private PlatformManageService platformManageService;
    @InjectMocks private PlatformManageController controller;

    @Before public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test public void detailUsesGet() throws Exception {
        mockMvc.perform(get("/api/fs/platform/detail").param("id", "1"))
               .andExpect(status().isOk());
    }

    @Test public void removeUsesDelete() throws Exception {
        mockMvc.perform(delete("/api/fs/platform/remove").param("id", "1"))
               .andExpect(status().isOk());
    }
}

// 参数校验测试（手动 Validator）
Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
Set<ConstraintViolation<PlatformSaveParam>> violations = validator.validate(param);

// @TransactionalEventListener AFTER_COMMIT 测试（需最小事务上下文）
@ContextConfiguration(classes = FsRuntimeConfigRefreshListenerTest.Config.class)
static class Config {
    @Bean TestPlatformTransactionManager transactionManager();
    @Bean TestTransactionService transactionService();
    @Bean FsRuntimeConfigRefreshListener listener(...);
}
// refreshRunsAfterCommit() / refreshDoesNotRunAfterRollback()
```

### 8.2 测试执行与覆盖

- 根 POM `maven-surefire-plugin` 全局 `<skipTests>true</skipTests>`
- **禁止改根 POM**；需跑测试的模块在自己 POM 覆盖（当前仅 `spring-boot-business/spring-boot-fs`）：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration><skipTests>false</skipTests></configuration>
</plugin>
```

- 运行命令（PowerShell）：

```
mvn -pl spring-boot-business/spring-boot-fs -am -Dtest=PlatformManageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 位置：`src/test/java/cn/com/tzy/springboot{module}/...`
- 命名：类 `{被测类}Test`；方法描述行为，不用 `test1`：
  `saveRejectsReversedRtpRange`、`saveRejectsDuplicateName`、`updatePreservesRuntimeStatus`、
  `removeRejectsEnabledPlatform`、`refreshRunsAfterCommit`、`refreshDoesNotRunAfterRollback`、
  `publisherDeduplicatesCompanyIds`
- 三层测试方案（fs 设计文档 §9）：Service 单元（Mockito）→ 参数校验（Validator）→
  Controller 契约（MockMvc standalone）；事务型行为加最小事务上下文验证 `AFTER_COMMIT`
- 提交前：`git status --short` + `git diff --check`；**不主动 `git commit`**

### 8.3 数据库表命名与建表约定

**表名**：`{module}_` 全小写下划线 —— `bean_*`、`fs_*`、`sys_*`、`oa_*`、`face_*`、`video_*`、`sms_*`；
第三方保留官方命名 `qrtz_*`（大写）、`act_*`。

**关联表**：`{主}_connect_{从}`（`bean_user_connect_role`）或 `{主}_{从}`（`fs_agent_group`、`fs_skill_agent`）。

**列与索引**：

| 项 | 约定 |
|---|---|
| 主键 | `id` BIGINT AUTO_INCREMENT（`LongIdEntity`）；特例 `fs_media_server` 字符串 ID |
| 审计字段 | 继承 `Base` 的表必带 `create_user_id`、`create_time`、`update_user_id`、`update_time` |
| 多租户 | `tenant_id` BIGINT，默认公共租户 `1L`（`Constant.TENANT_ID`） |
| 逻辑删除 | ❌ **无**（全项目 0 处 `@TableLogic`/`deleted`/`del_flag`/`is_deleted`）→ 物理删除，删前必须引用校验 |
| 状态 | `is_enable`/`enable`/`status` TINYINT/INT 取 0/1；`status` 为运行时状态，save 须置 null |
| 备注 | 统一 `memo` |
| 索引前缀 | 唯一 `uni_idx_*`/`un_*`，普通 `idx_*`，外键 `fk_*`（`ApiController` 正则强依赖） |
| 物理外键 | 普遍不建，引用完整性由业务层 + `@Transactional` 保证 |
| 关键字列名 | 反引号：`` `name` ``、`` `enable` ``、`` `status` `` |
| 唯一索引示例 | `uni_idx_phone(vdn_id, company_id)` —— 业务层预校验须与之对齐，唯一索引是最终防线 |

**SQL 文件位置**：根目录

- `sql/{module}.sql`：`freeswitch.sql`、`quartz.sql`、`sys_area.sql`、`sys_area_1.sql`、`sys_face.sql`、`sys_video.sql`
- `sql/sys_{module}-基类sql.sql`：`sys_bean-基类sql.sql`、`sys_oa-基类sql.sql`
- `sql/sys_{module}_初始化.sql`：`sys_bean_初始化.sql`

**数据库划分**（每模块独立库）：`sys_bean_dev`、`sys_oa`、`sys_video`、`sys_face`、`freeswitch`、
`nacos`、`seata`、`xxl_job`。建表语法 `CREATE TABLE [IF NOT EXISTS] {表名} ( ... )`。

**分页与查询约束**：分页大小设上限；日志类查询强制时间范围；`select` 下拉默认 `limit=20`；
企业级关联资源必须校验同属一个 `companyId`。

---

## 九、日志输出与管理规范

| 维度 | 约定 |
|---|---|
| API | Log4j2 API（`@Log4j2`），实现 Logback |
| 声明 | Lombok `@Log4j2`，**禁止 `@Slf4j`、禁止 `System.out.println`** |
| 本地路径 | `src/main/resources/logback-test.xml`，仅 Console，`root level=INFO` |
| 生产路径 | `start/logback.xml`，`LOG_PATH=./logs`；容器 `/spring-boot-{module}/logs` → 宿主 `/work/spring-cloud/spring-boot-{module}/logs` |
| 目录结构 | `${LOG_PATH}/{info,debug,warn,error}/app-%d{yyyy-MM-dd}.%i.log` |
| 级别过滤 | `LevelFilter` **精确单级别**（`onMatch=ACCEPT`/`onMismatch=DENY`），非阈值过滤 |
| 单文件大小 | `SizeAndTimeBasedFNATP` `maxFileSize=50MB` |
| 保留策略 | `maxHistory=30`（30 天）+ `CleanHistoryOnStart=true`；`totalSizeCap` 已注释未启用 |
| Pattern | `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %tid  %logger{50}  - %msg%n`（`%tid` 必留） |
| 控制台 Pattern | Spring Boot `%clr(...)` 彩色 + `%tid` + `%-40.40logger{39}` |
| 编码 | `PatternLayoutEncoder` + `<charset>utf-8</charset>`；warn/error `append=true` |
| 链路追踪 | `LayoutWrappingEncoder` + `TraceIdPatternLogbackLayout`（SkyWalking） |
| 上报 | `skywalkingFile` = `GRPCLogClientAppender` → OAP `1.82.217.118:12011` |
| ⚠️ 生产实况 | `<root level="INFO">` **只挂 `skywalkingFile`，四个文件 appender 全被注释** → 本地不落盘，需文件日志时取消注释 |
| 分级 logger | `org.springframework=warn`、`com.apache.ibatis=TRACE`、`java.sql.{Connection,Statement,PreparedStatement}=DEBUG` |
| 请求日志 | `LoggerAspect` 在 **DEBUG** 级自动打印，勿重复实现 |
| 操作日志 | `@ApiLog` + `LogsAspect` 异步落 `sys_logs` |
| 错误日志 | 由 `ApiController` 统一 `log.error("错误日志:", exception)`，业务代码勿重复 catch+log |
| MQ 日志 | MQ 密集模块用 `start/logback-mq.xml`（如 sms） |
| 敏感信息 | 密码/密钥/会议密码**禁止打印明文**；手机号用脱敏工具 |

**级别使用约定**：

- `ERROR` — 需人工介入的异常（全局处理器已兜底）
- `WARN` — 可自愈的降级、重试、配置缺失
- `INFO` — 启动完成、关键业务节点、缓存刷新触发
- `DEBUG` — 请求/响应详情、SQL、中间状态（生产不开）
- `TRACE` — MyBatis 映射细节

---

## 十、注释与文档规范

| 项 | 约定 |
|---|---|
| 总体原则 | 中文为主，说明"为什么"和业务语义，不复述代码；禁止无意义注释 |
| 公共底层类 | `/** 中文描述 */` + `@author TZY`（`RestResult`、`PageResult`、`RespCode`、`PageModel`、`BaseModel`、`Constant`、`ConstEnum`） |
| 第三方改编类 | 保留原作者：`Base.java`（`@author 芋道源码`）、`DefaultDBFieldHandler`（`@author hexiaowu`）、`ResourceServerManager`（`@author xianrui`） |
| **Entity 字段（强制双写）** | JavaDoc 中文 + `@ApiModelProperty(value="中文")` + `@TableField(value="下划线列名")`，三者文案一致 |
| Param/Vo 字段 | 简写 `@ApiModelProperty("编号")`；需示例时 `@ApiModelProperty(value="当前页数", example="1")` |
| Enum 类型字段 | `@ApiModelProperty("用户类型枚举") public ConstEnum.UserTypeEnum type;` |
| 枚举常量 | 前缀 `/** 中文说明 */` 块；`RespCode` 用 `/** 相应状态码 */` 概括整组 |
| Service 接口方法 | 中文说明 + `@return`（可省 `@param`） |
| 工具类方法 | 完整 JavaDoc：`@param`/`@return` + 兼容性说明 |
| Controller 方法 | 旧模块靠 Swagger 注解替代；client 层必写 `@Api(tags=..., position=1)` + `@ApiOperation(value,notes)` + `@ApiImplicitParams(name/value/required/paramType/dataType/example)` |
| 业务模块 Controller | 可省略（内部接口，不直接对外暴露文档） |
| 复杂逻辑 | 行内中文注释标步骤：`// RTP 起止端口校验`、`// 保留运行时 status，不允许通过 save 覆盖`、`// 只更新 enable 字段`、`// 顶级树` |
| 切面/配置类 | 保留模板说明性注释（`LoggerAspect`、`LogsAspect` 中关于 `@Pointcut` 用法的说明） |
| 刻意简化/延后项 | `ponytail:` 前缀注释标记天花板与升级路径（配合 `/ponytail-debt` 汇总） |
| Mapper XML | 保留 `<!--@mbg.generated-->` + `<!--@Table 表名-->`，便于重新生成时识别 |
| Entity 类级 | `@ApiModel(value="部门表")` 或 `@ApiModel(description="平台信息")` |
| 设计文档 | `docs/superpowers/specs/YYYY-MM-DD-{主题}-design.md`（方案对比 / 接口契约 / 校验删除规则 / 测试方案 / 验收标准 / 实施前确认项） |
| 实施计划 | `docs/superpowers/plans/YYYY-MM-DD-{主题}.md`（按 Task 拆分，每 Task 含 Files 清单 + RED/GREEN 步骤 + PowerShell 验证命令 + 预期结果 + `- [ ]` checkbox） |
| **规范类文档** | 统一放 `.qoder/rules/`（本文件为唯一权威副本），不再新建 docs 副本 |

⚠️ `@ApiModelProperty.value` 被 `DuplicateKeyException` 分支反射读取，**缺失则唯一键冲突提示不含字段名**。

---

## 十一、缺失与待补充清单

以下项目**代码中不存在**。需要时必须先问用户，**禁止凭通用经验臆造**。

| # | 项 | 状态 | 说明 |
|---|---|---|---|
| 1 | 金额字段约定（精度 / 单位 元 vs 分） | 🔴 待补充 | `spring-boot-pay` README 标注"暂未开发"，全项目无 `BigDecimal` 金额实证 |
| 2 | 防重提交统一实现 | 🟡 部分缺失 | `RespCode.CODE_318(318,"访问重复请求")` 已定义，无 `@RepeatSubmit` 注解或 AOP |
| 3 | 注解式分布式锁 | 🟡 部分缺失 | 仅 `RedisUtils.getLock/releaseLock` 裸方法，无 Redisson，需手动 try-finally |
| 4 | 方法级权限注解 | 🟡 架构性缺失 | `@PreAuthorize`/`@Secured`/`@RolesAllowed` 全项目 0 处；权限仅在网关 URL-角色层。`fs:{resource}:*` 编码已规划未落地 |
| 5 | 测试覆盖率与 CI 门禁 | 🔴 待补充 | 根 POM 全局 `skipTests=true`，仅 fs 覆盖；无 JaCoCo / 阈值 / CI 门禁 |
| 6 | 代码风格强制工具 | 🔴 待补充 | 无 Checkstyle / Spotless / PMD / EditorConfig，规范靠约定与评审维持 |
| 7 | 数据库版本管理 | 🔴 待补充 | SQL 以 `sql/*.sql` 静态文件存放，无 Flyway / Liquibase |
| 8 | API 版本化策略 | 🔴 待补充 | 路径无 `/v1/` 版本段（fs 设计文档中 `/api/v1/users/*` 仅为鉴权示例） |
| 9 | 国际化 / 错误码多语言 | 🔴 待补充 | 提示文案硬编码中文，无 `MessageSource` / i18n 资源 |
| 10 | 行级数据权限 | 🔴 待补充 | 仅租户级隔离（`tenant_id`），无部门/个人级过滤实现 |
| 11 | `PlatformSaveParam.java` 中文注释编码 | ⚠️ 已知问题 | 部分工具下显示为 `???`，疑似编码问题，未修改 |

---

## 附录：fs 模块 41 张表页面归属

来源：`docs/superpowers/specs/2026-07-27-spring-boot-fs-management-api-design.md` §4。
新增 fs 管理端功能时，先在此定位表所属页面，**关联表不单独建菜单**。

| 页面分组 | 表 |
|---|---|
| 系统配置 | `fs_platform`、`fs_media_server`、`fs_gate_way`、`fs_route_gateway`、`fs_route_group`(+`fs_route_gateway_group`)、`fs_route_call` |
| 企业资源 | `fs_company`、`fs_company_conference`、`fs_company_display`、`fs_company_phone`(+`fs_company_phone_group`)、`fs_playback` |
| 呼叫中心 | `fs_group`(+`fs_group_agent_strategy`/`fs_group_strategy_exp`/`fs_group_memory_config`/`fs_group_overflow`/`fs_skill_group`)、`fs_skill`(+`fs_skill_agent`/`fs_skill_group`)、`fs_agent`(+`fs_agent_sip`/`fs_agent_group`/`fs_user_agent`/`fs_skill_agent`)、`fs_overflow_config`(+`fs_overflow_front`/`fs_overflow_exp`) |
| 呼入 IVR | `fs_ivr_workflow`、`fs_vdn_schedule`、`fs_vdn_code`(+`fs_vdn_phone`/`fs_vdn_config`/`fs_vdn_dtmf`) |
| 运行记录（**只读**） | `fs_call_log`(+`fs_call_detail`/`fs_call_device`/`fs_call_dtmf`)、`fs_agent_state_log`、`fs_group_memory`、`fs_push_log` |

**原则**：关联表只在所属页面的详情/抽屉/标签页中管理；日志表只读。

---

## 变更记录

| 日期 | 变更 |
|---|---|
| 2026-09-28 | 首次建立。基于 `5.0.0-dev` 分支全量代码与配置实证，含项目概况 + 9 类规范 + 注释文档规范 + 11 项缺失标注 + fs 表归属附录。原计划另建 `docs/superpowers/specs/` 报告体副本，已取消 —— 本文件为唯一权威副本，避免双份维护漂移。 |
