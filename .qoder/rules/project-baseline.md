---
trigger: always_on
---

# 逆龙傲（spring-boot-cloud）开发基线铁律

> 常驻最小集。完整规范已按章拆为 9 个按需分册（同目录），**需要时才加载对应那一份**：
>
> | 分册 | 什么时候看 |
> |---|---|
> | `project-overview.md` | 技术栈与版本、模块划分、运行环境、业务定位 |
> | `naming-conventions.md` | 类/包/URL/表/字段/索引/枚举命名、历史拼写可改否 |
> | `architecture-patterns.md` | 功能落哪个模块、是否反模式、依赖、配置、Git、部署 |
> | `coding-standards.md` | 响应/主键/审计/时间/Lombok/分页/查询构造/树、注释文档 |
> | `layered-templates.md` | 照抄 Service/Controller/Mapper XML/Convert/Feign 骨架 |
> | `security-validation.md` | 参数校验、网关鉴权、限流、锁、租户、脱敏 |
> | `exception-logging.md` | 异常与错误码、日志切面、操作日志、缓存事件 |
> | `testing-database.md` | 写/跑测试、建表 SQL 与表结构 |
> | `gaps-and-fs-tables.md` | 项目里根本不存在的机制（别编）、fs 41 张表页面归属 |

## 1. 响应与错误（不可违反）

- 统一响应 `RestResult<T>`，**没有 `R.ok()/R.fail()`**：
  - 成功：`RestResult.result(RespCode.CODE_0.getValue(), null, data)`
  - 业务失败：`return RestResult.result(RespCode.CODE_2.getValue(), "中文提示")` —— **return，不抛异常**
- 分页统一 `PageResult`：`MyBatisUtils.selectPage(page)` 一步转换。
- 唯一被全局处理器识别的自定义异常是 `RespException`（`cn.com.tzy.springbootcomm.excption`，注意拼写）。
- 所有异常 HTTP 状态一律 `200`，错误靠 body 的 `code` 表达（`ApiController` 已实现，勿另建 `@ControllerAdvice`）。
- Controller **必须** `extends ApiController`（`cn.com.tzy.springbootstartercloud.api.ApiController`）。

## 2. 命名（照抄，勿"优化"）

| 类型 | 后缀/位置 |
|---|---|
| Entity | 无后缀，`springbootentity.dome.{module}`，`@TableName("表名")` |
| 入参 | `*Param`，`springbootentity.param.{module}` |
| 出参 | `*Vo`，`springbootentity.vo.{module}` |
| Mapper | `*Mapper` + `@Mapper`，`{module}.mapper.sql` |
| Service | `*Service extends IService<E>`，`{module}.service.api` |
| ServiceImpl | `*ServiceImpl extends ServiceImpl<M,E>`，`service.api.impl` |
| 管理 Service（新） | `*ManageService`（不继承 IService），`service.manage.{group}` |
| 转换器 | `*Convert`（MapStruct + `INSTANCE`），`{module}.convert.{group}` |
| Controller | `*Controller`，`{module}.controller.api.{group}` |
| Feign | `*ServiceFeign`，`springbootfeign{module}.api.{group}` |

- `@RestController` **必须显式指定唯一 Bean 名**：`@RestController("Api{Module}{Resource}Controller")`。
- URL：业务端 `/api/{module}/{resource}/{action}`；Web 端 `/webapi/...`；小程序 `/app/...`。
- action 词表：`page`/`detail`/`save`/`remove`/`select`/`tree`/`all`/`status`；多词用下划线（`save_gateways`）。
- **历史拼写必须沿用，禁止修正**：`excption`、`dome`、`spingbootstartermybatis`、
  `srpingbootstartersecurityoauth*`、`hedel`、`seate-config-*.yaml`、`ActivitiServcieImplTest`。

## 3. 实体字段

- 继承 `LongIdEntity`（`@TableId(type = IdType.AUTO)`）；主键 `id` 数据库自增。
- 审计字段由 `Base` + `DefaultDBFieldHandler` 自动填充，**勿手动赋值**。
- 多租户字段 `tenant_id`；默认公共租户 `Constant.TENANT_ID = 1L`。
- 时间用 `java.util.Date` + `@DateTimeFormat` 与 `@JsonFormat` **双注解**，pattern 必须引用
  `Constant.DATE_TIME_FORMAT`，**禁止硬编码格式串**。
- 状态字段 `Integer` 0/1，语义查 `ConstEnum.Flag`；`status` 表运行时状态，save 时须置 `null`。
- 字段必须同时写 JavaDoc 中文 + `@ApiModelProperty(value="中文")`，
  **且 `@TableField(value="下划线列名")` 显式映射** —— `DuplicateKeyException` 提示依赖反射读取这些注解。
- 索引前缀必须 `idx_` / `un_` / `uni_idx_` / `fk_`（同上，异常处理器正则依赖）。

## 4. 模块归属（放错位置即返工）

| 要写的东西 | 放这里 |
|---|---|
| 业务功能 | `spring-boot-business/spring-boot-{module}` |
| Web 聚合接口 | `spring-boot-client/spring-boot-web-api`（Service 仅转发 Feign） |
| 小程序接口 | `spring-boot-client/spring-boot-app` |
| 调用契约 | `spring-boot-feign/spring-boot-feign-{module}`（只放接口） |
| 共享 Entity/Param/Vo | `spring-boot-common/spring-boot-entity` |
| 工具/响应/异常/常量 | `spring-boot-common/spring-boot-comm` |
| 第三方技术封装 | `spring-boot-starter/spring-boot-starter-{tech}`（复杂拆 `-basic` + `-core`） |
| 建表 SQL | 根 `sql/{module}.sql` |

