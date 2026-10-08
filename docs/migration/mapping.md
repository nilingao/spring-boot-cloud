# NLA Cloud 迁移台账

> 本文件是 `5.0.0-dev`（spring-boot-cloud 微服务）→ `6.0.0-nla`（单体多模块）迁移的**唯一事实台账**。
> 记录命名基线、模块清单、类与表的映射关系、以及**全部有意偏离上游参考实现的地方**。
>
> 上游参考：`F:\work\java\_ref_RuoYi-Vue-Plus-6X`（RuoYi-Vue-Plus 6.X，只读、不入库）。
> 后续阶段开工前先读本文件，收尾后回来勾选进度。

## 目录

- [1. 命名基线](#1-命名基线)
- [2. 模块清单与规模](#2-模块清单与规模)
- [3. 阶段 2.1 引入取舍（已被推翻）](#3-阶段-21-引入取舍已被推翻)
- [4. 基座类映射（旧 → 新）](#4-基座类映射旧--新)
- [5. starter → nla-common 映射](#5-starter--nla-common-映射)
- [6. 业务模块映射](#6-业务模块映射)
- [7. 有意偏离上游的清单](#7-有意偏离上游的清单)
- [8. 阶段进度台账](#8-阶段进度台账)
- [9. 待清理项](#9-待清理项)
- [10. 迁移脚本索引](#10-迁移脚本索引)

---

## 1. 命名基线

全工程强制。**去若依化是硬性要求**，任何新增文件都必须符合本节。

### 1.1 工程与坐标

| 层级 | 上游参考名 | 本工程命名 |
|---|---|---|
| 工程根 artifactId | `ruoyi-vue-plus` | **`nla-cloud`** |
| Maven groupId | `org.dromara` | **`cn.com.nla`** |
| revision（工程版本） | `6.0.0` | **`1.0.0`** |
| Java 根包 | `org.dromara` | **`cn.com.nla`** |
| Git 分支 | — | **`6.0.0-nla`** |
| JavaDoc `@author` | `Lion Li` / `Michelle.Chung` | **`TZY`** |
| 数据库名 | `ry-vue` | **`nla-cloud`** |
| 镜像仓库前缀 | `ruoyi/ruoyi-*:6.0.0` | **`nla/nla-*:1.0.0`** |

`revision` 取 `1.0.0` 而非跟随上游 `6.0.0`：新工程是独立版本线，延续旧根 POM 的 `1.0.0`。
所有镜像 tag、`.run/*.run.xml` 的 `imageTag`、docker-compose 的 `image:` 都必须用 `1.0.0`。

### 1.2 一级目录与模块

| 上游参考名 | 本工程命名 |
|---|---|
| `ruoyi-admin` | `nla-admin` |
| `ruoyi-api` | `nla-api` |
| `ruoyi-common` | `nla-common` |
| `ruoyi-common/ruoyi-common-bom` | `nla-common/nla-common-bom` |
| `ruoyi-modules` | `nla-modules` |
| `ruoyi-extend` | `nla-extend` |
| `script` | `script`（通用词，保留原名） |
| `docs/migration` | `docs/migration`（本工程新增，即本文件所在） |

### 1.3 类与资源文件

| 上游参考名 | 本工程命名 |
|---|---|
| `org.dromara.DromaraApplication` | `cn.com.nla.NlaApplication` |
| `org.dromara.DromaraServletInitializer` | `cn.com.nla.NlaServletInitializer` |
| `org.dromara.web.controller.*` | `cn.com.nla.web.controller.*` |
| `org.dromara.common.core.*` | `cn.com.nla.common.core.*` |
| `org.dromara.system.*` | `cn.com.nla.system.*` |
| `logback-plus.xml` | `logback-nla.xml` |
| `script/sql/ry_vue.sql` | `script/sql/nla_system.sql` |
| `script/sql/ry_job.sql` | `script/sql/nla_job.sql` |
| `script/sql/ry_workflow.sql` | `script/sql/nla_workflow.sql` |
| `script/sql/ry_ai.sql` | `script/sql/nla_ai.sql` |
| `script/bin/ry.sh` / `ry.bat` | `script/bin/nla.sh` / `nla.bat` |
| `.run/ruoyi-*.run.xml` | `.run/nla-*.run.xml` |
| `banner.txt` 内 ASCII art | 重绘为 NLA Cloud 字样 |
| `i18n/messages*.properties` 内 RuoYi 字样 | 替换为 NLA 字样 |

多方言 SQL 同样按 `ry_*` → `nla_*` 改名，目录名 `oracle/`、`postgres/`、`sqlserver/` 保留。

### 1.4 保持不变的项

- **第三方 Maven 坐标**：`sa-token`、`hutool`、`redisson`、`mybatis-plus`、`mybatis-plus-join`、
  `warm-flow`、`snailjob`、`fesod`、`springdoc`、`sms4j`、`aws-sdk`、`mapstruct-plus`、
  `therapi-javadoc`、`justauth`、`mica-mqtt`、`easy-es`。均为外部依赖，不改名。
- **`org.dromara.*` 第三方包名**：`warm`、`sms4j`、`mica-mqtt`、`mica`、`easy-es`、`easyes`
  六个前缀在替换时必须被保护，不能被误改成 `cn.com.nla`。
- **表名前缀 `sys_*`**：行业通用前缀，沿用。
- **业务表语义前缀**：`fs_*`（呼叫中心 41 表）、`oa_*`、`face_*`、`video_*`、`sms_*` 沿用；
  `bean_*` 并入 `sys_*`（见 6.1）。
- **`com.seeta.sdk`** 内嵌源码包名：JNI 签名绑定，改动会破坏与 native 库的绑定，不在去品牌范围内。
- **`ip2region_v4.xdb`**：第三方数据文件，原名保留。

### 1.5 索引前缀的兼容约束

索引前缀 `idx_` / `un_` / `uni_idx_` / `fk_` **必须沿用** —— 全局异常处理器把
`DuplicateKeyException` 转中文提示时，依赖对这些前缀的正则解析，并反射读取实体上的
`@TableField` / `@ApiModelProperty` 注解。新工程 `GlobalExceptionHandler` 需保留该逻辑，
实体字段必须显式写 `@TableField(value="下划线列名")`。

---

## 2. 模块清单与规模

### 2.1 reactor 构建顺序（40 模块，权威清单）

`mvn -B -DskipTests clean install` 实测输出，**BUILD SUCCESS 40/40**：

| # | 模块 | 类型 | Java 文件 | 说明 |
|---|---|---|---|---|
| 1 | `NLA Cloud` | pom | — | 根 POM |
| 2 | `nla-common` | pom | — | common 聚合 |
| 3 | `nla-common-core` | jar | 77 | 核心工具/常量/异常/响应 |
| 4 | `nla-common-doc` | jar | 9 | SpringDoc + therapi |
| 5 | `nla-common-json` | jar | 19 | Jackson 增强 |
| 6 | `nla-common-redis` | jar | 24 | Redisson |
| 7 | `nla-common-social` | jar | 7 | justauth 社交登录 |
| 8 | `nla-common-mail` | jar | 4 | 邮件 |
| 9 | `nla-common-mcp` | jar | 5 | MCP 协议 |
| 10 | `nla-api` | jar | 34 | 跨模块契约 |
| 11 | `nla-common-satoken` | jar | 6 | Sa-Token 认证 |
| 12 | `nla-common-mybatis` | jar | 41 | MyBatis-Plus + 分页 + 多租户 |
| 13 | `nla-common-translation` | jar | 14 | 字段翻译 |
| 14 | `nla-common-oss` | jar | 19 | AWS S3 协议对象存储 |
| 15 | `nla-common-log` | jar | 8 | 操作日志切面 |
| 16 | `nla-common-excel` | jar | 25 | fesod 导入导出 |
| 17 | `nla-common-sms` | jar | 4 | sms4j 短信 |
| 18 | `nla-common-security` | jar | 4 | 权限注解 |
| 19 | `nla-common-web` | jar | 19 | Web 基座/全局异常 |
| 20 | `nla-common-sensitive` | jar | 6 | 数据脱敏 |
| 21 | `nla-common-encrypt` | jar | 27 | 字段加密 |
| 22 | `nla-common-push` | jar | 18 | 消息推送 |
| 23 | `nla-modules` | pom | — | 业务模块聚合 |
| 24 | `nla-system` | jar | 248 | 系统管理（用户/角色/菜单/字典/租户） |
| 25 | `nla-common-job` | jar | 2 | SnailJob 客户端 |
| 26 | `nla-job` | jar | 11 | 定时任务业务 |
| 27 | `nla-common-ai` | jar | 2 | Spring AI 基座 |
| 28 | `nla-ai` | jar | 1 | AI 业务 |
| 29 | `nla-common-elasticsearch` | jar | 3 | Easy-ES |
| 30 | `nla-common-mqtt` | jar | 4 | mica-mqtt |
| 31 | `nla-demo` | jar | 68 | 示例模块 |
| 32 | `nla-common-liteflow` | jar | 10 | 规则编排 |
| 33 | `nla-workflow` | jar | 128 | WarmFlow 工作流 |
| 34 | `nla-gen` | jar | 13 | 代码生成 |
| 35 | `nla-admin` | jar | 17 | **启动入口** |
| 36 | `nla-common-bom` | pom | 0 | common 坐标清单 |
| 37 | `nla-extend` | pom | — | extend 聚合 |
| 38 | `nla-monitor-admin` | jar | 3 | Spring Boot Admin 服务端 |
| 39 | `nla-snailai-server` | jar | 3 | SnailAI 服务端 |
| 40 | `nla-snailjob-server` | jar | 3 | SnailJob 服务端 |

合计 **34 个 jar/pom 子模块** + 6 个聚合/根 POM = 40 个 reactor 条目。
Java 文件总计 **934**（不含旧 `spring-boot-*` 目录）。

### 2.2 旧工程规模（迁移源头，待清理）

| 旧目录 | 子模块数 | Java 文件 |
|---|---|---|
| `spring-boot-business` | 7 | 586 |
| `spring-boot-client` | 2 | 136 |
| `spring-boot-common` | 2 | 218 |
| `spring-boot-feign` | 7 | 43 |
| `spring-boot-service` | 4 | 72 |
| `spring-boot-starter` | 21 | 744 |
| `spring-boot-system` | 3 | 48 |
| **合计** | **46** | **1847** |

### 2.3 `nla-admin` 资源文件（9 份，与上游文件数一致）

```
application.yml            application-dev.yml        application-prod.yml
banner.txt                 logback-nla.xml            ip2region_v4.xdb
i18n/messages.properties   i18n/messages_en_US.properties   i18n/messages_zh_CN.properties
```

上游无 `application-local.yml`，本工程也不建。

---

## 3. 阶段 2.1 引入取舍（已被推翻）

> **本节记录一次决策变更，务必先读。**

原方案 2.1 写的是"按需引入，禁止引全量"，明确排除：

- common 子模块只引入 19 个，**不引入** `ai` / `mcp` / `mqtt` / `liteflow` / `mail` / `social`
- modules **不建** `nla-demo`、`nla-ai`
- extend **不建** `nla-snailai-server`
- `script/sql/ry_ai.sql` **不引入**

**用户后续指令（原文）：**

> 将若依里所有模块引入，不要漏

该指令推翻了上述全部取舍。现执行结果为**与上游 100% 模块对齐**：

| 层级 | 原计划 | 实际执行 |
|---|---|---|
| common 子模块 | 19 | **25**（补 `ai`/`mcp`/`mqtt`/`liteflow`/`mail`/`social`） |
| modules | 4 | **6**（补 `nla-demo`/`nla-ai`） |
| extend | 2 | **3**（补 `nla-snailai-server`） |
| SQL 文件 | 3 | **14**（补 `nla_ai.sql` + oracle/postgres/sqlserver 三方言目录） |

连带影响（都已完成）：

1. 根 POM 补 4 个版本属性 + 6 个 `dependencyManagement` 块
2. 4 个聚合 POM 补 `<module>`，`nla-common-bom` 补 4 条坐标声明
3. `nla-admin/pom.xml` 恢复 `nla-common-social` / `nla-common-mail` / `nla-common-mcp` /
   `nla-ai` / `nla-demo` 依赖
4. `application*.yml` 还原 `justauth` / `mqtt` / `ai` / `mcp` 配置段
5. `nla_system.sql` 还原 40 行菜单数据（AI 会话 / AI 控制台 / demo 1500~1511 / 24 条 role_menu）
6. docker-compose 补 `nla-snailai-server` 服务块 + 2 处 `SNAIL_AI_PORT`
7. nginx.conf 补 `upstream snailai-server` + `location /snail-ai/`（SSE/WebSocket 代理）
8. `.run/` 补 `nla-snailai-server.run.xml`

**注意**：这些模块目前是**基线原样引入**，尚未接入本工程的 9 大业务域。
阶段 3~9 推进时，若某个基线模块确认与业务无关，可再评估摘除 —— 但摘除属于新的决策，需重新确认。

---

## 4. 基座类映射（旧 → 新）

旧根包 `cn.com.tzy.springbootcomm`，新根包 `cn.com.nla.common.*`。

| 当前 | 目标 | 说明 |
|---|---|---|
| `common.vo.RestResult` | `core.domain.R` | 成功码 `0`→`200`；`result(code,msg,data)` → `ok()/fail()` |
| `common.vo.RespCode` | `core.constant.HttpStatus` | `CODE_0`→`SUCCESS(200)`；`CODE_2`→`fail(msg)`；310~319 认证码并入 Sa-Token 异常体系；101~105 限流码随 Sentinel 废弃 |
| `common.vo.PageResult` | `core.domain.PageResult` | **同名类**，仅换包与内部结构；配合 `mybatis.core.page.PageQuery`；`MyBatisUtils.selectPage(page)` → `PageQuery.build()` |
| `common.bean.Base` | `mybatis.core.domain.BaseEntity` | `create_user_id`→`create_by`、`update_user_id`→`update_by`，新增 `create_dept` |
| `common.bean.LongIdEntity` | 实体直接继承 `BaseEntity` + `@TableId` | 主键 `IdType.AUTO` → 雪花 `ASSIGN_ID` |
| `common.bean.IntIdEntity` / `StringIdEntity` | 同上，按需保留 | 逐个核对实际使用点 |
| `common.bean.TreeNode` | `core.domain.TreeEntity` 或 `TreeUtil` | 对齐基线树构建 |
| `common.model.BaseModel` 校验分组 | `core.validate.AddGroup/EditGroup` | 小写分组名 `add/edit/page` → 基线分组接口 |
| `common.model.PageModel/PageSortModel` | `mybatis.core.page.PageQuery` | |
| `excption.RespException` | `core.exception.ServiceException` | 旧拼写 `excption` 不再沿用 |
| `excption.BizException/ParamException/UserException/JwtException/TimingException` | 合并入 `ServiceException` 或 `core.exception.*` | `JwtException` 随 OAuth2 废弃 |
| `constant.Constant` | 拆分至 `core.constant.*`（`Constants`/`UserConstants`/`CacheNames`） | `DATE_TIME_FORMAT` 等硬格式串随 `LocalDateTime` 取消 |
| `constant.NotNullMap` | 保留为 `core.utils.NotNullMap` | 43 处使用，暂不强制改 Vo |
| `constant.ImgConstant` | `core.constant` 或对应业务模块常量 | |
| `utils.AppUtils` | 拆分至 `core.utils.*` | `getMobileMask()` → `nla-common-sensitive` 脱敏注解 |
| `utils.JwtUtils` / `common.jwt.JwtCommon` | **废弃** | 改 Sa-Token |
| `utils.CollectionUtils` | 换 Hutool `CollUtil` | 去重 |
| `utils.ValidatorUtils` | 换 `jakarta.validation` + `core.utils.ValidatorUtils` | |
| `utils.DateEditor` | **废弃** | `LocalDateTime` 无需 |
| `utils.DynamicTask` | `nla-common-job`（SnailJob） | |
| `utils.ChangeChar` / `DesensitizedFormatterUtils` | `core.utils` / `nla-common-sensitive` | |
| `annotation.Desensitized` + `common.enumcom.SensitiveTypeEnum` | `nla-common-sensitive` 的 `@Sensitive` + `SensitiveStrategy` | |
| `common.enumcom.ConstEnum` | 拆分至 `core.enums.*` | 静态内部枚举写法按基线风格展开 |
| `common.enumcom.OAuthClientEnum` | `system.api.model.LoginUser` + `sys_client` 表 | 基线客户端管理 |
| `common.mq.MqConstant` | 自建 `nla-common-mq`（阶段 3） | |
| `spring.SpringContextHolder` | `core.utils.SpringUtils` | |
| `spring.AssertUtils` | Hutool `Assert` 或 `core.utils` | |
| `interfaces.SFunction` | MyBatis-Plus 自带 `SFunction` | 去重 |
| `common.info.SecurityBaseUser/UserPayload` | `system.api.model.LoginUser` | |
| `common.info.VueRoutes` | `system.domain.vo.RouterVo` | |
| `common.info.AreaInfo` | 业务模块 Vo | |

### 4.1 全工程级批量替换（量化改造面）

| 改造项 | 影响文件数 | 动作 | 状态 |
|---|---|---|---|
| `javax.*` → `jakarta.*` | 414 | 批量替换 | 待阶段 4/6 |
| `RestResult` → `R<T>` | 390 | 响应封装替换 + code 语义变更 | 待阶段 6 |
| `@Log4j2` → `@Slf4j` | 285 | 注解替换 | 待阶段 6 |
| Swagger2 注解 → JavaDoc | 200 | 删注解，改 SpringDoc | 待阶段 4 |
| `PageResult` 换包 + 构造方式 | 197 | 分页封装替换 | 待阶段 6 |
| `@Autowired` → 构造器注入 | 125 | 改 `@RequiredArgsConstructor` | 待阶段 6 |
| `extends ApiController` → `BaseController` | 115 | 基类替换 | 待阶段 6 |
| `java.util.Date` → `LocalDateTime` | 98 | 时间类型 + 去双注解 | 待阶段 4 |
| `extends ServiceImpl` → 纯接口实现 | 82 | Service 层重构 | 待阶段 6 |
| 租户字段适配 | 65 | 对齐基线租户机制 | 待阶段 5 |
| Spring Security → Sa-Token | 51 | 认证重写 | 待阶段 5 |
| `*Param` → `*Bo` | 57 类 | 重命名 + 校验分组改法 | 待阶段 4 |
| `@FeignClient` → 本地调用 | 43 | 微服务合并 | 待阶段 8 |
| MapStruct → MapStruct-Plus | 35 | 改 `@AutoMapper`，删 `*Convert` | 待阶段 4 |
| Activiti → WarmFlow | 14 | **重写，非迁移** | 待阶段 7 |
| Seata `@GlobalTransactional` | 7 | 废弃，改本地事务 | 待阶段 6 |

类清单：Controller 132 / Service 189 / ServiceImpl 113 / Mapper 84 / Convert 35 / Param 57 / Vo 45。

### 4.2 基线实测校正（计划与实际不符之处）

以下两项是**照实际代码核实后**发现的，与原方案的假设不同，阶段 4/6 开工前必读。

#### 4.2.1 分页类：`TableDataInfo` 在 6.X 已不存在

原方案写的是 `PageResult` → `TableDataInfo`（197 文件）。实测：

- 全工程 **无** `TableDataInfo.java`
- 基线用的是 `cn.com.nla.common.core.domain.PageResult<T>`

```java
@Data
@NoArgsConstructor
public class PageResult<T> implements Serializable {
    private long total;              // 总记录数
    private Collection<T> rows;      // 列表数据

    public static <T> PageResult<T> build(Collection<T> list, long total) { ... }
    public static <T> PageResult<T> build(Collection<T> list) { ... }  // total = list.size()
    public static <T> PageResult<T> build() { ... }                    // 空对象
}
```

**对迁移是好消息**：旧工程的 `cn.com.tzy.springbootcomm.common.vo.PageResult`
与新基线**同名**，197 处调用点只需改 import 与构造方式，不需改类型名。
仍需核对的是字段名与 JSON 结构是否与旧版一致（影响前端契约）。

`PageQuery` 在 `cn.com.nla.common.mybatis.core.page`，`R<T>` 在 `cn.com.nla.common.core.domain`。

#### 4.2.2 代码生成模板：`fm/` 而非 `vm/`

`.qoder/rules/backend-crud.md`（拷自上游 agent 文档）写的是
`ruoyi-gen/src/main/resources/vm/`。实测本工程与上游都是：

```
nla-modules/nla-gen/src/main/resources/fm/    ← FreeMarker 模板（.ftl）共 20 份
    java/    bo / controller / domain / mapper / service / serviceImpl / vo
    vue/     api.ts / index.vue / index-tree.vue / types.ts
    react/   api.ts / index.tsx / index-tree.tsx / types.ts
    sql/     mysql / oracle / postgres / sqlserver
    xml/     mapper.xml
```

`vm/` 是 5.X 时代的 Velocity 模板目录，6.X 已换 FreeMarker，上游自己的文档没跟上。
阶段 6 用生成器产出骨架时，改模板要改 `fm/*.ftl`。

### 4.3 逻辑删除：与旧铁律直接冲突的基线变更

> **这是本次迁移影响面最大的单项架构变更，阶段 4 与 6 开工前必读。**

旧铁律（`.qoder/rules/project-baseline.md` 第 7 节）写的是：

> **无逻辑删除**（全项目 0 处 `@TableLogic`/`del_flag`）→ 物理删除，**删除前必须做引用校验**

新基线恰好相反，逻辑删除是**全局开启**的：

- `nla-admin/src/main/resources/application.yml`：`mybatis-plus.enableLogicDelete: true`
- 已确认带 `@TableLogic` 的实体（不完全列举）：

| 实体 | 模块 | `delFlag` 类型 |
|---|---|---|
| `SysUser` | `nla-system` | `String` |
| `SysRole` | `nla-system` | `String` |
| `SysDept` | `nla-system` | `String` |
| `SysPost` | `nla-system` | `String` |
| `SysClient` | `nla-system` | `String` |
| `FlowCategory` | `nla-workflow` | `String` |
| `FlowInstanceBizExt` | `nla-workflow` | `String` |
| `TestDemo` | `nla-demo` | **`Long`** |
| `TestTree` | `nla-demo` | **`Long`** |

> 注意基线内部就不统一：`nla-system` / `nla-workflow` 用 `String`，`nla-demo` 用 `Long`。
> **本工程统一采用 `String delFlag`**（跟系统表一致，demo 仅示例），阶段 4 建表时 `del_flag` 用 `char(1)`。

- `nla-gen` 的 `GenConstants` 已把 `del_flag` 从生成范围排除：
  `COLUMNNAME_NOT_ADD` / `NOT_EDIT` / `NOT_LIST` / `NOT_QUERY` 均含 `del_flag`
  —— 说明代码生成器把它归为框架接管字段，不会出现在表单与列表里。

#### 对后续阶段的具体影响

| 阶段 | 影响 |
|---|---|
| **4.3 DDL 重写** | 所有业务表需新增 `del_flag char(1) default '0'` 列；存量数据迁移脚本需把已有行全部置 `'0'` |
| **6 业务迁移** | 旧代码的 `removeById` 是**物理删除**，迁移后自动变成 `update del_flag`。原本靠"删除前引用校验"保证的完整性，现在语义变了：被引用的行只是标记删除，仍占着唯一索引 |
| **6 业务迁移** | 旧代码里手写的 `LambdaQueryWrapper` 若显式指定了 select 列或用了原生 SQL，**不会自动拼 `del_flag = '0'`**，需逐个排查 |
| **唯一索引** | 旧铁律要求索引前缀 `un_` / `uni_idx_`；逻辑删除后"删除再新建同名"会撞唯一键，需评估是否把 `del_flag` 纳入联合唯一索引 |
| **铁律文档** | `.qoder/rules/project-baseline.md` 第 7 节与第 9 节禁止清单需同步更新（同 5.2 鉴权变更的处理方式） |

> 旧铁律的"删除前引用校验"**不应丢弃**：即使改为逻辑删除，业务上的引用完整性校验
> （如"请先删除子部门"）仍是必要的用户体验，只是底层从 `delete` 变成了 `update`。

---

## 5. starter → nla-common 映射

旧 `spring-boot-starter` 21 个子模块共 744 Java 文件。

| 当前 starter | 目标 | 处理 | 状态 |
|---|---|---|---|
| `starter-redis`(6) | `nla-common-redis` | 采用基线 Redisson 实现；`RedisUtils` API 对齐，138 处调用点改造 | 基座已就位，调用点待改 |
| `starter-mybatis`(15) | `nla-common-mybatis` | 采用基线实现；`MyBatisUtils.selectPage` → `PageQuery.build()`；`LambdaQueryWrapperX`/`QueryWrapperX` 评估是否保留 | 基座已就位 |
| `starter-swagger`(5) | `nla-common-doc` | springfox 2.9.2 → SpringDoc + therapi JavaDoc | 基座已就位 |
| `starter-logs`(7) | `nla-common-log` | `@ApiLog`+`LogsAspect` → `@Log`+基线日志切面；`sys_logs` → `sys_oper_log` | 基座已就位 |
| `starter-minio`(3) | `nla-common-oss` | MinIO 8.0.3 直连 → AWS S3 协议，`MinioUtils` 调用点改造 | 基座已就位 |
| `starter-sms`(22) | `nla-common-sms` | 自建多渠道 → sms4j；阿里/腾讯/容联云 SDK 走 sms4j 适配 | 基座已就位 |
| `starter-autopoi`(9) | `nla-common-excel` | poi 3.15 + autopoi → fesod | 基座已就位 |
| `starter-security-oauth`(54) | `nla-common-satoken` + `nla-common-security` | **重写**，见阶段 5 | 基座已就位，策略待迁 |
| `starter-xxl-job`(2) | `nla-common-job` | XXL-JOB 2.3.1 → SnailJob | 基座已就位 |
| `starter-elasticsearch` | `nla-common-elasticsearch` | Easy-ES 1.1.1 → 3.0.2，7 处调用需改造 | 基座已就位 |
| `starter-cloud`(5) | **废弃** | `ApiController` 全局异常处理 → 基线 `GlobalExceptionHandler`（需保留索引前缀正则解析，见 1.5） | 待阶段 6 |
| `starter-feign`(2) / `starter-nacos`(2) | **废弃** | 单体化 | 待阶段 8 |
| `starter-sentinel`(5) | **废弃** | 限流改基线 `@RateLimiter`（Redisson 实现） | 待阶段 6 |
| `starter-quartz`(12) | **废弃** | 已弃用 | 待阶段 7 |
| `starter-netty`(19) / `starter-socket-io`(11) | 自建 `nla-common-socketio` | netty-socketio 1.7.19 升级到 JDK 21 兼容版 | 待阶段 3 |
| `starter-rabbitmq`(2) | 自建 `nla-common-mq` | spring-amqp 走 Boot 4 版本；`MQConfig` 迁移 | 待阶段 3 |
| `starter-freeswitch`(199) | 自建 `nla-common-freeswitch` | 见 5.1 | 待阶段 3 |
| `starter-video`(201) | 自建 `nla-common-gb28181` | 见 5.1 | 待阶段 3 |
| `starter-pay`(163, 8 渠道) | 自建 `nla-common-pay` | 见 5.1 | 待阶段 3 |

### 5.1 阶段 3 待建的 6 个自建封装

坐标 `cn.com.nla:nla-common-{tech}`，全部需登记进 `nla-common-bom`。
每个遵循 `basic`（注解/枚举/POJO，无 Spring 依赖）+ `core`（配置/AOP/实现）二段式。

| 模块 | 源 | 关键风险 |
|---|---|---|
| `nla-common-freeswitch` | `starter-freeswitch`(199) | `freeswitch-esl 1.6.7.RELEASE` 需验证 JDK 21；铁律沿用：管理接口禁止执行 FreeSWITCH CLI / 系统命令 / 文件删除 |
| `nla-common-gb28181` | `starter-video`(201) | `sip 1.3.0-91`、`onvif 1.0.2`、`netty` 需升级到 Boot 4 兼容版；SIP 栈在 JDK 21 需实测注册/心跳/点播链路 |
| `nla-common-pay` | `starter-pay`(163) | `alipay-easysdk`、`yungouos-pay-sdk`、`wx.binarywang`、`weixin-popular` 逐个升级验证；**金额字段约定项目内缺失**，不臆造精度方案 |
| `nla-common-facesdk` | `spring-boot-face/.../com/seeta/sdk` 内嵌源码 | JNI `.so`(12) + `.csta` 模型路径外置；包名保持 `com.seeta.sdk`；基础镜像 `seetaface_face_work` 需重建 JDK 21 版；`LoadNativeCore.LOAD_NATIVE` 加载路径需实测 |
| `nla-common-socketio` | `starter-socket-io` + `starter-netty` | netty-socketio JDK 21 兼容性 |
| `nla-common-mq` | `starter-rabbitmq` + `MqConstant` | spring-amqp Boot 4 版本 |

---

## 6. 业务模块映射

| 批次 | 源模块（文件数） | 目标模块 | 要点 | 状态 |
|---|---|---|---|---|
| 6.1 | `bean`(98) + `sys` 实体 | 合并进 `nla-modules/nla-system` | 表映射见下 | 待推进 |
| 6.2 | `oa`(6) + `Leave` | `nla-modules/nla-oa` | 最轻，作为迁移范式验证 | 待推进 |
| 6.3 | `sms`(47) | `nla-modules/nla-message` | `MobileMessage`/`MobileMessageTemplate`/`PublicNotice`/`ReadNoticeUser`/`SmsConfig`；短信走 `nla-common-sms`；`Quartz` 实体废弃 | 待推进 |
| 6.4 | `face`(116) | `nla-modules/nla-face` | 18 Pool + 18 Proxy 对象池包装 JNI，依赖 `nla-common-facesdk` | 待推进 |
| 6.5 | `video`(80) | `nla-modules/nla-video` | GB28181 设备/通道/录像/云台，依赖 `nla-common-gb28181`；`video.MediaServer` 与 `fs.MediaServer` **同名不同表**，需消歧 | 待推进 |
| 6.6 | `fs`(196, 41 表) | `nla-modules/nla-callcenter` | 最大业务模块；沿用 `service/manage/{group}` 纯接口 + 构造器注入（与基线风格天然一致）；`*SaveParam`→`*Bo` | 待推进 |
| 6.7 | `pay`(27) | `nla-modules/nla-pay` | 依赖 `nla-common-pay`；保持"暂未开发"现状 | 待推进 |

### 6.1 `bean_*` → `sys_*` 表映射

| 旧表 | 新表 |
|---|---|
| `bean_user` | `sys_user` |
| `bean_department` | `sys_dept` |
| `bean_role` | `sys_role` |
| `bean_menu` | `sys_menu` |
| `bean_position` | `sys_post` |
| `sys_dictionary_*` | `sys_dict_*` |
| `bean_*_connect_*` | `sys_user_role` / `sys_role_menu` / `sys_user_post` / `sys_role_dept` |

bean 独有表**保留为业务表**，不并入 `sys_*`：`bean_user_set`、`bean_mini`、`bean_mini_user`、`sys_area`。

### 6.2 URL 与 action 词表映射

旧：`/api/{module}/{resource}/{action}`、`/webapi/**`、`/app/**`
新：基线风格 `/{module}/{resource}` + RESTful 动词

| 旧 action | 新惯例 |
|---|---|
| `page` | `list` |
| `detail` | `getInfo` |
| `save` | `add` / `edit` |
| `remove` | `remove` |
| `select` / `tree` / `all` / `status` | 逐一登记（迁移时补全本表） |

### 6.3 每个业务模块的统一改造清单

- `Controller`：`extends ApiController` → `extends BaseController`；
  `@RestController("Api{Module}{Resource}Controller")` 显式 Bean 名**沿用**
  （单体化后 Bean 名冲突风险上升，更需要唯一名）；返回 `RestResult` → `R<T>`
- `Service`：`extends IService<E>` → `IXxxService`（I 前缀）；
  `ServiceImpl<M,E>` → 纯实现 + `@RequiredArgsConstructor`
- `@Log4j2` → `@Slf4j`；`@ApiLog(type=LogsTypeEnum.XXX)` → `@Log(title=, businessType=)`
- **沿用不变的铁律**：
  - 缓存刷新 `@TransactionalEventListener(AFTER_COMMIT)` + 定向刷新 + 禁 `delAll()`
  - 写操作 `@Transactional(rollbackFor = Exception.class)`
  - 物理删除前引用校验（`return R.fail("请先删除xxx")`）
  - 无逻辑删除、不建物理外键
  - 密码/密钥只写不读；手机号脱敏

---

## 7. 有意偏离上游的清单

**本节是最重要的部分。** 上游文件一律"从上游读取 → 过替换链 → 套用下列规则 → 校验命中数 → 写盘"，
不手工打补丁。任何一条规则的命中数低于 `min` 就 `throw`，宁可失败也不静默产出未去品牌的文件。

替换链顺序不可变：`protect（占位符）→ own（org.dromara→cn.com.nla）→ brand（RuoYi/dromara→NLA/nla）→ restore（还原占位符）`。

### 7.1 SQL（14 份，8 条偏离规则）

`script/sql/` 下 4 方言共 14 份，全部由 `.migration/regen-sql.ps1` 生成。

| # | 规则 | 替换 | min | 理由 |
|---|---|---|---|---|
| 1 | `疯狂的狮子Li` | → `NLA管理员` | 4 | `sys_user` 中 admin 的昵称是上游作者品牌 |
| 2 | `crazyLionLi@163\.com` | → `admin@nla.com.cn` | 4 | 同上，admin 邮箱 |
| 3 | `(?m)^.*PLUS官网.*\r?\n` | → 删除整行 | 4 | `sys_menu` 中 `1761400000000000004` 是纯品牌外链菜单，无任何模块依赖 |
| 4 | `gitee\.com/nla/warm-flow` | → `gitee.com/dromara/warm-flow` | 3 | **还原第三方真实溯源地址**；warm-flow 是第三方项目，改写后 URL 会指向不存在的仓库 |
| 5 | `postgres_ry_ai\.sql` | → `postgres_nla_ai.sql` | 1 | AI 库自带用法注释引用自己的上游文件名 |
| 6 | `< ry_ai\.sql` | → `< nla_ai.sql` | 1 | 同上（MySQL 版） |
| 7 | `若依新版本发布啦` | → `新版本发布啦` | 1 | **修上游自身不一致**：`sqlserver_ry_vue.sql` 的 `sys_notice` 种子行仍带品牌，而 MySQL/Oracle/Postgres 三方言已清理 |
| 8 | `若依系统凌晨维护` | → `系统凌晨维护` | 1 | 同上 |

规则 3 的效果：`nla_system.sql` 相比上游少 1 行（934 vs 935），其余方言同理各少 1 行。
`nla_job.sql` / `oracle_nla_job.sql` / `postgres_nla_job.sql` / `postgres_nla_workflow.sql` /
`sqlserver_nla_job.sql` 五份与上游**逐字节相同**（不含任何品牌字样）。

**已还原的菜单数据**（阶段 3 补齐，共 40 行）：

- `1761400000000000008` AI 会话（`ai/chat/index`）
- `1761400000000000121` AI 控制台（`monitor/snailai/index`，perms `monitor:snailai:list`）
- `1761400000000001500`~`1511` demo 测试单表/树表的 12 条菜单与按钮权限
- 24 条 `sys_role_menu`（角色 `1761300000000000003` / `1761300000000000004` 各 12 条）

### 7.2 `nla-admin` 配置（yml / banner / logback）

`application.yml`：

| 上游 | 本工程 | 理由 |
|---|---|---|
| `config: classpath:logback-plus.xml` | `classpath:logback-nla.xml` | 命名基线 1.3 |
| `name: Lion Li` | `name: TZY` | 作者基线 1.1 |
| `email: crazylionli@163.com` | **整行删除** | 上游作者私人邮箱 |
| `url: https://gitee.com/ruoyi/RuoYi-Vue-Plus` | **整行删除** | SpringDoc contact 的上游项目主页 |

`application-dev.yml` / `application-prod.yml`（两文件对称，各 5 类偏离）：

| 上游 | 本工程 | 理由 |
|---|---|---|
| `script/sql/ry_job.sql`（2 处注释） | `script/sql/nla_job.sql` | 命名基线 |
| `# jdbc 所有参数配置参考 https://lionli.blog.csdn.net/article/details/122018562` | `# jdbc 参数说明：` | 上游作者个人博客外链 |
| `url: jdbc:mysql://localhost:3306/ry-vue?...`（生效 + 注释各 1 处） | `.../nla-cloud?...` | 数据库改名 |
| `clientName: RuoYi-Vue-Plus` | `clientName: nla-cloud` | SnailJob 客户端名 |
| `# 前端改动 https://gitee.com/JavaLionLi/plus-ui/pulls/204` | `# 前端需自行实现 gitea 授权回调页` | 上游前端仓库 PR 外链，本工程前端另立任务线 |

`logback-nla.xml`：规范化后与上游 `logback-plus.xml` **完全一致**，只改了文件名。

`banner.txt`：ASCII art 整体重绘为 NLA Cloud 字样。
（`nla-extend` 三个模块的 `banner.txt` 与上游逐字节相同 —— 那是上游自己画的图案，非去品牌损伤。）

### 7.3 Docker 与运行配置（9 条规则）

由 `.migration/regen-support-cfg.ps1` 生成，覆盖 `script/docker/docker-compose.yml`、
`script/docker/nginx/conf/nginx.conf`、4 个 `.run/*.run.xml`。

| 组 | 规则 | 替换 | min |
|---|---|---|---|
| 镜像 tag | `image: nla/nla-server:6.0.0` | → `:1.0.0` | 2 |
| | `image: nla/nla-monitor-admin:6.0.0` | → `:1.0.0` | 1 |
| | `image: nla/nla-snailjob-server:6.0.0` | → `:1.0.0` | 1 |
| | `image: nla/nla-snailai-server:6.0.0` | → `:1.0.0` | 1 |
| 数据库 | `MYSQL_DATABASE: ry-vue` | → `nla-cloud` | 1 |
| `.run` | `value="nla/nla-server:6.0.0"` | → `:1.0.0` | 1 |
| | `value="nla/nla-monitor-admin:6.0.0"` | → `:1.0.0` | 1 |
| | `value="nla/nla-snailjob-server:6.0.0"` | → `:1.0.0` | 1 |
| | `value="nla/nla-snailai-server:6.0.0"` | → `:1.0.0` | 1 |

`.run/` 目录：上游 4 个 `ruoyi-*.run.xml` → 本工程 4 个 `nla-*.run.xml`。
两者都是 `docker-deploy` 类型 + `buildOnly=true`，即**只构建镜像、不推送也不启动容器**，
产出的 tag 与 `docker-compose.yml` 的 `image:` 逐一对应。
旧工程遗留的 11 个 `spring-boot-*.run.xml` 已全部删除，`ours-only = 0`。
（旧的 11 份是**完整部署配置**：带 `containerName`、`--spring.profiles.active=prod`、
`NACOS_SERVER_IP` 等环境变量，镜像推到腾讯云仓库 —— 与新配置的语义不同，不要照搬。）

### 7.3.1 `script/docker/` 实际组成（易混淆，特此记录）

两个 compose 文件职责**完全不同**，不要当成"基础设施 + 应用"的两阶段编排：

| 文件 | 内容 |
|---|---|
| `docker-compose.yml` | **主编排**，9 个服务：`mysql:8.4.9`、`redis:8.6.3`、`minio`、`nginx-web:1.31.1`、`nla-server1` + `nla-server2`（主服务**双实例**，nginx 负载）、`nla-monitor-admin`、`nla-snailjob-server`、`nla-snailai-server` |
| `database.yml` | **备选数据库方言**，4 个容器：Oracle 12c、SQLServer 2017、PostgreSQL 14.2、PostgreSQL 13.6。供不用 MySQL 的部署按需启动，对应 `script/sql/` 的同名方言子目录 |

主服务镜像名是 **`nla-server`** 而非 `nla-admin` —— 由 `nla-admin` 模块打包产出，
这也是规则组里 `image tag nla-server` 的 `min` 为 2 的原因（双实例各一处）。

### 7.4 Java 源码（2 处）

| 文件 | 上游 | 本工程 | 理由 |
|---|---|---|---|
| `nla-modules/nla-gen/.../util/GenUtils.java` | `RegExUtils.replaceAll(text, "(?:表\|若依)", "")` | `"(?:表\|NLA)"` | `replaceText` 用于从表注释剔除噪音词生成代码功能名，剔除的是本工程品牌词 |
| `nla-modules/nla-demo/.../controller/TestExcelController.java` | `map.put("author", "Lion Li")` | `"TZY"` | 填充 `excel/多列表.xlsx` 模板的 `{author}` 占位符 |

### 7.5 明确不引入的上游内容

| 上游路径 | 文件数 | 理由 |
|---|---|---|
| `.gitee/PULL_REQUEST_TEMPLATE.zh-CN.md`、`.gitee/ISSUE_TEMPLATE/{bug,config,feature}.yml` | 4 | 上游自己 gitee 仓库的托管平台元数据，引入后 issue/PR 会指向 dromara 的仓库 |
| `.claude/agents/backend-*.md` | 6 | **已等价落地**为 `.qoder/rules/backend-*.md`，文件名逐一对应：`backend-common-infrastructure` / `backend-crud` / `backend-engineering` / `backend-javadoc` / `backend-module-enhancement` / `backend-query-permission` |
| `sys_menu` 中 `1761400000000000004`（PLUS官网） | 1 行 | 见 7.1 规则 3 |
| 上游 `.flattened-pom.xml` | 各模块 | 构建产物，已 gitignore，且本工程 revision 不同 |

### 7.6 品牌审计白名单

`.migration/audit-brand.ps1` 当前基线：
**`scanning 998 files` / `scanned(text)=988  hits=0  allowlisted=7  file-exceptions=6`**

> 基线曾是 `scanned=985 / allowlisted=6`，差的 3 份是一个**已修复的真实盲点**：
> 脚本按扩展名过滤文本文件，而 `Path.GetExtension('.gitignore')` 返回的是
> `.gitignore` 而非空串，导致 `.gitignore` / `.editorconfig` / `.gitattributes`
> 三份 dotfile 从未被扫过（已用 `probe-audit-blindspot.ps1` 量化：在 998 份范围内
> 共 7 份未被扫，其中 4 份是 `.xdb` / `.xlsx` 真二进制，跳过合理）。
> 修复方式：文件名只有一个前导点时按无扩展名文本处理，兼容将来的 `.dockerignore` 等。

审计范围含 `.qoder`：本次提交后其下有 12 份入库文件（6 份 `rules/backend-*.md`
+ 6 份 `skills/ponytail*/SKILL.md`），属交付物。其中 6 份 ponytail SKILL.md 是在
`.qoder/` 被整体 ignore 之前就提交、靠 git 持续跟踪留下的；6 份规则文档则需靠
上面新的取反规则才能正常入库。**注意 `Grep` 工具尊重 `.gitignore`，对该目录会返回
假阴性的 0 匹配**，必须走 `Get-ChildItem` 的脚本才能扫到 —— 这条教训就是靠它发现
`.qoder/rules/backend-*.md` 里 9 处真实品牌残留的。

行内白名单（`$allow`，2 条）：

| 模式 | 命中数 | 理由 |
|---|---|---|
| `gitee\.com/dromara/warm-flow` | 3 | warm-flow 是第三方开源项目，这 3 处是工作流库结构说明中引用其真实来源地址。改写会指向不存在的仓库 |
| `skills/ruoyi-plus-ai-coding` | 4 | 本地安装的 skill 保留自己的目录名（见下方目录例外），指向其 reference 文档的规则只能照实写；3 处在 `.qoder/rules/backend-*.md`，1 处在 `.gitignore`（就是把它排除入库的那条规则本身） |

文件级例外（`$allowFiles`，1 份 / 计 6 次跳过中的 1 项）：

| 路径 | 理由 |
|---|---|
| `docs/migration/mapping.md` | 迁移台账必须在每一行映射的一侧写明上游产物名，去掉品牌词就失去了存在意义。计划本身也把"docs 中的来源说明"排除在扫查之外 |

目录级例外（`$allowDirs`，1 个）：

| 路径 | 理由 |
|---|---|
| `.qoder/skills/ruoyi-plus-ai-coding` | 该 skill 是本地安装的、**不受 git 跟踪**，名字不会进仓库。改名会弄坏 IDE 的 skill 注册，没有收益 |

`.gitignore` 已把上述意图写明，不再靠 `git add -f` 绕过（否则以后新增规则会被静默忽略）：

```gitignore
.qoder/*                            # 用 .qoder/* 而非 .qoder/，后者会使子目录取反失效
!.qoder/rules/                      # 6 份规范文档，README 直接引用
!.qoder/skills/                     # 已入库的 ponytail 系列
.qoder/skills/ruoyi-plus-ai-coding/  # 本地安装的第三方 skill
```

四个用例已用 `git check-ignore -v --no-index` 逐个验证：`rules/` 与 `skills/ponytail/`
可跟踪，`skills/ruoyi-plus-ai-coding/` 与任意 `.qoder/cache/*` 被忽略。

审计扫描的 11 个模式：`(?i)ruoyi`、`(?i)dromara(?!\.(warm|sms4j|mica|easy))`、`(?i)lion\s?li`、
`(?i)lionli`、`(?i)crazylionli`、`ry-vue`、`\bry_(vue|job|workflow|ai)\b`、`logback-plus`、
`若依`、`Michelle\.Chung`、`(?i)plus-ui`。扫描范围排除 `\target\`。

**任何后续编辑完成后都应重跑该脚本**，`hits` 必须为 0。
脚本内含中文字面量，SearchReplace 会剥掉 BOM，改完需重跑 `add-bom.ps1`。

### 7.7 上游遗留：SQL 排序规则不统一（阶段 4 必须处理）

上游建表脚本在字符集声明上不一致，本工程忠实复制后继承了这个差异：

| 脚本 | `COLLATE` 声明 |
|---|---|
| `nla_system.sql` | **完全不写**，建表时继承库默认值 |
| `nla_job.sql` | 同上 |
| `nla_workflow.sql` | 同上 |
| `nla_ai.sql` | **每张表都显式写** `COLLATE=utf8mb4_unicode_ci` |

后果：若把库建成 `utf8mb4_general_ci`，`nla_ai` 的表与其他三批表排序规则不一致，
跨表 join 时报 `Illegal mix of collations`。

**当前约定**：建库统一用 `utf8mb4` + `utf8mb4_unicode_ci`（已写入 README）。

**阶段 4 重写业务表 DDL 时必须显式写 `COLLATE=utf8mb4_unicode_ci`**，
不要再依赖库默认值 —— 否则部署环境的库字符集一旦不同就会重现这个问题。

---

## 8. 阶段进度台账

| 阶段 | 内容 | 状态 | 备注 |
|---|---|---|---|
| **0** | 前置环境 | ✅ 完成 | JDK 21 已就位（`D:\develop\Jdk\jdk-21.0.12.1`）；Maven `3.9.9` 全路径调用；依赖可拉取（阿里云 + 中央仓库） |
| **1** | 工程骨架 | ✅ 完成 | 根 POM、`nla-common-bom`、全部模块 POM、`NlaApplication`、9 份 admin 资源、`script/`、`.editorconfig`/`.gitattributes`/`mvnw` |
| **2.1** | common 子模块引入 | ✅ 完成 | **25 个全引入**，见第 3 节的取舍推翻记录 |
| **2.2** | 基座类映射落地 | ✅ 完成 | `core`/`mybatis`/`web` 三项基座（`R`、`HttpStatus`、`ServiceException`、`BaseEntity`、`PageQuery`、`PageResult`、`BaseController`、常量与工具类）随基线原样引入 |
| **2.3** | starter 映射 | 🟡 基座就位 | 基线侧的 10 个 `nla-common-*` 已引入；旧 starter 的 744 文件**尚未迁移**，调用点改造未开始 |
| **3** | 自建技术封装 | ⬜ 未开始 | 6 个自建模块（freeswitch/gb28181/pay/facesdk/socketio/mq），约 680 文件 |
| **4** | 数据层重写 | ⬜ 未开始 | 实体 + Bo/Vo + DDL + 91 个 Mapper XML |
| **5** | 认证鉴权与租户 | ⬜ 未开始 | OAuth2 → Sa-Token 重写；网关集中鉴权 → 注解式鉴权（**引入原项目没有的方法级权限注解，属架构基线变更**） |
| **6** | 业务模块迁移 | ⬜ 未开始 | 7 批次，见第 6 节 |
| **7** | 工作流与调度替换 | ⬜ 未开始 | Activiti → WarmFlow **重写**；XXL-JOB → SnailJob。**历史流程实例数据不可迁移，在途流程需用户确认兜底方式** |
| **8** | client 聚合层扁平化 | ⬜ 未开始 | 43 个 `@FeignClient` 全废弃 |
| **9** | 部署与验证 | 🟡 部分完成 | Dockerfile / docker-compose / nginx / `.run` 已按新模块重建；JUnit 4 → 5 未做；全量回归未做 |

### 8.1 本轮（阶段 0 + 1 + 2）验收结果

| 验收项 | 结果 |
|---|---|
| `mvn -B -DskipTests clean install` | ✅ **BUILD SUCCESS 40/40**，`MVN_EXIT=0`，耗时 2:52 |
| 全仓品牌残留 | ✅ `scanned(text)=988  hits=0  allowlisted=7  file-exceptions=6`（含 `.qoder` 目录与 3 份 dotfile） |
| 模块对齐上游 | ✅ 34 子模块文件数全对齐 |
| SQL 对齐上游 | ✅ 14 份全对齐，差异全部收敛到 8 条已记录规则 |
| 支撑文件对齐 | ✅ `present=32  missing=10  ours-only=0`（missing 10 = `.gitee` 4 + `.claude` 6，均见 7.5） |
| `nla-admin` 启动至数据源连接 | ⬜ **未验证**，需 MySQL 8 + Redis 就绪后执行 |

### 8.1.1 初始化账号（已实测，非推断）

`sys_user` 的种子行只存 BCrypt 哈希，仓库内无明文。已用
`spring-security-crypto` 的 `BCrypt.checkpw` 实测比对（脚本 `.migration/check-admin-pwd.ps1`）：

| 账号 | 密码 | 数据权限 | 依据 |
|---|---|---|---|
| `admin` | `admin123` | 全部数据 | **实测哈希匹配** |
| `test` | `666666` | 本部门及以下 | SQL 内 `nick_name` 明文标注 |
| `test1` | `666666` | 仅本人 | 同上 |

另有 SnailAI 服务端独立账号（`sai_user` 表，pbkdf2 哈希）：`admin` / `admin123`，
在 `nla_ai.sql` 中有明文注释。**两者是不同体系，不要混淆。**

> 安全提醒：`application.yml` 的 `api-decrypt` RSA 公私钥是仓库内公开可见的示例密钥，
> 生产部署必须连同初始化密码一起更换。

### 8.2 已知遗留问题

1. **`nla-admin` 启动冒烟未做** —— 需要可用的 MySQL 8（库名 `nla-cloud`）与 Redis，
   并先执行 `script/sql/nla_system.sql` + `nla_job.sql` + `nla_workflow.sql` + `nla_ai.sql`。
2. **旧 `spring-boot-*` 目录仍在仓库中** —— 用户已确认采用分级清理，
   保留到阶段 6 完成后删除，清单与理由见 9.2；已删的 4 份部署产物见 9.1。
3. **`probe-newmod-res.ps1` 有个已知 bug** —— 测试路径未把 `cn\com\nla` 反映射回 `org\dromara`，
   导致 5 个 `src/test/java/**` 文件误报 `NO-UPSTREAM`。已人工 Read 上游 `SocialUtilsTest.java`
   确认这些测试确实来自上游，结论不受影响，脚本未修。
4. **阶段 5 的鉴权模型变更引入了原项目没有的能力** ——
   项目铁律 `gaps-and-fs-tables.md` 明确"方法级权限注解不存在"，而基线用 `@SaCheckPermission`。
   这是已明示的架构基线变更，不是违反铁律，但迁移时需同步更新铁律文档。

---

## 9. 旧代码清理

用户已确认采用**分级清理**：先删确定无用的部署产物，保留迁移需要对照的源码与 DDL。
旧代码在 `5.0.0-dev` 分支完整保留，删除可恢复。

### 9.1 已删除

| 路径 | 规模 | 删除依据 |
|---|---|---|
| `docker-compose.yml` | 1 份 | 旧微服务编排，8 个服务全指向已删模块 |
| `docker-compose-server.yml` | 1 份 | `nginx` + `mysql:5.7.17`（新工程要 8.0+），容器名全为 `spring-boot-*` |
| `docker-compose-backups.yml` | 1 份 | 同上，且 `build.context` 指向不存在的 `./spring-cloud/` 目录 |
| `spring-boot-cloud.iml` | 1 份 | IDEA 模块文件，`.gitignore` 的 `*.iml` 已覆盖，本来就未入库 |

新工程的编排已由 `script/docker/docker-compose.yml` 接管（见 7.3.1）。

### 9.2 保留到阶段 6 完成

| 路径 | 类型 | 规模 | 保留理由 |
|---|---|---|---|
| `spring-boot-business/` | 目录 | 7 子模块，586 Java | 阶段 6.2~6.7 的迁移源 |
| `spring-boot-client/` | 目录 | 2 子模块，136 Java | 阶段 8 的迁移源 |
| `spring-boot-common/` | 目录 | 2 子模块，218 Java | 阶段 4.1 实体迁移的源 |
| `spring-boot-feign/` | 目录 | 7 子模块，43 Java | 阶段 8 需逐个确认废弃 |
| `spring-boot-service/` | 目录 | 4 子模块，72 Java | 阶段 7.2 XXL-JOB → SnailJob 的对照源 |
| `spring-boot-starter/` | 目录 | 21 子模块，744 Java | 阶段 3 自建封装的源（见第 5 节） |
| `spring-boot-system/` | 目录 | 3 子模块，48 Java | 阶段 5 认证重写的对照源 |
| `sql/` | 目录 | 10 份 | **阶段 4.3 DDL 重写的输入源**，详见 9.3 |
| `.lib-service/` | 目录 | — | `allatori` / `rabbitmq-plugins` / `skywalking-agent`；已 gitignore（`tracked=0`），不占仓库体积，阶段 3/9 评估 native 与 agent 兼容性时可能还要用 |

### 9.3 `sql/` 为何不在本轮删除

虽然用户选项里列了 `sql/`，但逐份核对后发现它是**阶段 4.3 DDL 重写的直接输入**，
与保留 `spring-boot-*` 源码目录的理由完全一致，因此一并保留：

| 旧脚本 | 对应阶段 |
|---|---|
| `sys_bean-基类sql.sql`、`sys_bean_初始化.sql` | 6.1 `bean_*` → `sys_*` 表映射与存量数据迁移 |
| `sys_area.sql`、`sys_area_1.sql`（679273 行） | 6.1；`sys_area` 是 bean 独有表，**计划明确要求保留为业务表** |
| `sys_oa-基类sql.sql` | 6.2 |
| `sys_face.sql` | 6.4 |
| `sys_video.sql` | 6.5 |
| `freeswitch.sql` | 6.6（fs 呼叫中心 41 表） |
| `sys_activiti.sql` | 7.1；其中 `ACT_*` 表废弃，但旧表结构是重新设计 WarmFlow 流程的参照 |
| `quartz.sql`（219 行） | 7.2；已确认废弃，**可随阶段 7 一并删** |

`sys_area_1.sql`（679273 行）需单独评估是入库还是改为运行时导入 ——
注意它当前**已在仓库里**（`sql/` 目录 `tracked=10`），阶段 4 处理时需一并决定去留。

---

## 10. 迁移脚本索引

`.migration/` 目录（未入库或按需入库），全部为 PowerShell 5.1 脚本。

| 脚本 | 作用 | 幂等 |
|---|---|---|
| `build.ps1` | 唯一编译入口。设 `JAVA_HOME` + 全路径 mvn，日志落 `.migration/build.log` | ✅ |
| `add-bom.ps1` | 给含 CJK 字面量的脚本补 UTF-8 BOM。**PowerShell 5.1 会按 GBK 读无 BOM 的 .ps1** | ✅ |
| `audit-brand.ps1` | 全仓品牌残留审计，带白名单与命中计数 | ✅ |
| `regen-sql.ps1` | 14 份 SQL 的唯一事实来源（上游 → 替换链 → 8 条规则 → 校验 → 写盘） | ✅ |
| `regen-support-cfg.ps1` | docker-compose / nginx.conf / 4 个 `.run` 的唯一事实来源 | ✅ |
| `probe-yml-diff.ps1` | `nla-admin` 9 份资源与上游的规范化逐行差异 | ✅ |
| `probe-sql-diff.ps1` | 14 对 SQL 的差异 | ✅ |
| `probe-sql-brand.ps1` | 上游 SQL 的品牌行行号定位 | — ⚠️ **已丢失**，需时从 `regen-sql.ps1` 的 8 条规则反推 |
| `probe-cfg-diff.ps1` | docker-compose / nginx 的差异 | ✅ |
| `probe-support-parity.ps1` | 支撑文件（`.run` / `.gitee` / `.claude`）的存在性对齐 | ✅ |
| `probe-newmod-res.ps1` | 7 个新模块资源文件的逐份对齐（有已知 bug，见 8.2） | ✅ |
| `check-admin-pwd.ps1` | 用 `BCrypt.checkpw` 实测 `sys_user` 种子哈希对应的明文密码 | ✅ |
| `precommit-size-check.ps1` | 提交前的体积守卫：`git add -A -n` 干跑，列出最大的待入库文件、可疑二进制扩展名与按顶级目录的计数，防止人脸模型 / jar / native 库误入提交 | ✅ |
| `verify-encoding.ps1` | 中文完整性守卫：对 18 份关键交付物做三重检查——严格 UTF-8 解码 + 字节级往返比对、9 个 GBK 乱码特征字、通用的 GBK 往返双重编码探测（无需硬编码字符），失败时 `exit 1` | ✅ |
| `probe-audit-blindspot.ps1` | 列出审计范围内因扩展名不被识别而**未被扫描**的文件及其扩展名直方图，用于定期反查审计盲点 | ✅ |
| `probe-bom.ps1` | 盘点 `.migration/` 每个脚本的字节数、BOM 有无、是否含 CJK 字面量，并标出 `NEEDS-BOM`（即无 BOM 却含中文的危险脚本） | ✅ |
| `probe-codepoints.ps1` | 从现有文件字节里提取 CJK 字面量的 `\uXXXX` 码点，用于把脚本改成纯 ASCII 时不靠记忆拼转义 | ✅ |
| `probe-script-ledger.ps1` | 将本节索引与 `.migration/` 实际文件对账，分别列出"已登记但丢失"与"存在但未登记" | ✅ |

### 10.1 脚本编写的四条踩坑教训

1. **不要用 Bash 工具跑内联 PowerShell** —— `$`、引号、反引号会被剥离，`&&` 不被 5.1 支持。
   一切含这些字符的逻辑写入 `.ps1`，用 `powershell -NoProfile -ExecutionPolicy Bypass -File` 执行。
2. **脚本一律写成纯 ASCII，不要依赖 BOM** —— PowerShell 5.1 会把无 BOM 的 `.ps1` 按
   ANSI（zh-CN 机器上是 GBK）解析，中文字面量会被弄坏，甚至直接语法报错。
   `SearchReplace` 会剥掉 BOM，而且实测发现**未做任何编辑时 BOM 也会自行消失**
   （`audit-brand.ps1` 4943→4940、`verify-encoding.ps1` 2825→2822，正好各少 3 字节），
   所以"每次改完重跑 `add-bom.ps1`"并不可靠。
   **永久解法：把中文字面量改写为正则 `\uXXXX` 转义**（`audit-brand.ps1` 的 `若依`
   → `\u82e5\u4f9d`，`verify-encoding.ps1` 的 9 个乱码特征字同理），用
   `probe-codepoints.ps1` 从现有文件取码点、`probe-bom.ps1` 验收 `CJK=False`。
   ⚠️ **遗留风险**：`regen-sql.ps1`（7610 字节，`BOM=True`）仍含中文字面量、仍靠 BOM。
   它是 14 份 SQL 的唯一事实来源，下次需要重生 SQL 前必须先把它改成纯 ASCII。
3. **函数定义必须在调用之前** —— PowerShell 不做前向解析，否则运行时报未识别。
4. **不要依赖 `.migration/` 内脚本的持久性** —— 该目录已 gitignore，已确认丢失过：
   `probe-yml-diff.ps1`（被迫重写）与 `probe-sql-brand.ps1`（至今缺失）；
   另外 `audit-brand.ps1` 与 `verify-encoding.ps1` 的 BOM 也在无人编辑的情况下自行消失。
   **关键结论必须写进本文档**，脚本只当可再生的一次性工具；
   每次引用本节索引前先跑 `probe-script-ledger.ps1` 对账。

附带三条工具级陷阱：

- **`Grep` 尊重 `.gitignore`** —— 对已忽略但**仍被 git 跟踪**的目录（如 `.qoder/`）会返回
  假阴性的 0 匹配。这类目录必须走 `Get-ChildItem` 的脚本审计（见 7.6）。
- **`-like '??*'` 里的 `?` 是单字符通配符** —— 想筛 `git status --porcelain` 的未跟踪行
  不能用它，会匹配全部行；改用 `.Substring(0,2) -eq '??'`。
- **`Get-Content` 默认按 ANSI/GBK 解码无 BOM 的 UTF-8 文件** —— 实测本文件
  （纯 LF、无 BOM）被 `Get-Content` 读成 **655 行**，而加 `-Encoding UTF8` 得 801 行、
  `[IO.File]::ReadAllText` 按 LF 切得 802 段（含末尾空段），凭空少了 146 行。
  **读数、比对、写回一律显式指定 UTF-8**（`Get-Content -Encoding UTF8` 或
  `[IO.File]::ReadAllText/ReadAllBytes`）；`Set-Content` 默认编码同样会损坏中文。
  已用 `verify-encoding.ps1` 对 13 份关键交付物做字节级往返比对，全部 `roundtrip=True`、
  无乱码特征字，确认迁移过程未发生编码损坏。

### 10.2 "从上游重建"范式

配置与 SQL 文件**不手工打补丁**，一律：

```
读上游原文 → protect 占位 → own 包名替换 → brand 品牌替换 → restore 还原占位
           → 套用带 min 命中数的偏离规则 → 全部转换完并校验命中数 → 统一写盘
```

任一条规则命中数低于 `min` 就 `throw 'rules did not fire, nothing written'`，
宁可整批失败也不静默产出未去品牌的文件。
正则替换统一走 scriptblock `{ param($m) $lit }.GetNewClosure()`，
避免替换串里的字面 `$` 被当成组引用。