- 依赖流向单向：`client → feign → business → starter → common`；**业务模块间禁止直接依赖**。
- 新依赖三步走：根 `pom.xml` `<properties>` 加版本变量 → `<dependencyManagement>` 声明 → 子模块引用**不写 version**。
- 复用优先级：`spring-boot-comm` 工具 > `starter-*` 封装 > Hutool > 自写。
  已有勿重造：`AppUtils`、`JwtUtils`、`TreeUtil`、`MyBatisUtils`、`RedisUtils`、`MinioUtils`、`IPUtil`、`NotNullMap`。

## 5. 校验与权限

- 校验分组定义在 `BaseModel` 内部注解，**小写命名**：`page/list/add/edit/delete/detail/export/tree/updateStatus`；
  用法 `@NotBlank(message="xx不能为空", groups={add.class, edit.class})`。
- fs 新规范的 `*SaveParam` 不用分组，直接平铺 `@NotBlank/@NotNull/@Min/@Max/@Pattern`，message 中文。
- JSON 体：`@Validated @RequestBody XxxParam`；查询参数**显式** `@RequestParam`。
- 权限**只在网关层**（URL-角色规则存 Redis `Constant.ALL_URL_KEY`）；
  项目 **0 处** `@PreAuthorize/@Secured/@RolesAllowed`，勿引入方法级注解。
- 密码/密钥/会议密码：**只写不读** —— 列表不返回、详情返回 `null`、修改时空值保持原值、日志不打印明文。
- 手机号脱敏用 `AppUtils.getMobileMask()` 或 `NotNullMap.putMobileMask()`。

## 6. 日志

- 声明统一 `@Log4j2`（`lombok.extern.log4j.Log4j2`），**不用 `@Slf4j`**。
- 操作日志：方法加 `@ApiLog(type = LogsTypeEnum.XXX)`，由 `LogsAspect` 异步落 `sys_logs`。
- 异步线程内取 Bean 必须 `SpringUtil.getBean(X.class)`（ThreadLocal 不传递）。
- 缓存刷新必须 `@TransactionalEventListener(phase = AFTER_COMMIT)`，**回滚不得刷新**；
  定向刷新（按 companyId/agentId），**禁止 `delAll()`**。
- 禁止 `System.out.println`；请求参数日志由 `LoggerAspect` 在 DEBUG 级自动打印，勿重复实现。

## 7. 数据库

- 表名 `{module}_` 全小写下划线（`bean_*`/`fs_*`/`sys_*`/`oa_*`/`face_*`/`video_*`/`sms_*`）。
- 关联表 `{主}_connect_{从}` 或 `{主}_{从}`。
- MySQL 关键字列名加反引号：`` `name` ``、`` `enable` ``、`` `status` ``。
- **无逻辑删除**（全项目 0 处 `@TableLogic`/`del_flag`）→ 物理删除，**删除前必须做引用校验**，
  被引用则 `return RestResult.result(CODE_2, "请先删除xxx")`。
- 普遍不建物理外键，引用完整性由业务层 + 事务保证。
- 写操作必须 `@Transactional(rollbackFor = Exception.class)`。

## 8. 测试

- JUnit **4**（`org.junit.Test`）+ `@RunWith(SpringRunner.class) @SpringBootTest`（集成）
  / `@RunWith(MockitoJUnitRunner.class)`（单元）/ `MockMvcBuilders.standaloneSetup`（Controller 契约）。
- 根 POM 全局 `skipTests=true`，**禁止改根 POM**；需跑测试的模块在自己 POM 覆盖 `skipTests=false`（当前仅 `spring-boot-fs`）。
- 命令：`mvn -pl spring-boot-business/spring-boot-fs -am -Dtest=XxxTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 类名 `{被测类}Test`，方法名描述行为（`saveRejectsDuplicateName`、`removeRejectsEnabledPlatform`）。

## 9. 禁止清单

- ❌ 用 Entity 直接接收管理端 `@RequestBody`
- ❌ 一表一 CRUD 机械暴露；❌ 元数据通用 CRUD（按页面聚合）
- ❌ 日常保存调用 Redis `delAll()`
- ❌ 管理接口执行 FreeSWITCH CLI / 系统命令 / 文件删除
- ❌ Controller 注入 Mapper 或基础 Service（只注入 `*ManageService`）
- ❌ 事务内刷新缓存
- ❌ 用 quartz 写新定时任务（已弃用，改 `@XxlJob("handlerName")`）
- ❌ 子模块 `<dependency>` 写 `<version>`；❌ 硬编码日期格式串
- ❌ 修正历史拼写
- ❌ 未经用户明确要求执行 `git commit`

## 10. 新旧模块隔离

改**旧模块**（bean/video/sms/oa/face/activiti）→ 遵循旧模式：`service/api` + `extends ServiceImpl` +
`@Autowired` 字段注入 + 单一 `XxxParam` + `@GetMapping("remove")`，**不做无关重构**。

建**新管理端功能** → 遵循 fs 规范：`service/manage/{group}` + 纯接口实现（不继承 IService）+
构造器注入 `private final` + `*PageParam`/`*SaveParam`/`*StatusParam`/`*DetailVo`/`*OptionVo` +
MapStruct Convert + `@DeleteMapping("remove")` + TDD。

## 11. 查不到就别编

以下项目**代码中不存在**，需要时必须先问用户，禁止凭通用经验臆造：
金额字段约定、防重提交注解、注解式分布式锁（Redisson）、方法级权限注解、
覆盖率门禁（JaCoCo）、代码风格工具（Checkstyle/Spotless）、DB 版本管理（Flyway）、
API 版本化（`/v1/`）、国际化（i18n）、行级数据权限。
完整清单见 `.qoder/rules/gaps-and-fs-tables.md`。
