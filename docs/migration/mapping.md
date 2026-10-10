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

### 4.4 数据层第一批交付：`face_person`（已落地）

阶段 3 的非 pay 技术封装已就绪，pay 沿用暂缓约定；下一阶段从数据层开始，第一批选择只有一张独有表的 face。新建 `nla-modules/nla-face`，登记业务 reactor 和根 POM 坐标管理；共 **4 个主 Java 文件 + 1 个 Mapper XML + 1 个测试类**。本轮只交付 Person / PersonBo / PersonVo / PersonMapper 与 DDL，未引入 JNI、未接入 admin、未迁业务服务/Controller。模块说明见 [`nla-face/README.md`](../../nla-modules/nla-face/README.md)。旧源码与 DDL 保持原位。

**数据契约与修正**：

- `Person` 继承 `BaseEntity`，显式 `Long id` / `IdType.ASSIGN_ID`；旧自增主键取消，`create_user_id/update_user_id` 改 `create_by/update_by`，新增 `create_dept`。时间统一 LocalDateTime；不新增租户字段。
- 按 §4.3 使用 **String delFlag + @TableLogic(0/1)**，DDL 对应 `char(1)`；两个自定义 XML 查询都显式过滤 `del_flag='0'`，避免逻辑删除人员进入搜索结果和特征缓存。
- 保留 imgId/imgUrl/personName/personAge/gender/address 和特征字符串字段。旧 `extract varchar(256)` 扩为 **LONGTEXT**，避免数组字符串超长；普通 BaseMapper 查询通过 `@TableField(select=false)` 排除特征，`selectImgIdList` 同样不读取特征。PersonVo 和 PersonBo 不包含 extract，实体 toString 也排除特征。
- `selectImgIdList` 保留方法名，参数绑定；null/空集合明确返回空结果，不再生成无效 IN SQL。新增内部 `selectFeatureList`，仅选择有效人员 id/img_id/extract，排除空特征。返回部分填充实体，不用于展示人员资料。
- Bo 使用 AddGroup/EditGroup 与 MapStruct-Plus；新增不得自带 ID，编辑需 Long ID；姓名、图片编号/地址、字段长度、非负年龄和性别 0..2 校验与列定义对应。特征由后续识别服务生成，Bo 转实体后不能直接当完整记录保存。
- `script/sql/nla_face.sql` 为**新库初始化**脚本：仅 `create table if not exists`，不 drop、不导入旧测试数据、不预置菜单；显式 utf8mb4 / `utf8mb4_cs_0900_ai_ci`。保留旧图片编号无唯一约束的语义，增加 `(img_id, del_flag)` 普通索引；重复图片编号的业务处理留阶段 6.4。已有旧表不自动升级，外部数据库脚本未执行。

**验证（2026-10-10，Windows / JDK21）**：

- `PersonDataContractTest` **9/9 通过，0 跳过，BUILD SUCCESS，MVN_EXIT=0**；实际解析交付 Mapper XML 并在 H2 2.4.240 MySQL 模式执行 SQL，覆盖雪花 ID、审计填充、超过 256 字符的特征持久化、普通读取不带特征、参数绑定与空集合、逻辑删除保留物理行并过滤全部读取、更新保留特征/创建审计、内部特征查询、生成映射/中文 JSON、校验分组、数据库非空/列长约束。日志 `.migration/test-face-data.log`。
- 根工程 `mvn -o -B -DskipTests compile` → **47/47 模块 BUILD SUCCESS，MVN_EXIT=0**；模块 reactor 集成已验。日志 `.migration/build-face-data-reactor.log`。
- 编译依赖树检查：Boot4 / Spring7、MyBatis-Plus 3.5.17、MyBatis 3.5.19、MapStruct-Plus 1.5.3；H2 仅 test，未依赖旧 starter/cloud 或 JNI。仅在新模块 MyBatis 依赖边界排除 Redis/lock4j 间接引入的 Boot2 AOP starter，沿用 common-core 的 Boot4 AspectJ；基线 MyBatis/Redis 模块未改。日志 `.migration/deps-face-data.log`。
- 12 份交付文本 UTF-8 严格往返通过，实体列与 DDL 审计列对齐，无旧 Java 包/Swagger2/javax/Date/尾随空白，`git diff --check` 通过。

**范围与剩余验收**：H2 测试只剥离 MySQL 表级引擎/字符集/排序规则/行格式选项，不代表真实 MySQL 排序规则和索引性能验收；固定测试审计填充器不代表生产登录态/权限验收。阶段 4 已开始但未整体完成，后续 video 12 表 / fs 41 表仍待重写；阶段 6.4 的识别、特征缓存、图片输入、业务鉴权和管理接口仍待迁移。

---

### 4.5 短信数据层删除标志校正（已落地）

核对已迁 `nla-message` 时发现 SmsConfig / MobileMessageTemplate 的 `Long delFlag` 与 §4.3 的 String/char(1) 约定不一致。本轮已修正两张实体及初始化 DDL；说明和已有表升级检查示例见 [`nla-message/README.md`](../../nla-modules/nla-message/README.md)。

**改动与兼容边界**：

- 两张实体改为 **String delFlag**，显式 `@TableLogic(value="0", delval="1")`；DDL 两列改为 **char(1) not null default '0'**。Bo/Vo 未暴露删除标志，管理接口字段不变。
- 短信三张表的初始化 DDL 补齐显式 **utf8mb4 / utf8mb4_cs_0900_ai_ci**，符合阶段 4 约定。菜单/角色授权种子、其他列定义及业务发送路径未改。
- `MobileMessage` 仍是追加型发送记录，不新增 delFlag，保留物理删除。渠道/模板逻辑删除后保留物理行，删除前有效模板引用校验继续生效。
- 初始化脚本原有 drop 和种子语句保留，本轮未向外部数据库执行脚本。已有表须先核对 0/1/null 值和 schema，再以独立列升级方式处理；重新初始化不能代替数据库升级。

**验证（2026-10-10，Windows / JDK21）**：

- `mvn -o -B -pl nla-modules/nla-message -am -Dtest=SmsDataContractTest -Dsurefire.failIfNoSpecifiedTests=false test` → **8/8 通过，0 跳过，BUILD SUCCESS，MVN_EXIT=0**。实际加载交付 DDL 的三张表，在 H2 2.4.240 MySQL 模式执行真实 Mapper 与业务查询：String/char(1) 默认值与非空列定义、供应商读取排除删除渠道、删除最新模板后的回退、可用渠道排除删除模板/渠道和停用项、删除前引用校验及定向注销、批量逻辑删除/分页计数、更新与重复删除不恢复删除项、发送记录物理删除。只将注销回调 mock，不创建供应商客户端或发送短信。日志 `.migration/test-sms-data.log`。
- 根工程 `mvn -o -B -DskipTests compile` → **47/47 模块 BUILD SUCCESS，MVN_EXIT=0**；实体类型变更与 admin 依赖编译通过。日志 `.migration/build-sms-data-reactor.log`。
- H2 仅 test 依赖；测试资源引用交付 DDL，不另维护一份建表定义。7 份交付文本严格 UTF-8 往返通过，两个删除字段与 DDL 类型对齐，菜单/角色种子保持一致，`git diff --check` 通过。

**剩余验收**：H2 剥离 MySQL 表级引擎/字符集/排序规则选项，不执行 drop 或菜单种子。真实 MySQL 列转换与排序规则、完整应用启动、Redis 与供应商发送联调未执行；短信发送入口仍按 §6.6.1 待定。阶段 4 后续 video 12 表 / fs 41 表待重写。

---

### 4.6 视频监控数据层：12 张表（已落地）

新建 `nla-modules/nla-video`，登记业务 reactor 和根 POM 坐标管理，交付 **48 个主 Java 文件（12 套 Entity/Bo/Vo/Mapper）+ 2 个 Mapper XML + 1 个测试类**。建表脚本 [`nla_video.sql`](../../script/sql/nla_video.sql)，模块说明见 [`nla-video/README.md`](../../nla-modules/nla-video/README.md)。旧源码/DDL 保留，不导入旧测试数据；未依赖协议封装、未接入 admin，Controller/Service/权限/菜单留阶段 6.5。

| 旧实体 / 表 | 新实体 / 主键 | 迁移约束 |
|---|---|---|
| Device / video_device | Device / Long id | 保留 device_id 有效唯一、online 默认 2；状态含义留业务阶段核定 |
| DeviceChannel / video_device_channel | DeviceChannel / Long id | channel_id 仍在有效记录中全局唯一；manufacture 列名保留，修正经纬度注释 |
| DeviceAlarm / video_device_alarm | DeviceAlarm / Long id | 保留报警级别/方式/类型与 LocalDateTime 报警时间 |
| DeviceMobilePosition / video_device_mobile_position | DeviceMobilePosition / Long id | 保留原坐标、速度/方向与 LocalDateTime 位置时间 |
| MediaServer / video_media_server | **VideoMediaServer / String id** | 与 fs.MediaServer 消歧；协议节点 ID 由调用方提供，有效 (ip,http_port) 唯一 |
| ParentPlatform / video_parent_platform | ParentPlatform / Long id | server_gb_id 有效唯一；默认目录和协议配置保留 |
| PlatformCatalog / video_platform_catalog | **PlatformCatalog / String id** | 目录协议主键由调用方提供，不变为 Long |
| PlatformGbStream / video_platform_gb_stream | PlatformGbStream / Long id | String gb_stream_id 对应 GbStream.gbId，非 Long 数据库主键 |
| PlatformGbChannel / video_platform_gb_channel | PlatformGbChannel / Long id | String device_channel_id 对应 DeviceChannel.channelId |
| GbStream / video_gb_stream | **GbStream / Long gb_stream_id** | 特殊主键列名保留；有效 (app,stream) 与 gb_id 分别唯一 |
| StreamProxy / video_stream_proxy | StreamProxy / Long id | 保留代理配置及有效 (app,stream) 唯一 |
| StreamPush / video_stream_push | StreamPush / Long id | self→onSelf、total_reader_count String 保留；有效 (app,stream) 唯一 |

**数据契约**：

- 10 个 Long 主键使用 ASSIGN_ID，2 个 String 主键使用 INPUT；全部继承 BaseEntity，旧 create_user_id/update_user_id 改 create_by/update_by，新增 create_dept，审计五列允许空值；Date 统一 LocalDateTime。无租户列、无物理外键。
- 12 张表统一 String delFlag / `char(1) not null default '0'` / 显式 @TableLogic(0/1)。原生关联查询逐张过滤删除标志。报警/位置记录本轮也采用逻辑删除，历史清理与留存策略留业务阶段。
- 9 张带唯一业务键的表新增数据库生成列 active_marker：有效为 1，删除为 NULL；保留 10 个原唯一索引名和业务键，追加该列，支持多次删除/重建。未使用会在第二次删除时冲突的 `(业务键,del_flag)`。生成列不进入实体/Bo/Vo。字符串主键仍不可复用，恢复或新身份的规则留业务阶段；复用国标编号前须事务性处理旧有效关联，不能跳过引用校验。
- Bo 采用 AddGroup/EditGroup 与 MapStruct-Plus，拒绝审计/删除字段；Long 新增不接受自带主键、编辑必填，String 主键新增/编辑必填。字符串长度、必填列、原 unsigned 字段的非负数及端口 0..65535 校验对应 DDL。整数列改 signed 对齐 Java Integer/Long，其他业务列长和默认值保留。
- Vo 不包含 password/secret/delFlag，原始 url/srcUrl/dstUrl 因可能携带账号或 token 也不进入 Vo。实体/Bo 的凭据及原始流地址 JSON 仅写、toString 排除；内部 Mapper 读取保留协议所需凭据，业务接口须返回 Vo。
- 旧非持久化 channelCount、platformId/catalogId、manufacturer/hostAddress、childrenCount/type 放入对应 Vo，不添加数据库列；普通 CRUD 不计算派生查询字段。ptzTypeText 作为独立列保留，旧 setter 派生文本行为留业务阶段。
- 重写两个只读平台关联查询 selectSharedChannels/selectSharedStreams，按平台国标编号与目录集合查询，验证目录所属平台；通道查询过滤关联/设备/通道/平台/目录五表，流查询过滤关联/流/平台/目录四表。全部绑定参数，null/空目录集合返回空结果，已删除或缺失目标不返回。旧分页/树/流状态聚合/目录事件/原生删除 SQL 留阶段 6.5 重写，未直接复制旧 `${}` 拼接。
- DDL 仅新库 CREATE IF NOT EXISTS，显式 utf8mb4 / utf8mb4_cs_0900_ai_ci，无 drop、旧测试数据、媒体节点种子或凭据；已有表需独立 schema 升级，本轮未执行外部数据库脚本。

**验证（2026-10-10，Windows / JDK21）**：

- `mvn -o -B -pl nla-modules/nla-video -am -Dtest=VideoDataContractTest -Dsurefire.failIfNoSpecifiedTests=false test` → **47/47 通过，0 跳过，BUILD SUCCESS，MVN_EXIT=0**。H2 2.4.240 MySQL 模式加载交付 DDL 和两个真实 XML，执行全部 12 个 Mapper 全字段 CRUD、审计更新、删除后读取过滤/更新与重复删除不恢复、中文与 LocalDateTime、生成映射、校验分组、凭据 JSON 仅写、数据库非空/长度约束、默认值、字符串/特殊主键、国标关联/参数绑定/目录归属/空集合、全部参与查询表的删除过滤、独立唯一键及三轮删除/重建。日志 `.migration/test-video-data.log`。
- 根工程 `mvn -o -B -DskipTests compile` → **48/48 模块 BUILD SUCCESS，MVN_EXIT=0**。日志 `.migration/build-video-data-reactor.log`。
- 编译依赖树确认 Boot4 / Spring7、MyBatis-Plus 3.5.17、MyBatis 3.5.19、MapStruct-Plus 1.5.3；H2 仅 test，未依赖旧工程 starter/cloud 或 SIP/ZLM/JNI。MyBatis 依赖边界排除 Redis/lock4j 间接引入的 Boot2 AOP starter，沿用 common-core 的 Boot4 AspectJ。日志 `.migration/deps-video-data.log`。
- 58 份交付文本严格 UTF-8 往返通过，12 套旧实体持久字段与新实体/DDL 对齐；无旧 Java 包、Swagger2、javax、Date 或尾随空白，`git diff --check` 通过。

**剩余验收**：H2 剥离 MySQL 表级选项及 STORED 关键字，保留生成列表达式；只在测试中给索引名加表名前缀，适配 H2 的 schema 级索引命名。真实 MySQL 生成列/排序规则/索引性能、生产登录态审计/权限、完整应用启动和 GB28181/ZLM/ONVIF 业务联调未执行。阶段 4 后续 fs 41 表待重写；pay 持续暂缓。

---

### 4.7 呼叫中心数据层：41 张表（已落地）

新建 `nla-modules/nla-callcenter`，登记业务 reactor 和根 POM 坐标管理，交付 **164 个主 Java 文件（41 套 Entity/Bo/Vo/Mapper）+ 2 个 Mapper XML + 1 个测试类**。建表脚本 [`nla_callcenter.sql`](../../script/sql/nla_callcenter.sql)，表分组、数据契约与接入约束见 [`nla-callcenter/README.md`](../../nla-modules/nla-callcenter/README.md)。本轮只迁数据层，未接入 admin 或协议封装；Controller/Service/菜单/权限/策略引擎留阶段 6.6。旧源码、旧 SQL 和 FreeSWITCH 自身运行表未改，不迁旧测试数据。

**迁移决策**：

- fs.MediaServer 消歧为 FsMediaServer，保留 String 协议主键 / INPUT；其余 40 表 Long 主键 / ASSIGN_ID，无自增。全部继承 BaseEntity，旧 Date 改 LocalDateTime，统一五个允许空值的审计列，不增加租户列或物理外键。
- 41 表采用 String delFlag / char(1)，显式 `@TableLogic(value="0",delval="1")`。历史通话/事件表的留存与清理规则待业务阶段定义，不能把逻辑删除当成完整业务删除流程。
- 保留 16 表共 19 个业务唯一键的原名称、字段与全局/企业范围，唯一键追加 active_marker 生成列（有效 1、删除 NULL），支持多轮删除/重建。包括 company_name/company_code/uniq_skill_name 等遗留名称；VdnPhone 原 uni_idx_phone(vdn_id,company_id) 保留。字符串主键删除后不能直接复用，引用检查、恢复与级联事务留 Service。
- Company.balance 的旧 DDL double 改 decimal(19,4)，与 BigDecimal 对齐；补齐旧实体遗漏的 Company.conferenceLimit、Agent.display。
- UserAgent.agentId/userId 从 String / varchar 改 Long / bigint，必填无默认值；旧 XML 的真实关联是 `fs_user_agent.agent_id = fs_agent.id`，userId 是系统用户数据库 ID。避免 MySQL 隐式浮点比较丢失大于 2^53 的雪花 ID 精度，新增 idx_user_agent_company_user(company_id,user_id,del_flag)。Agent 自身工号与 SIP 号码仍是 String。
- OverflowConfig.overflowValue 从 Integer / int 改 Long / bigint，承载技能组/IVR/VDN 雪花目标 ID；到协议字符串 DTO 的转换留阶段 6.6。CallLog 等原 Long 时间戳/时长、`fristQueueTime` 拼写、VdnSchedule 的字符串调度规则与 VdnConfig.routeValue 的混合字符串语义保留；GateWay.username / routeId 的旧类型保留待业务核定。
- Bo 使用 AddGroup/EditGroup，不接受审计、删除和生成列。实体/Bo 的 passwd/password/sipPwd/secret/secretKey/notifyUrl/cdrNotifyUrl JSON 仅写并排除日志，Vo 不包含这些字段；内部实体读取保留凭据供协议使用。
- 号码字段排除 toString，Vo 标注 PHONE 脱敏；实际输出依赖既有 JsonValueEnhancer、SensitiveJsonFieldProcessor 和 SensitiveService 权限判定。缺少 SensitiveService 时保留原值，裸 JSON、Vo getter 和数据库仍是原号码，生产响应链待业务接入验收。Agent.sipPhoneList 仅保留 Vo，普通 CRUD 不计算。
- 重写三个参数绑定查询：selectByUserId(companyId,userId) 用 exists 去重，按 create_time/id 稳定倒序返回 AgentVo 列表；selectBySip(companyId,sip) 限同企业有效座席/SIP；selectConfigsByGroup(companyId,groupId) 限同企业有效技能组/配置，按关联优先级排序并返回配置主键。全部参与表显式过滤 del_flag；null、错企业和不存在对象不匹配，不回显凭据。系统用户存在性/账号状态与当前登录权限由后续业务层负责。
- DDL 仅新库 CREATE IF NOT EXISTS，41 表显式 utf8mb4 / utf8mb4_cs_0900_ai_ci，无 drop、seed 或外部数据库操作；已有库须单独升级，初始化脚本不会改已有表。

**验证（2026-10-10，Windows / JDK21）**：

- `mvn -o -B -pl nla-modules/nla-callcenter -am -Dtest=CallcenterDataContractTest -Dsurefire.failIfNoSpecifiedTests=false test` → **121/121 通过，0 跳过，BUILD SUCCESS，MVN_EXIT=0**。加载交付 DDL、两个真实 XML 和全部 41 个 Mapper，覆盖全字段 CRUD/审计/逻辑删除、Bo→Entity→Vo 全字段生成映射、分组校验、精确金额、遗漏列、雪花关联精度、调度字符串/计数、参数绑定、企业隔离、每个查询参与表删除过滤、重复绑定去重/稳定排序、19 个唯一键独立冲突及三轮删除重建、凭据仅写和响应增强号码脱敏。日志 `.migration/test-callcenter-data.log`。
- 根工程 `mvn -o -B -DskipTests compile` → **49/49 模块 BUILD SUCCESS，MVN_EXIT=0**。日志 `.migration/build-callcenter-data-reactor.log`。
- 编译依赖树保持 Boot4 / Spring7 / MyBatis-Plus / MapStruct-Plus；H2 仅 test，无旧 starter/cloud、ESL/SIP/JNI 依赖。排除 MyBatis 间接引入的 Boot2 AOP starter，沿用 common-core 的 Boot4 AspectJ。日志 `.migration/deps-callcenter-data.log`。
- 41 套旧实体字段与全部旧 DDL 列/默认值/普通及唯一索引逐项核对，只采用上面记录的修正；164 个主 Java + 1 个测试、2 XML、16 生成列/19 唯一键核对通过。交付文本严格 UTF-8 往返，无旧 Java 包、Swagger2、javax、Date 或尾随空白；`git diff --check` 通过。核对脚本 `.migration/audit-callcenter-data.py`。

**剩余验收**：H2 剥离 MySQL 表级选项和 STORED，保留生成列表达式；测试索引加表名前缀适配 schema 级命名。真实 MySQL 生成列/排序规则/索引性能、生产审计/鉴权、完整应用启动及 ESL/SIP/媒体联调未执行。sys_area 数据层已在 6.1.1 交付，参考数据导入与搜索方案评估仍待处理；阶段 4 未整体验收完成，pay 持续暂缓。

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
| `starter-netty`(19) / `starter-socket-io`(11) | 自建 `nla-common-socketio` | netty-socketio 1.7.19 → 2.0.14；TCP 有效代码迁入，见 5.1.4 | ✅ 已建·编译/协议测试 GREEN |
| `starter-rabbitmq`(2) | 自建 `nla-common-mq` | Spring AMQP4 / Jackson3；配置与客户端重写，见 5.1.5 | ✅ 已建·编译/契约测试 GREEN |
| `starter-freeswitch`(199) | 自建 `nla-common-freeswitch` | 见 5.1.1 | ✅ 已建·编译 GREEN |
| `starter-video`(201) | 自建 `nla-common-video` | 见 5.1.2 | ✅ 已建·编译/契约测试 GREEN |
| `starter-pay`(163, 8 渠道) | 自建 `nla-common-pay` | 见 5.1 | 待阶段 3 |

### 5.1 阶段 3 的 6 个自建封装（5 已建 / 1 待建）

坐标 `cn.com.nla:nla-common-{tech}`，全部需登记进 `nla-common-bom`。
每个遵循 `basic`（注解/枚举/POJO，无 Spring 依赖）+ `core`（配置/AOP/实现）二段式。
facesdk 的基础层保留 `com.seeta.sdk` 包名以匹配 JNI 符号，项目对象池/代理/配置放 `cn.com.nla.common.facesdk.core`，不重命名 JNI 绑定。

| 模块 | 源 | 关键风险 |
|---|---|---|
| ✅ `nla-common-freeswitch` | `starter-freeswitch`(199) | `freeswitch-esl 1.6.7.RELEASE` **尖峰已验：核心库 JDK21 GO（Java8 字节码、0 处 `javax/*`）；starter 是 Boot2/`spring.factories` 产物，Boot4 不自动加载 → 自建 `@AutoConfiguration` 替代；netty 4.1→4.2 需实测；见 5.2**；铁律沿用：管理接口禁止执行 FreeSWITCH CLI / 系统命令 / 文件删除 |
| ✅ `nla-common-video` | `starter-video`(201) | **已交付，见 5.1.2**；JDK21 本机 SIP UDP 收包、TCP/UDP 端口释放与模拟 ZLM HTTP 已验；真实设备注册/心跳/点播、ZLM RTP 与 ONVIF 联调延后 |
| `nla-common-pay` | `starter-pay`(163) | **尖峰已验 JDK21 GO（有条件），见 5.3.2**：实测外部 SDK 只有 `IJPay-Core:2.9.11`（Java8）+ `alipay-sdk-java:4.39.42.ALL`（Java6，0 处 sun/risky javax），**非旧估的 alipay-easysdk/yungouos/binarywang/weixin-popular**（那些不在 POM）；唯一迁移面 `javax.servlet`→`jakarta`（封装层不走 IJPay servlet helper 即绕过）；`bcprov-jdk15on`建议换 `jdk18on`；**金额字段约定项目内缺失**，不臆造精度方案；pay「保持暂未开发」优先级最低 |
| ✅ `nla-common-facesdk` | `spring-boot-face/.../com/seeta/sdk` + pool/proxy | **已交付，见 5.1.3**：29 SDK 文件、135 个实际 native 方法签名保持；16 组对象池/代理；Windows amd64 CPU / JDK21 真实 JNI 创建与释放已验。Linux/GPU 与真实识别效果待环境；基础镜像 `seetaface_face_work` 重建留部署阶段 |
| ✅ `nla-common-socketio` | `starter-socket-io` + `starter-netty` | **已交付，见 5.1.4**：Boot4 / JDK21 / Netty4.2 下 21 项测试通过，含真实 WebSocket/Polling/TCP；真实 Redis 多节点和代理联调延后 |
| ✅ `nla-common-mq` | `starter-rabbitmq` + `MqConstant` | **已交付，见 5.1.5**：Boot4 / AMQP4 / Jackson3，20 项测试通过；真实 RabbitMQ、delay 插件与 confirm/return 联调延后 |

#### 5.1.1 `nla-common-freeswitch` 交付结果（阶段 3.1，已落地）

源 `starter-freeswitch` 199 文件 + 9 资源整体迁入 `nla-common-freeswitch`，已登记 `nla-common/pom.xml` 反应堆 `<module>` + `nla-common-bom` 坐标声明（反应堆由 40 → 41 模块；全量 41 模块 reactor 重建待下次整构建确认，本轮以单模块 `-am` 编译为准）。单模块 `mvnw -o -pl nla-common/nla-common-freeswitch -am compile`（JDK21）→ **BUILD SUCCESS**（`SUCCESS [11.845s]`，日志 `.migration/build-w4i.log`）。

**关键决策**（详版在 gitignored `.migration/ledger-freeswitch.md`，结论按 §10.1 教训 4 固化于此）：

- **ESL 去 starter 化**：只依赖核心库 `freeswitch-esl:1.6.7.RELEASE`（非 Boot2 starter），自建 `EslEventHandler`/`@EslEventName`/`FreeswitchEslProperties`/`EslEventDispatcher` + `FreeswitchAutoConfiguration`（`.imports` 注册）；配置前缀沿用 `link.thingscloud.freeswitch.esl.inbound`（既有 YAML 免改）。
- **nacos 解耦**：去 spring-cloud-alibaba，5 文件 21 处 `NacosDiscoveryProperties.getIp()/getPort()` 改 `SipConfigProperties` + hutool `NetUtil.getLocalhostStr()`；节点 Address 的 port 由 HTTP-port 改 SIP-port（老数据已弃、写比同源）。
- **Redis 全量重写为 nla Redisson（Strategy B）**：`incr`→`incrAtomicValue`、动态订阅 `subscribeAndGetListenerId`/`unsubscribe`、发布统一 `SerializationUtils.serialize` → **byte[] 跨模块契约**（迁 video/fs 业务层时发布侧须遵守）。
- **去 web 化**：`FsRestResult` 由 `extends DeferredResult` 改 `extends CompletableFuture`（`delayedExecutor` 惰性复刻超时），common 库 **0 处 `org.springframework.web`**。
- **Date→LocalDateTime（w4i）**：模型 10 + 运行时 17 文件；计费 `Math.abs(Duration.between(a,b).getSeconds())`；消费侧 `.getTime()` 断点（`DefaultLineupStrategy`/`VipLineupStrategy`）改 `.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()`；3 处有意排除（`VdnConfigInfo` 周日=1 映射、`VideoSipDate` 的 `Calendar.setTime`、一行注释死代码）。Redis Jackson3(`TypedJsonJackson3Codec`) 序列化双向验证绿灯。

**部署告诫**：`FreeswitchAutoConfiguration` 受 `@ConditionalOnProperty("freeswitch.enabled"="true")` 门控（默认关、无 `matchIfMissing`）；但 `FreeswitchConfig`（`@ComponentScan("cn.com.nla.common.freeswitch")`）与 10 个 ESL handler（`@Component` 注入 `InboundClient`）由消费方组件扫描**无条件注册** → 消费方 app **必须**设 `freeswitch.enabled=true`，否则 `InboundClient` 缺失致启动 `UnsatisfiedDependency`。SIP 侧（`SipServer`/`SipServerRunner`/`Sip*Event`）靠 `@Component` 扫描、不受门控。当前无任何 nla app 依赖本模块，迁 6.6 fs 业务时须处理此门控（备选：把 handler/FreeswitchConfig 也纳入门控以支持“依赖但停用”）。

**提交状态**：本轮 freeswitch 全部改动**未提交**（提交需用户授权）。

#### 5.1.2 `nla-common-video` 交付结果（阶段 3.2，已落地）

旧 `starter-video-basic/core` 的 **201 个 Java 文件全部迁入** `nla-common/nla-common-video`，补充适配类后主源码共 **212 个 Java 文件**。包分为 `cn.com.nla.common.video.basic`（协议模型、枚举，无 Spring/MyBatis 依赖）和 `core`（SIP/ZLM/ONVIF 实现、缓存与订阅）。已登记 `nla-common/pom.xml` 与 `nla-common-bom`；根 POM 管理 ONVIF `1.0.2`，JAIN-SIP 仍为 `1.3.0-91`。旧迁移源保持不变，接入示例见 [`nla-common-video/README.md`](../../nla-common/nla-common-video/README.md)。

**关键契约与适配**：

- **Boot4 自动配置**：`SipConfig` 改 `@AutoConfiguration`，由 `.imports` 注册，`video.enabled=true` 才启用，默认关闭；协议组件仅经该配置导入，消费方宽范围扫描也不会绕开门控。配置 `sip.*` → **`video.sip.*`**，`video-settings.*` → **`video.settings.*`**，避免 FreeSWITCH 配置混用。
- **SIP 去 Nacos 化**：保留 `bind-ip`/`advertised-ip` 的监听与公布地址分离，节点注册保存 **SIP 端口**。独立创建 `SipStackImpl`（绕开 `SipFactory` 按 IP 复用栈），栈名 `NLA_VIDEO_GB28181_SIP` 与 FreeSWITCH 分开；迁入代码按类型注入，已验证同名 `sipServer`/`dynamicTask`/`sipCommander`/`videoProperties` Bean 不干扰 video。初始化校验 id/domain/port，绑定失败抛错，应用关闭停止 SIP 栈并释放传输端口。
- **HTTP 去 Forest 化**：ZLM 使用 Hutool GET，保留 HTTP/HTTPS、路径前缀、secret、查询参数编码与 10 秒超时；关闭 HTTP 响应，传输失败返回协议错误 code=2。移除旧工程 starter/cloud、Spring Data Redis、`javax.annotation` 和直接 Spring Web 引用；日志统一 SLF4J，JAIN-SIP 的旧 Log4j API 由 `log4j-over-slf4j` 桥接。
- **Redis 改用既有 Redisson**：缓存基于 `RedisUtils`，`VideoCache` 补齐秒级 TTL、非正 TTL 永久保存、原子计数、hash/set/zset 操作。所有协议 Topic 保持 **`SerializationUtils.serialize(...)` → `byte[]` → 接收方反序列化一次** 的跨模块契约。
- **异步与订阅**：`VideoRestResult<T>` 改为 `CompletableFuture<T>`，超时回调惰性求值；回调按 key/id 隔离并在完成/超时后清理。每节点只监听一个回调 Topic；Hook 按类型共享订阅并按字段过滤，删除/续期按 HookKey 身份操作，过期时间不参与 hash identity；SIP 成功/错误及录像查询订阅在完成、替换和关闭时释放。动态调度线程池随上下文关闭。
- **模型与消费方 SPI**：业务 DTO 时间转 `LocalDateTime`，SIP 日期编码与 Hook 过期计时保留 `Date`。`ProtocolResult` 保留 **0-success** 协议码，阶段 6.5 的 Controller 须转换成 `R<T>` 语义。移除旧 JWT，`CurrentUserProvider` 可由消费方覆盖，默认 null 触发旧下载默认用户；业务服务与流鉴权 SPI 留给 6.5 实现。启用模块须提供 `DeviceChannelVoService` Bean，其他 SPI 随 Runner/协议流程使用，未实现时不可直接启用完整业务。ZLM Hook HTTP 路由由消费方调用 `MediaHookServer`。

**验证（2026-10-10，JDK21）**：

- `mvn -o -B -pl nla-common/nla-common-video -am -Dtest=VideoContractTest -Dsurefire.failIfNoSpecifiedTests=false test` → **15/15 通过，BUILD SUCCESS，MVN_EXIT=0**。覆盖默认关闭/宽扫描、启用绑定、同名 Bean 与用户 SPI 覆盖、回调隔离和超时清理、Hook 匹配/删除/续期、SIP 原始/包装事件、录像跨节点发布与订阅替换、TTL/原子计数、中文 DTO/LocalDateTime Redis codec 往返、模拟 ZLM 请求，以及真实本机 SIP UDP 收包、TCP/UDP 端口释放，以及关闭 video 后同 IP 的既有 SIP 栈继续监听。Redis Topic 使用内存模拟，不代表真实 Redis 连通性验收。
- 根工程 `mvn -o -B -DskipTests compile` → **43/43 模块 BUILD SUCCESS，MVN_EXIT=0，最终复验耗时 16.169 秒**；已实证 video 的 reactor 注册与依赖集成。日志 `.migration/test-video.log` / `.migration/build-video-reactor.log`。

- `dependency:tree -Dscope=compile` → **BUILD SUCCESS**；video 的依赖子树无旧工程 starter/cloud、Forest、Log4j 1、Boot 2 AOP starter。仅在 video 的 Redis 依赖边界排除 lock4j 间接引入的旧 AOP starter，沿用 common-core 的 Boot4 AspectJ。基线 Redis 模块本身未改。日志 `.migration/deps-video.log`。
- 文件/编码审计：201 个迁移源逐个对照 **missing=0**；220 份交付文本严格 UTF-8 往返通过；76 个 basic Java 无 Spring/MyBatis import；新模块无旧包/未门控组件/尾随空白，`git diff --check` 通过。

**剩余外部验收**：真实 GB28181 设备注册/心跳、点播/录像回放、ZLM RTP/Hook 和 ONVIF 设备联调需外部环境，尚未执行；阶段 6.5 的 12 张业务表、Controller 与 SPI 实现不属于本轮技术封装。

#### 5.1.3 `nla-common-facesdk` 交付结果（阶段 3.3，已落地）

新建 `nla-common/nla-common-facesdk`，登记 `nla-common` reactor 与 `nla-common-bom`。迁入 **29 个 SDK 文件 + 17 个 pool 文件（16 算法池 + 配置）+ 16 个 proxy 文件**，新增自动配置、属性与加载初始化器后共 **65 个主 Java 文件**。接入示例见 [`nla-common-facesdk/README.md`](../../nla-common/nla-common-facesdk/README.md)。旧 face 业务模块、DLL/SO 与模型保持原位，本轮只迁技术层。

**关键契约与修正**：

- **JNI ABI 保持**：`com.seeta.sdk` 包名、native 方法签名与数据字段保留，逐文件核对 **135 个实际 native 声明一致**；原尖峰的 137 计数包含 2 行注释中的声明，已校正。项目代码包为 `cn.com.nla.common.facesdk.core`，SDK 层无 Spring import。
- **Boot4 门控**：`FaceSdkAutoConfiguration` 通过 `.imports` 注册，`face.enabled=true` 才加载 native，默认关闭；消费方组件宽扫描不会触发加载。沿用 `face.dll-path` / `face.csta-path`，新增设备/设备 ID、每算法池容量与等待时间配置，默认最大 8 个对象、借用等待 30 秒。消费方可替换算法代理或加载初始化器。
- **加载失败即启动失败**：修正旧 Windows 路径拼接；支持 native 根目录或直接平台目录，按 `dll.properties` 数字顺序加载 base → JNI，CPU/GPU 的 tennis 库单独选取。启动前检查全部库文件和默认算法使用的 10 个模型，关闭清单输入流，不再吞掉加载异常或错误标记成功；同 JVM 不允许中途切换库/设备。
- **显式 native 生命周期**：18 个有 native 句柄的 SDK 类支持幂等 `AutoCloseable.close()`，释放后 `impl=0`；`finalize()` 保留兼容兜底。旧池的 `destroyObject` 仅 `object=null`，实际没有释放 native；现在 16 个池销毁时调用 `close()`，校验指针非零。16 个代理都可关闭，随 Spring 上下文关闭释放对象池。
- **操作失败向上抛出**：代理保留算法方法名，异常改抛 `IllegalStateException`，避免错误被包装成正常结果；特征提取返回 false 时同样失败。`QualityOfLBN` 模型从错误的 `pose_estimation.csta` 改为 `quality_lbn.csta`。
- **图像适配去 Web 化**：SDK 工具去旧 `MockMultipartFile` / Spring `MultipartFile`，输入改用 `InputStream`，输出使用真实 PNG/JPEG 编码字节，由阶段 6.4 Controller 适配上传/下载；修正 BGR → BufferedImage 时红蓝通道颠倒的问题，图像读取错误显式抛出。
- **原生资产不入模块 JAR**：DLL/SO/`.csta` 由外部配置目录提供；未复制到新模块，避免将模型与二进制重复入库。

**验证（2026-10-10，JDK21）**：

- `FaceSdkContractTest` **12/12 通过**：默认门控/宽扫描、配置绑定与 16 池关闭、自定义代理、无效配置/缺模型失败、Windows/Linux/arch 路径与清单顺序、缺库预检查、池句柄关闭与拒绝继续借用、代理失败行为、BGR/PNG 往返与读取错误。
- `FaceSdkNativeSmokeTest` **2/2 通过，0 跳过**：显式传入旧模块 `conf/seetaface6` 和 `conf/sf3.0_models`，在 **Windows amd64 / CPU / JDK21** 实际加载 24 个库、创建全部 16 种算法句柄、检测器接收合成图、查询特征维度；上下文关闭后 16 个句柄归零，重复 `close()` 和同配置重复加载通过。合计 **14/14，BUILD SUCCESS，MVN_EXIT=0**，日志 `.migration/test-facesdk.log`。常规 CI 未传路径时仅跳过两个 native 冒烟用例。
- 根工程 `mvn -o -B -DskipTests compile` → **44/44 模块 BUILD SUCCESS，MVN_EXIT=0，耗时 15.002 秒**；依赖树 `dependency:tree -Dscope=compile` 同样成功，只有 Boot4/Spring7、commons-pool2、Hutool、Lombok 等技术依赖，无旧工程 starter/cloud/Web/Redis 依赖。日志 `.migration/build-facesdk-reactor.log` / `.migration/deps-facesdk.log`。
- 文件/编码审计：29 SDK 文件 **missing=0**，135 native 签名一致；全部交付文本 UTF-8 往返通过，新模块无旧包/未门控组件/尾随空白/二进制资产，`git diff --check` 通过。

**剩余验收**：Linux ARM/GPU、真实人脸识别准确率/活体识别效果、生产负载未测；当前仓库 Linux 原生包仅 ARM 平台，Linux amd64 需另备匹配库。阶段 6.4 的 Person 表、业务流程/鉴权/Controller 仍待迁移。

#### 5.1.4 `nla-common-socketio` 交付结果（阶段 3.4，已落地）

新建 `nla-common/nla-common-socketio`，登记 `nla-common` reactor 与 `nla-common-bom`，根 POM 增加 `netty-socketio.version=2.0.14`。共 **27 个主 Java 文件 + 3 个测试文件**，接入示例见 [`nla-common-socketio/README.md`](../../nla-common/nla-common-socketio/README.md)。未接入 admin 业务依赖，旧技术与业务源码保持原位。

**范围和逐项取舍**：

- 旧 socket-io 的 **9 个主文件**（原表 11 包含 2 个手动聊天示例测试）全部覆盖：2 个监听接口、Message 和 OutType 迁入 basic；配置/属性重写为 Boot4 自动配置；3 个 Jedis 命名的 RedisTemplate 适配改为 Redisson 工厂、PubSub 与 codec。旧手动示例由新的契约与真实协议测试替代。
- 旧 netty 的 **19 个文件中 15 个有效文件**迁入 `basic.netty` / `core.netty`；MainServer、MainServerHandler、VirtualServerClient、Biz100000039 共 **4 个只有注释的文件不迁入**。保留 Message/TypeOperator、MsgCode/Msg100000039、工厂/业务接口、编解码、客户端/服务端/处理器；TCP 显式启动，不自动开放第二个端口。
- `basic` 无 Spring import；新模块无旧工程 starter、RedisTemplate、静态 SpringUtil、Nacos/OAuth2/JWT 耦合。Lombok 日志统一改 SLF4J。

**关键契约与修正**：

- **默认关闭**：`.imports` 注册 `SocketIoAutoConfiguration`，仅 `socket-io.enabled=true` 生效；组件宽扫描即便指定 `store=redisson` 也不会越过门控。内存模式不需要 Redisson 类，已通过过滤 classloader 测试；Redisson 为 optional 依赖，集群模式需消费方提供 RedissonClient。
- **配置兼容与调整**：沿用 `socket-io` 前缀、port/boss-count/work-count、消息上限及毫秒超时字段；恢复有效 host 配置，增加 context/origin/store/redis-prefix/auto-start。work-count 由旧 100 改为 0（库按 CPU 选择），allow-custom-requests 由 true 改为 false；SO_LINGER 由旧 0 改为 -1，修复鉴权拒绝响应被 TCP reset 截断的问题。`name` 只保留配置兼容，不做服务注册。
- **监听与生命周期**：独立 namespace Bean 和事件引用的 namespace 都注册，根 namespace 也挂载连接/断开监听；同对象去重，冲突 namespace 或重复事件名启动失败。支持 @OnConnect/@OnDisconnect/@OnEvent。注册器延迟到全部单例初始化后执行，SmartLifecycle 随后启动服务器；关闭幂等，Netty 通过 Future 直接抛出的 checked 绑定异常也捕获并清理资源。
- **用户覆盖和鉴权边界**：SocketIOServer/StoreFactory/生命周期可覆盖，配置 customizer 按 Spring 顺序调用；原生 AuthorizationListener Bean 接管握手鉴权。未提供鉴权时沿用库允许连接行为；旧用户/坐席身份、二维码登录、房间命名及 Sa-Token 改造属于后续业务迁移。
- **Redis 存储真实写回**：旧 `JedisSore`/`createMap()` 返回 Hash entries 快照，修改未写回 Redis；现在使用实时 RMap，并对 Map/会话/Topic 加服务前缀。断开删除对应会话；PubSub 忽略自己节点，只移除自己拥有的监听 ID，不关闭共享 RedissonClient。7 类消息路由采用库自带 BaseStoreFactory，可忽略本节点不存在的 namespace，无静态 Spring 上下文。默认独立 Jackson2 codec，已验证 Dispatch/Join、中文 POJO、UUID、LocalDateTime 与 byte[] 往返。
- **TCP 解码与资源释放**：保留原线格式和 CRC16；修复 CRC 头半包越界、粘包跨帧 CRC、未知码残留、负长度和超限。CRC 能力从实际 Message 读取，支持扩展时间头和自定义工厂；默认整帧上限 1 MiB，字符串/列表读取校验长度。NettyServer/NettyClient 可显式关闭并清理连接/线程组，客户端连接超时 5 秒、重连间隔 1 秒；DefaultBizFactory 显式注入 BeanFactory，保留 biz%09d 命名。

**验证（2026-10-10，Windows / JDK21）**：

- `mvn -o -B -pl nla-common/nla-common-socketio -am -Dtest=SocketIoContractTest,SocketIoProtocolTest,NettyContractTest -Dsurefire.failIfNoSpecifiedTests=false test` → **21/21 通过，0 跳过，BUILD SUCCESS**（12 配置/Redis 契约 + 3 本机 Socket.IO 协议 + 6 TCP 测试）。真实 WebSocket 验证 Engine.IO4、中文 typed event/ACK、注解回调、房间广播、独立 namespace 和关闭后的端口释放；真实 Polling 验证允许握手/401 拒绝，真实 TCP 验证编解码往返与绑定失败清理。Redis codec 额外验证 final record、根 UUID 与根 LocalDateTime 类型保留。日志 `.migration/test-socketio.log`。
- 根工程 `mvn -o -B -DskipTests compile` → **45/45 模块 BUILD SUCCESS**。日志 `.migration/build-socketio-reactor.log`。
- `dependency:tree -Dscope=compile` → **BUILD SUCCESS**；netty-socketio 2.0.14、Boot 4.1.1 / Spring 7.0.9、Netty 4.2.17.Final、Jackson2 2.21.5；Redisson 4.7.0 optional。无旧 cloud/starter、Boot2 AOP、Spring Web/RedisTemplate 依赖。日志 `.migration/deps-socketio.log`。
- 文件/编码审计：15 个有效 TCP 源文件逐个匹配，4 个不迁文件确认仅有注释；37 份交付文本 UTF-8 严格往返通过；旧包/静态 SpringUtil/未门控组件/basic Spring import/尾随空白均为 0，`git diff --check` 通过。

**外部验收与限制**：Redis 使用内存模拟客户端及真实 codec 字节往返，尚未执行真实 Redis 多节点、代理层 WebSocket/Polling 联调、Linux 和生产负载测试；Polling 多节点仍需代理会话粘滞。异常退出留下的 Redis 会话数据暂无自动 TTL 清理。网络 JSON support 与 Redis codec 独立，Java 时间等扩展网络类型由消费方 customizer 注册模块。阶段 6 的坐席/公告/二维码监听器及业务鉴权不在本轮技术封装范围。

---

#### 5.1.5 `nla-common-mq` 交付结果（阶段 3.5，已落地）

新建 `nla-common/nla-common-mq`，登记 `nla-common` reactor 与 `nla-common-bom`。旧 `starter-rabbitmq` 的配置/客户端及 common 的 `MqConstant` 由 **5 个主 Java 文件 + 2 个测试文件**覆盖；接入示例见 [`nla-common-mq/README.md`](../../nla-common/nla-common-mq/README.md)。旧源码保持原位，未接入 admin 或迁入二维码业务消费者。

**关键契约与修正**：

- **Boot4 默认门控与依赖边界**：`.imports` 注册 `MqAutoConfiguration`，仅 `nla.mq.enabled=true` 生效，默认关闭；宽范围组件扫描不越过门控。编译依赖为 `spring-rabbit`、`spring-boot-autoconfigure`、Jackson3，不传递引入 Boot AMQP starter；消费方启用时自行引入 `spring-boot-starter-amqp` 或提供 ConnectionFactory/RabbitTemplate。开关只控制本模块，不会关闭消费方另行引入的 Boot AMQP 自动配置与监听器；启用但缺基础设施启动失败。
- **发送链路使用消费方 template**：转换器先于 Boot Rabbit 自动配置注册，`MqClient` 注入消费方 RabbitTemplate，保留 confirm/return、重试及 customizer；simple/direct 监听器使用同一 converter。MessageConverter/AmqpAdmin/MqClient 支持用户 Bean 覆盖，用户 RabbitTemplate 由 Boot 退让。保留 `MqClient(RabbitAdmin)` 手动构造，但该构造仍用 admin 内部模板，完整 Boot 配置需使用注入的客户端或双参数构造。
- **JSON 契约与旧序列化边界**：对象（含 String）出站改 Jackson3 JSON，byte[] 原样；入站识别 JSON、vendor +json、charset、文本和原始字节，传递泛型 conversion hint 与 listener inferred type。具体 DTO/List<DTO> 监听参数不要求发送方 Java 类型头；依赖类型头时须配置 DTO 的完整可信包名。AMQP4 仅精确匹配包名，默认 `cn.com.nla` 不覆盖子包。旧 `application/x-java-serialized-object` 不自动反序列化，返回 bytes，业务迁移须同步 JSON 或提供自定义 converter。
- **拓扑参数分离与失败传播**：保留主要 binding/remove/delete/send API 及 6 个 QR/DLX broker 名称。新增 queueArguments/bindingArguments 双 Map 重载并复制输入；单 Map 兼容重载对 headers 解释为绑定参数，其他类型解释为队列参数，避免原有 DLX 参数重复进入 binding。exchange/queue 默认持久化，queue 非 exclusive/non-auto-delete；按 exchange → queue → binding 声明，异常不再忽略，queue 返回 null 同样失败，不自动回滚前面声明的资源。删除/解绑必须显式调用，模块不自动声明二维码拓扑。
- **延迟与 ACK 边界**：`sendDelay` 第 2 参数明确为 routing key，保留 Integer 并增加 long 毫秒重载，校验 x-delay 范围并使用 AMQP4 `setDelayLong`；delayed exchange 需要外部插件，不能替代 TTL/DLX。普通发送增加 CorrelationData 重载；发送方法返回不等于 broker/消费者完成确认。旧业务显式 basicAck 需消费方配置 MANUAL，本模块不全局改变 ACK。
- **自动声明开关**：`nla.mq.auto-declare` 默认 true，控制本模块 RabbitAdmin 的 autoStartup；false 不禁止显式 binding，也不替代消费方监听容器的声明配置。broker 类型/参数冲突会向上抛出。

**验证（2026-10-10，Windows / JDK21）**：

- `mvn -o -B -pl nla-common/nla-common-mq -am -Dtest=MqContractTest,MqClientTest -Dsurefire.failIfNoSpecifiedTests=false test` → **20/20 通过，0 跳过，BUILD SUCCESS，MVN_EXIT=0**（12 配置/转换契约 + 8 拓扑/发布测试）。覆盖默认关闭/宽扫描、缺基础设施失败、Boot 连接/confirm/return 与 simple/direct MANUAL 配置、Bean 覆盖、真实监听适配器 DTO 转换、类型头可信包、中文/LocalDateTime/泛型 JSON、文本/字节和旧序列化边界、QR/DLX/headers 参数分离与失败传播。真实 RabbitTemplate 经模拟 AMQP Channel 验证 persistent JSON、mandatory 与 `3_000_000_000L` x-delay，连接/通道关闭已验。日志 `.migration/test-mq.log`。
- 根工程 `mvn -o -B -DskipTests compile` → **46/46 模块 BUILD SUCCESS，MVN_EXIT=0，耗时 17.393 秒**。日志 `.migration/build-mq-reactor.log`。
- `dependency:tree -Dscope=compile` → **BUILD SUCCESS**；Boot 4.1.1 / Spring 7.0.9、Spring AMQP 4.1.1、RabbitMQ client 5.30.0、Jackson3 3.1.5、Netty 4.2.17.Final。无旧工程 starter/cloud、Boot2、Redis、Spring Web 依赖；AMQP starter 仅用于本模块测试。日志 `.migration/deps-mq.log`。
- 文件/编码审计：6 个旧 broker 常量逐项一致；13 份交付文本严格 UTF-8 往返通过，basic 无 Spring import；旧包/静态 SpringUtil/未门控组件/尾随空白/交付二进制资产均为 0，`git diff --check` 通过。

**外部验收与限制**：测试未连接真实 RabbitMQ；真实 broker 拓扑/重连、delayed-message 插件投递、confirm/return 回调、业务 ACK/DLX 与二维码登录流程尚未联调。业务消费者迁移留阶段 6，本轮不迁入 JWT/Redis/Socket.IO 调用。阶段 3 当前 **5/6**；pay 沿用最低优先级、暂未开发的约定。

---

### 5.2 阶段3 兼容性尖峰结论（freeswitch-esl / SIP 栈，JDK21+Boot4）

**背景**：阶段3 的自建封装是 6.4~6.7 硬前置，其中 freeswitch / video 依赖外部协议库，能否在 JDK21+Boot4 存活是最大未知。本轮先做**静态兼容性尖峰**（依赖可拉取性 + 字节码版本 + jakarta 迁移面 + JDK 内部 API + 自动配置机制），退役未知后再投入完整封装。所有 artifact 已在本地仓（旧项目构建残留），无需下载；证据取自 `javap` / `jar tf` / `findstr` 实测。

| 库 | 坐标:版本 | 字节码 | javax/jakarta 面 | 结论 |
|---|---|---|---|---|
| FreeSWITCH ESL 核心 | `link.thingscloud:freeswitch-esl:1.6.7.RELEASE` | **52 (Java8)** | **0 处 `javax/*`**（纯 netty ESL 客户端，53 类） | ✅ **GO**：Java8 字节码 JDK21 向后兼容可加载 |
| FreeSWITCH ESL Starter | `link.thingscloud:freeswitch-esl-spring-boot-starter:1.6.7.RELEASE` | 52 | 仅 `META-INF/spring.factories`（**Boot2 机制，无 `.imports`**） | ⚠️ 自动配置在 Boot4 **不会被加载**；但方案本就**自建 `nla-common-freeswitch` 只依赖核心库 + 自写 `@AutoConfiguration`**，不复用其 starter → 已规避 |
| JAIN-SIP RI | `javax.sip:jain-sip-ri:1.3.0-91` | **51 (Java7)** | 无 servlet/xml.bind/annotation 等；`javax.sip`/`javax.sdp` 是**独立 JAIN 规范命名空间**（从未迁 jakarta、不冲突）；**0 处 `sun.misc`/`sun.security`** | ✅ **GO**：无 JDK21 强封装风险 |
| ONVIF | `be.teletask.onvif:onvif:1.0.2` | **52 (Java8)** | **无 JAXB**（`javax.xml.bind` 已从 JDK11 移除）——自带 `OnvifXMLBuilder`/`parsers` 处理 XML | ✅ **GO**：不触雷 JAXB 缺失 |

**关键发现**：
1. **freeswitch-esl starter 是 Boot 2.5 产物**（parent `freeswitch-esl-all` 锁 `spring-boot 2.5.0`、`java 1.8`、`netty 4.1.65`），自动配置走 `spring.factories`。Boot 3+ 已移除 `spring.factories` 的 `EnableAutoConfiguration` 支持 → 其 `FreeswitchEslAutoConfiguration` 在 Boot4 不自动生效。**对策**（与计划一致）：`nla-common-freeswitch` 依赖**核心库 `freeswitch-esl`**（非 starter），自写 basic+core 二段式 + 自有 `@AutoConfiguration`。
2. **netty 版本落差（残留风险 LOW-MED）**：Boot 4.1.1 BOM 管理 **netty 4.2.17.Final**，而 freeswitch-esl 核心按 **netty 4.1.65** 编译（旧项目用 4.1.53）。netty 4.1→4.2 常见 transport/ByteBuf API 兼容，但 4.2 改了默认 buffer 分配器等 → 建 `nla-common-freeswitch` 时需编译 + ESL 连通实测确认。
3. **log4j 1.x 需排除**：freeswitch/video 旧 POM 直接依赖 `log4j:log4j:1.2.17`（EOL、含 CVE）。自建封装时 **exclude 掉**，统一走 Boot 的 log4j2/logback。

**尖峰未覆盖（延后）**：**运行时协议连通**（ESL 登录 FreeSWITCH、SIP 注册/心跳/点播）需真实服务器，本地无 FreeSWITCH/SIP 端点，**留到建 `nla-common-freeswitch`/`nla-common-video` 时有环境再冒烟**（对齐计划阶段3 验收「三个含 native/外部协议模块 JDK21 连通性冒烟」）。

**净结论**：freeswitch-esl 核心 + jain-sip-ri + onvif 三者**静态层面 JDK21+Boot4 全部 GO**，最大未知已退役；6.6 fs(41表) 与 6.5 video(12表) 无底层库阻塞，可按计划自建封装推进。

### 5.3 阶段3 兼容性尖峰结论（facesdk JNI / pay 8 SDK，JDK21+Boot4）

**背景**：承接 5.2，退役阶段3 剩余两个未知——facesdk（JNI native，比纯 Java 协议库风险更高）与 pay（8 个老支付 SDK）。证据取自源码 `findstr` 扫描 + `javap` 字节码 + `jar` 解包实测，artifact 均已在本地仓。

#### 5.3.1 facesdk（`com.seeta.sdk` 内嵌源码 + JNI）

| 项 | 实测 | 结论 |
|---|---|---|
| 源码位置 | `spring-boot-face/.../com/seeta/sdk`，**29 个 `.java`** 内嵌（非外部 jar），包名须保持 `com.seeta.sdk`（native 库按此包名做 JNI 符号绑定） | ✅ 迁移即整包搬入 `nla-common-facesdk`，不改包名 |
| native 库 | Linux `.so`(aarch64) + Windows `.dll`(amd64) + JNI 桥接库齐备；14 个 `.csta` 模型 | ✅ **双平台齐备**——Windows 开发机可本地冒烟（区别于 freeswitch/SIP 无端点） |
| 加载机制 | `LoadNativeCore.LOAD_NATIVE(dllPath, SeetaDevice)`：读 `dll.properties`→按 OS(`os.name`)+arch(`os.arch`)定位→`System.load(绝对路径)` 逐个加载；导入**纯标准 JDK**（`java.io`/`nio.file`/`util`/`util.logging`/`stream`），**0 处 `javax/*`、`sun/*`、removed API** | ✅ **GO**：`System.load` 机制 JDK21 无变化 |
| native 方法 | **135 个实际 `native` 方法**（阶段 3.3 校正：原 137 含 2 行注释声明）；JNI ABI 与 JDK 版本解耦 | ✅ 签名逐文件一致，见 5.1.3 |
| `finalize()` | 18 处；Java9 起废弃但 **JDK21 仍功能正常**（仅编译告警）。旧池销毁仅 `object=null`，没有显式释放 | ✅ 阶段 3.3 已补幂等 `close()` 与池/代理关闭，`finalize` 留兼容兜底 |
| `javax.imageio`/`javax.swing` | 仅 `SeetafaceUtil.java` 用到——二者是**永久 JDK 包**（`java.desktop` 模块），**不迁 jakarta、不冲突** | ✅ GO |

**facesdk 净结论**：✅ **GO**。阶段 3.3 已完成 Windows amd64 CPU / JDK21 真实 JNI 冒烟（见 5.1.3），6.4 face 的技术前置已就绪。Linux/GPU 验收与基础镜像 `seetaface_face_work` 的 JDK21 重建留对应部署阶段。

#### 5.3.2 pay（8 子模块：alipay/jdpay/qqpay/unionpay/wxpay/paypal/xpay/starter）

**外部 SDK 面比计划 L408 估计的小**——实测只有 2 个真实外部支付库 + servlet-api，其余子模块（jdpay/qqpay/unionpay/wxpay/paypal/xpay）**仅依赖内部 `pay-starter` + IJPay-Core**，无各自外部 SDK。

| 库 | 坐标:版本 | 字节码 | javax/jakarta 面 | 结论 |
|---|---|---|---|---|
| IJPay 核心 | `com.github.javen205:IJPay-Core:2.9.11` | **52 (Java8)** | 23 类；仅 `HttpKit.getRequest()`+`IpKit.getIp(HttpServletRequest)` 碰 `javax.servlet`；`javax/crypto`/`imageio`/`net.ssl`/`xml` 均**永久 JDK 包**；**无自动配置文件**（纯 kit 库，无 spring.factories/.imports） | ⚠️ **条件 GO**（见下） |
| 支付宝 SDK | `com.alipay.sdk:alipay-sdk-java:4.39.42.ALL` | **50 (Java6)** | 24005 类（纯 `com.alipay` API 模型集，**未 shade** fastjson/httpclient）；`javax/crypto`/`security`/`xml` 均永久 JDK 包；**0 处 risky javax、0 处 `sun.*`** | ✅ **GO** |
| fastjson | `com.alibaba:fastjson` | — | 老 pay 源码**未 import** `com.alibaba.fastjson`（仅 alipay 传递，且 POM 已 `<exclusion>`） | ✅ **出范围**（pay 不用） |
| servlet-api | `javax.servlet:javax.servlet-api` | — | 父 POM + IJPay 均依赖 `javax.servlet`（IJPay 中为 `provided` scope，不传递） | ⚠️ **须迁 `jakarta.servlet`** |

**关键发现**：
1. **javax.servlet 是唯一迁移面，且可绕过**：老 pay 大量调用的 `HttpKit.getDelegate().post/get/...`（出站 HTTP 客户端）**完全不碰 servlet**；碰 servlet 的只有 ①IJPay 的 `HttpKit.getRequest()`/`IpKit`（2 helper，`provided` scope 不传递），②老封装 `AliPayApi.toMap(HttpServletRequest)`（1 处）。JVM 懒解析下，只要**不调用**这些 servlet 方法，`HttpKit` 类照常加载、`getDelegate()` 正常工作。**对策**：`nla-common-pay` 封装**不用 `HttpKit.getRequest()`/`IpKit`**，回调读参改用 Boot4 的 `jakarta.servlet.http.HttpServletRequest`（controller 层已有）→ 手工转 Map，与 alipay notify 对接。`javax.servlet-api` 依赖删除（Boot4 web 已提供 jakarta.servlet-api）。
2. **bouncycastle 建议升级**：IJPay-Core 传递 `bcprov-jdk15on:1.70`；本地仓已备 `bcprov-jdk18on`/`bcpkix-jdk18on`/`bcutil-jdk18on`。JDK21 下建议 exclude `jdk15on` 换 `jdk18on`（维护线），建封装时验证签名/加解密。
3. **IJPay 传递依赖**（非 fastjson）：zxing 3.5.3（二维码）、slf4j-api 2.0.8（provided）、xk-time 3.2.4、bouncycastle。均 JDK21 可用。

**pay 净结论**：✅ **GO（有条件）**。alipay-sdk-java 与 IJPay-Core 字节码 JDK21 向后兼容可加载，0 处 JDK 内部 API；唯一 `javax.servlet` 面在封装层绕过即可（不走 IJPay servlet helper，改用 jakarta）。6.7 pay(0表) 无底层阻塞——但按用户决策 pay「保持暂未开发现状」，`nla-common-pay` 优先级最低。

**尖峰未覆盖（延后）**：pay 的**运行时联调**（支付宝/微信真实下单、回调验签）需商户密钥 + 外网，本地无凭据，留到实际开发 pay 时冒烟。

---

### 5.4 阶段 5.1：登录与客户端访问契约（已交付）

在现有 Sa-Token 基座上落实认证入口、JWT 客户端绑定、方法级权限和社交解绑归属。请求/响应、公开端点和验证边界详见 [authentication.md](authentication.md)。新工程沿用 `/auth/login` 与 `Authorization: Bearer <access_token>` / `clientid`，不新增旧 `/oauth/token` 兼容入口，不迁旧账号、token 或租户测试数据。

**本批改动**：

- `AuthController`：授权类型按逗号分隔的完整名称匹配，拒绝子串和组合策略名；类级 `SaIgnore` 改为 login/register/authBinding/logout 四个方法的公开注解，社交回调与解绑经过统一登录、客户端和路径/IP 检查。
- `SecurityConfig`：沿用 `AllUrlHandler.getUrls()` 的基线路径匹配和排除配置；请求头/参数中至少提供一个 `clientid`，且每个值都须与 token 一致，冲突、空值和缺失扩展字段返回既有 401 业务码。
- `ISysSocialService` / `SysSocialServiceImpl`：新增按绑定 ID 与当前用户 ID 的同一条 SQL 条件删除，拒绝他人绑定或缺失条件，保留通用内部删除 API；当前用户从登录会话取得。
- `nla-system` 的 H2 仅用于 test，测试资源只复制交付 `nla_system.sql`，不改变生产数据库配置或执行种子数据。

**本批验收**：

- `AuthSecurityContractTest` **37 项**：真实 MVC 拦截器、AllUrlHandler、JWT 简单模式、LoginHelper、权限实现及异常处理器；覆盖授权类型、客户端状态/冲突、公开端点、已注册路径（含单段路径变量）、菜单/角色、Cookie、非法/撤销 JWT、雪花 ID 精度、超时、路径/IP 策略及解绑用户传递。密码策略、用户/客户端和外部服务是测试替身。
- `SysSocialOwnershipTest` **4 项**：从交付 SQL 提取 sys_social DDL，以 H2 MySQL 模式和真实 Mapper 验证本人/他人/空条件/删除前重新归属；只使用内存测试数据。
- 加上既有 `SecurityConfigTest` 3 项与 `SaTokenFunctionTest` 6 项，本批 **50 项全通过，无失败、错误或跳过**，日志 `.migration/test-auth-security-contract.log`。
- JDK21 根工程 `mvn -o -B -DskipTests compile` → **49/49 模块成功**，日志 `.migration/build-auth-security-reactor.log`。
- `nla-admin -am` 编译依赖过滤检查未出现 H2，确认新增依赖保持 test scope，日志 `.migration/deps-auth-security.log`。

**边界与后续进度**：错误仍沿用 HTTP 200 + `R.code` 的基线响应；客户端路径/IP 策略是登录时 token 快照，未实现配置即时刷新。真实密码/验证码/重试锁定/账号状态及旧短信、小程序、二维码登录映射已在阶段 5.2 验证与记录（见 5.5）；第三方真实绑定、Redis 会话/权限刷新、生产代理和完整应用启动未验收。方法级权限属于架构基线变更，当前多租户保持禁用，企业 companyId 不代替登录权限，pay 继续暂缓。

### 5.5 阶段 5.2：登录策略验证与旧登录功能映射（已交付）

完整请求字段、失败计数规则、测试边界和旧功能缺口见 [login-strategies.md](login-strategies.md)。本批沿用现有密码/短信/邮箱策略；生产代码只在 `LoginHelper.fillRequestContext` 增加两处解析结果空值判断，修复请求缺失 User-Agent 时正确密码/验证码登录空指针。`AllUrlHandler.getUrls()` 路径匹配保持基线。

**验证结果**：

- 新增 `LoginStrategyContractTest` **41 项全通过**：真实 BCrypt、图形验证码 math/char 图片生成与答案校验、顺序消费/重放拒绝、密码请求约束、缺失/停用账号、第五次错误锁定、正确凭证不能绕过锁定、过期模拟、账号计数隔离与密码/短信/邮箱共享锁定、失败事件发布、虚拟线程权限组装、JWT 会话及可选 User-Agent。
- 与阶段 5.1 的 50 项共同运行，**91 项全通过，无失败、错误或跳过**。日志 `.migration/test-login-strategy-contract.log`。
- JDK21 根工程 `mvn -o -B -DskipTests compile` → **49/49 模块成功**，日志 `.migration/build-login-strategy-reactor.log`。
- Mapper、权限/部门/角色/岗位数据源和 Redis 操作使用替身，Sa-Token DAO 为内存实现；测试核对缓存 Duration 并移除条目模拟过期，未验收真实 Redis TTL/多实例并发、生产日志落库、MySQL 或供应商。

**功能映射结论**：旧 `code` 登录由新 `password` + 图形验证码覆盖；旧短信参数 phone/SmsCodeCode 改为 phoneNumber/smsCode。5.2 时发现表驱动发送与基线短信登录使用不同 key，且 `/resource/sms/code` 使用固定 config1/空模板号；这些短信缺口已在 5.3 接通（见 5.6）。5.2 时 `XcxAuthStrategy` 为密钥/用户查询占位模板，5.4 已补小程序配置、账号绑定和登录（见 5.7）；旧用户资料/手机号处理和 wx_mini_web 网页登录尚未等价实现。二维码旧链路还依赖微信码生成、Redis 场景、MQ 通知和 Socket.IO 房间推送，不能以技术封装就绪认定业务完成。

**后续**：阶段 5.3/5.4 已交付，见下；二维码状态/通知链路、小程序资料及手机号授权另批实施。旧账号/token/租户数据不迁，原 mini 表不重建，阶段 5 保持进行中，pay 继续暂缓。

### 5.6 阶段 5.3：表驱动短信发送与登录验证码契约（已交付）

完整 API、模板配置、缓存与消费时机见 [sms-login.md](sms-login.md)。`CaptchaController.smsCode` 调用新增 `SmsLoginCodeService`，检查手机号及账号可用性后调用 `nla-message/SmsSendManager`；渠道与模板来自业务表，保持内部服务调用和现有公共 URL，无新增通用短信发送接口。

- 发送与短信登录共用 `SmsConstant.verificationCodeKey`，登录 key 为 `redis:verificationCode:1_{phoneNumber}`；按用途隔离，不双写旧图形/邮箱验证码 key。
- 模板验证码值为空时由 SecureRandom 生成 6 位数字，缓存分钟数为空默认 5。登录模板缺少验证码/正期限、类型不匹配或变量非法时跳过，不发送不可校验的短信。供应商失败不写验证码，继续故障转移，沿用最终尝试落发送记录。
- 缓存命中保持原码、防止重复发送，继续返回成功与等待提示；原公共接口限流和 HTTP 200 + `R.code` 契约保留。登录错误输入保留验证码并递增既有账号重试计数；缺失/停用账号和锁定账号不能消费或建立会话。
- 答案匹配后通过同一 Redisson 客户端的 `RBucket.compareAndSet(code, null)` 原子比较删除，只有消费成功的请求继续签发 JWT；消费竞争失败拒绝，不误删替换后的新码。消费发生在权限组装与签发之前，后续失败需重新获取验证码。

**验证结果**：`LoginStrategyContractTest` 新增 **32 项**、合计 **73 项**；与 5.1 回归及 `SmsDataContractTest` 的 8 项共同执行，**131 项全通过，无失败、错误或跳过**。根工程 JDK21 离线编译 **49/49 模块成功**。日志 `.migration/test-sms-login-contract.log`、`.migration/build-sms-login-reactor.log`。

**验收边界**：测试执行真实发送/认证逻辑与 JWT，用户/渠道/模板查询、记录 Mapper、供应商、Redis 使用替身；已有 H2/Mapper 数据层测试共同回归。过期及消费竞争通过替身模拟，未验收真实 Redis/MySQL/供应商、限流切面或完整启动。原 synchronized 仅保障单实例发送串行，多实例发送去重和基线账号计数并发仍待环境/后续处理。`SecurityConfig`/`AllUrlHandler` 路径匹配沿用原逻辑，图形验证码、邮箱和 video 数据层不改；多租户禁用，pay 暂缓。小程序绑定/登录后续已在 5.4 交付，二维码业务及外部验收仍待推进。

### 5.7 阶段 5.4：微信小程序登录与账号绑定（已交付）

配置、请求与绑定规则见 [xcx-login.md](xcx-login.md)。新增 `XcxProperties`（默认关闭、appid→密钥/客户端白名单）、`WechatMiniClient`（JustAuth 凭证交换边界）、`SysXcxBindingService`（现有 sys_social 绑定）与受保护的 `/auth/xcx/bind`；`XcxAuthStrategy` 移除密钥和用户查询占位，实现唯一绑定账号登录。

- 登录沿用 `/auth/login`、grant `xcx`、客户端启用/授权检查；appid/code 增加输入约束。绑定账号来自 token，openid 来自服务端微信响应，不信任前端 userId/openid。未配置/停用的小程序或不在白名单的客户端不请求微信。
- source=`WECHAT_MINI_PROGRAM:{appid}`、auth_id=`{source}:{openid}`，按 appid 隔离；新查询过滤删除标志并精确比较 openid，防默认排序规则忽略大小写。未绑定、重复/损坏绑定、缺失/停用用户及共享账号锁定均拒绝，不选第一条绑定。
- 绑定采用按 appid 的 Lock4j 锁，同一身份重绑幂等，禁止抢绑他人或静默换绑；解绑继续按当前用户归属执行既有 SQL。锁是本服务的协作约束，未新增数据库唯一索引，绕过服务产生的重复绑定会导致登录拒绝。
- 小程序上下文复制真实 `buildLoginUser` 的部门、角色、岗位、权限与数据范围，再增加 appid/openid；客户端有效期和路径/IP 参数继续沿用基线。unionId 可缺失，只作元数据，不跨 app 自动合并账号；session_key 不落库/会话/响应。

**验证结果**：本批新增 **48 项**（供应商/配置 20、真实 H2/Mapper 绑定 12、登录策略 12、MVC 4）；与既有认证与短信数据层回归共同运行，**179 项全通过，无失败、错误或跳过**。JDK21 根工程 **49/49 模块编译成功**。日志 `.migration/test-xcx-contract.log`、`.migration/build-xcx-reactor.log`。

**边界与下一批**：微信请求、用户/权限数据源与 Redis 用替身；H2 验证实际绑定 SQL、大小写候选过滤与解绑归属，未执行真实 Redis 锁、多实例竞争、微信 code 一次性消费、真实 MySQL 或完整启动。小程序资料/手机号授权及自动注册尚未交付。二维码后端后续已在 5.5 接通，通知改为 REST 轮询（见 5.8），旧 MQ/Socket.IO 业务事件没有迁入。旧 mini 表/账号/token 数据不迁，SecurityConfig/AllUrlHandler 路径匹配沿用原逻辑，多租户禁用，pay 暂缓，阶段 5 仍进行中。

### 5.8 阶段 5.5：小程序扫码网页登录（已交付）

配置、前端调用顺序和验收边界见 [qr-login.md](qr-login.md)。新增 `QrLoginController`、`QrLoginService`、`QrSceneStore`、`WechatQrClient`、`QrAuthStrategy` 及二维码请求模型；只扩展已有 `XcxProperties`，不修改路由基座、旧代码或数据库种子。

- 网页客户端需完整授权 `qr` grant；小程序端需 `xcx` grant，扫码会话必须是同 appid 的 `XcxLoginUser`。小程序客户端和可扫码登录的网页客户端分别配置白名单，二维码能力默认关闭。
- 微信 stable_token 与不限次数小程序码调用使用服务端密钥、固定 HTTPS 地址和受限的响应/超时；供应商错误不透传。生成失败不创建场景，二维码只编码 128 位随机 scene，独立 256 位 browserToken 只交付网页，Redis 仅保存其 SHA-256。
- 场景有效期 180 秒。WAITING → SCANNED → CONFIRMED/CANCELLED → CONSUMED 通过 Redis Lua 比较原 JSON 与正数 PTTL 后原子迁移，保留剩余 TTL；过期不能复活，重扫/查询不续期。第一个扫码者占有场景，必须显式确认，重复确认和他人覆盖拒绝。
- 网页轮询 POST `/auth/qr/status`，必须提供对应 browserToken/clientId，只返回阶段。CONFIRMED 后通过既有 POST `/auth/login` 的 `qr` 策略一次性领取 JWT；领取重查客户端、绑定、账号/共享锁定与完整权限，终端信息取网页请求。先 CAS 至 CONSUMED，再签发，失败不重开场景。
- 本单体用 REST 轮询完成网页通知，不新增旧 QR MQ 拓扑、消费者、Socket.IO 房间或令牌推送。前端必须按新接口适配；这是通知方式变更，旧 MQ/Socket.IO 事件接口没有等价迁入。

**验证结果**：本批新增 **51 项**（登录策略 30、MVC 5、供应商/配置/响应限长 12、Redis 适配器 4）；与此前 179 项共同运行，**230 项全通过，无失败、错误或跳过**。JDK21 根工程 **49/49 模块编译成功**。日志 `.migration/test-qr-contract.log`、`.migration/build-qr-reactor.log`。

**验收边界**：真实服务/策略/权限组装/JWT/MVC、供应商协议和 Redis codec/CAS 参数已验证；微信网络、用户/权限/客户端/绑定数据源及 Redis 为替身，过期与竞争用场景移除/CAS 失败模拟。尚未执行真实 Redis Lua、多实例竞争、微信码扫描、前端确认/轮询、生产代理或完整应用启动；既有 H2 数据层共同回归。客户端重新查询仍沿用现有缓存失效机制，直接 SQL 修改和领取期间并发撤权不承诺即时生效。小程序资料/手机号授权、自动注册、第三方真实授权及其他外部验收仍待推进，阶段 5 保持进行中，多租户禁用，pay 暂缓。

---

## 6. 业务模块映射

| 批次 | 源模块（文件数） | 目标模块 | 要点 | 状态 |
|---|---|---|---|---|
| 6.1 | `bean`(98) + `sys` 实体 | 合并进 `nla-modules/nla-system` | 基线覆盖复核后**仅 `sys_area` 需迁**（mini/mini_user→`sys_social`+`sys_user`、user_set→`sys_user.status`+超管角色，不新建）；老数据丢弃、流B 数据迁移取消；表映射见下，交付见 6.1.1 | ✅ 完成 |
| 6.2 | ~~`oa`(6) + `Leave`~~ | **废弃·不迁移** | 请假流程已由 `nla-workflow` 的 `TestLeave`（请假 + WarmFlow）完整覆盖，功能重复；旧 `oa_leave` + Activiti 弃用，详见 6.4 | ✅ 废弃 |
| 6.3 | `sms`(47) | `nla-modules/nla-message` | 3 表(`SmsConfig`/`MobileMessageTemplate`/`MobileMessage`) CRUD + 表驱动适配层(`DbSmsReadConfig`/`SmsChannelManager`/`SmsSendManager`)；4 SPI 供应商(dxw/swlh/wnd/wyyd)下沉 `nla-common-sms`，aliyun/tencent/cloopen 复用 sms4j 内置；创蓝网(`smsType=2`)废弃、`PublicNotice`→`SysNotice` 免迁、`Quartz`→`nla_job` 废弃、`ReadNoticeUser` 随公告已读机制暂缓；DDL 见 `script/sql/nla_message.sql`。详见 6.6.1 | ✅ 完成 |
| 6.4 | `face`(116) | `nla-modules/nla-face` | 实测 16 Pool + 16 Proxy 对象池包装 JNI，**技术前置已就绪（见 5.1.3），Person 数据层已建（见 4.4）** | 业务待推进（数据层已就绪） |
| 6.5 | `video`(80) | `nla-modules/nla-video` | video 设备/通道/录像/云台，**前置 `nla-common-video` 已就绪（见 5.1.2），12 表数据层已建（见 4.6）**；video.MediaServer 已消歧为 VideoMediaServer | 业务待推进（数据层已就绪） |
| 6.6 | `fs`(196, 41 表) | `nla-modules/nla-callcenter` | 最大业务模块；沿用 `service/manage/{group}` 纯接口 + 构造器注入（与基线风格天然一致）；`*SaveParam`→`*Bo`；**前置 `nla-common-freeswitch` 已就绪（见 5.1.1），41 表数据层已建（见 4.7）** | 业务待推进（数据层已就绪） |
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

bean 独有表初判 4 张（`bean_user_set`/`bean_mini`/`bean_mini_user`/`sys_area`），**基线覆盖复核后仅 `sys_area` 为真实缺口需迁**：mini/mini_user 由 `sys_social`+`sys_user` 覆盖、user_set 由 `sys_user.status`+超管角色覆盖，均不新建。详见 6.1.1。

#### 6.1.1 批次 6.1 交付结果（已落地）

**范围收窄（关键决策）**：6.5.1 初判 bean「需迁 4 表」（Mini/MiniUser/UserSet/Area）。实施前对基线 22 个 `sys_*` 逐一做覆盖复核，据用户「如果没有的则需要迁移」指令，**真实缺口仅 `sys_area` 1 表**：

| 旧表 | 基线等价物 | 结论 |
|---|---|---|
| `bean_mini`（小程序 openId/nick_name/avatar_url/phone/gender/login_last_time） | `sys_social`（openId/userId/nickName/avatar/source/unionId…）+ `sys_user`（phonenumber/sex/login_date） | 已覆盖，**不新建** |
| `bean_mini_user`（小程序↔用户绑定 userId） | `sys_social.user_id` 绑定关系 | 已覆盖，**不新建** |
| `bean_user_set`（is_admin/is_enabled，1:1 附设置；旧 `UserSetServiceImpl` 为空实现、无独有逻辑） | `sys_user.status`（启停）+ 超级管理员角色（is_admin） | 已覆盖，**不新建** |
| `sys_area`（国标行政区划参考数据） | 基线无 | **缺口 → 迁移** |

**老数据一律丢弃（流B 取消）**：用户明确「老数据一概不要了，全按照新的框架走，都是些测试数据」。故 6.5.2 洞察3 与 6.5.3 所列「bean 数据迁移子任务」（存量 user/dept/role/menu/dict 迁到基线 `sys_*`、密码 Shiro 盐值→BCrypt、菜单 Arco varchar-id 模型→RuoYi M/C/F、role_menu 逗号列表→一行一对）**整条取消**，不做流B 数据迁移。

**`sys_area` 迁移实现（只读参考表）**：
- **实体特殊处理**：国标行政区划为只读、批量导入的参考数据，故 `SysArea` **不继承 `BaseEntity`**（无审计列），`@TableId(value="area_id", type=IdType.INPUT)`（地区Id 由外部国标数据提供，非雪花）；
- 分层齐全：`SysArea`/`SysAreaBo`（纯查询，无 `@AutoMapper`）/`SysAreaVo`（`@AutoMapper`）/`SysAreaMapper`（`BaseMapperPlus`，**无 `@DataPermission`**——全局参考数据）/`ISysAreaService`+`SysAreaServiceImpl`/`SysAreaController`；
- **迁移旧 `AreaServiceImpl` 三段核心逻辑**：`findAreaAll`(树)→`selectAreaTree`（改用基线 `TreeBuildUtils.buildMultiRoot`）、`findAllAreaName`(名称图)→`selectAreaNameMap`（`@Cacheable(CacheNames.SYS_AREA)` + `StreamUtils.toMap`）、`findAddress`(地址拼接)→`findAddress`（`SpringUtils.getAopProxy(this)` 命中缓存）；
- Controller `/system/area` 4 端点**只读**：`list`/`tree`/`{areaId}`/`address`，权限 `system:area:list`（list/tree）+ `system:area:query`（getInfo/address）；无写入故不加 `@Log`/`@RepeatSubmit`；
- `CacheNames` 新增 `SYS_AREA = "sys_area#30d"`。

**DDL** `script/sql/nla_system_ext.sql`（独立新文件，不追加到 91.9KB 基线 `nla_system.sql`，保持基线种子干净、便于上游再同步）：`sys_area` 建表（`area_id` 主键无自增 + 4 索引 `idx_area_code`/`idx_parent_id`/`idx_level`/`idx_area_name`）+ 菜单权限（「行政区划」C 菜单 `1761400000000002100` + 查询 F 按钮 `1761400000000002101`，挂系统管理 `1761400000000000001` 下 `order_num=12`）+ 2 条 `sys_role_menu` 授权（普通角色 `1761300000000000003`）；号段 `2100` 与短信号段 `2000`（`nla_message.sql`）无冲突。

**延后项**：`sys_area` 国标数据批量导入（旧库 67 万余行 / 91MB）延后处理，不随脚本入库；Easy-ES 全文检索（旧 `bean_sys_area` 索引）延后至阶段4（`nla-common-elasticsearch`）再评估。

**验证**：`nla-modules/nla-system -am` 编译 `MVN_EXIT=0`；`SysAreaMapper` 的 IDE 自动装配告警为 MyBatis `@MapperScan` 运行时注册的静态分析假阳性，与基线全部 mapper（如 `SysPostMapper` 同样无 `@Mapper` 注解）一致。

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
  - 删除前引用校验（采用 §4.3 逻辑删除仍保留，`return R.fail("请先删除xxx")`）
  - 业务表按 §4.3 使用 String delFlag 逻辑删除，不建物理外键
  - 密码/密钥只写不读；手机号脱敏

### 6.4 `oa` 废弃决策（批次 6.2 终止）

**结论：`oa` 不迁移，旧 `spring-boot-oa` 废弃。** 本轮已建的 `nla-oa` 骨架（`pom.xml` +
`nla-modules/pom.xml` 的 `<module>` + `nla-admin/pom.xml` 依赖）已全部回退干净，磁盘无残留。

**理由——功能重复**：旧 `oa` 全部内容就是一张请假表 `oa_leave`（6 个 Java 文件：`Leave` 实体 +
`LeaveParam` + `LeaveMapper` + `LeaveService/Impl` + `LeaveController`，5 接口
find/insert/updateState/update_process_instance_id/delete）+ 一条 Activiti 请假流程。而基线
`nla-workflow` 的 **`TestLeave` 已经是「请假 + WarmFlow」的完整实现**
（`TestLeaveServiceImpl.submitAndFlowStart` 走 `WorkflowService.startCompleteTask`，外加
processHandler/processTaskHandler/processDeleteHandler 三个 `@EventListener`），业务与流程都比旧
oa 更完整、更先进。旧 `oa_leave` + Activiti 属被上游示例覆盖的重复功能，无迁移价值。

**附带核查成果——旧 `spring-boot-*` → 新 `nla-*` 迁移映射规则（对 6.1/6.3~6.7 后续批次直接复用）**：

| 项 | 旧基类/写法 | 新基线 | 迁移规则 |
|---|---|---|---|
| 实体基类 | `LongIdEntity`（`@TableId AUTO` 自增）+ `Base` | `BaseEntity`（**无 id、无 tenantId**） | 实体自带 `@TableId(value="id") Long id`（雪花，DDL 去 `auto_increment`）；审计 5 列 create_dept/create_by/create_time/update_by/update_time 自动填充 |
| 时间类型 | `java.util.Date` + `@DateTimeFormat`/`@JsonFormat` 双注解 | `LocalDateTime` | 直接换类型，去掉两个格式注解（基线用全局 Jackson 配置） |
| 租户 | `tenant_id`（默认公共租户 1L） | **基线全局禁用多租户**（application.yml 无 tenant 配置、`sys_user` 无 `tenant_id`） | 旧实体 `tenantId` 字段一律去掉，不留孤立租户列 |
| 当前登录人 | `JwtUtils.getUserId()` + Feign（`UserServiceFeign.getInfo` + `findUserConnectDepartment`） | `LoginHelper.getUserId()/getUsername()/getDeptId()/getDeptName()`（从 SaToken 会话直接取） | 用户/部门填充改 `LoginHelper`，**彻底消除这类 Feign 调用** |
| 校验分组 | `BaseModel` 内部注解 `add/edit/delete`（小写） | `AddGroup/EditGroup`（大写，jakarta.validation） | `@Validated(AddGroup.class)` |
| 流程类业务 | Activiti + `process_instance_id`/`state` 字段 | WarmFlow + `BusinessStatusEnum`（DRAFT/WAITING/FINISH…） | 走 `nla-workflow` 的 `TestLeave` 模式（归阶段 7） |

**旧 `spring-boot-oa` 处置**：按分级清理策略保留到阶段 6 全部完成后，随其他旧 `spring-boot-*`
业务模块一并删除（本轮不动老项目一行）。`sql/sys_oa-基类sql.sql` 同理，见 9 节。

### 6.5 全量重估：迁移边界重划

**触发**：oa 因「基线 TestLeave 已覆盖」废弃，暴露「凭文件数直觉会严重误判迁移量」。据此对 7 个旧业务模块做两端全景对照（旧实体/表 × 基线已有能力），重划边界。

**方法**：旧实体清单（`springbootentity/dome/{module}` + 模块内 domain）逐一对照基线（`nla-common` 25 starter + `nla-system` 22 个 `sys_*` + `nla-workflow`），判定四类处置：`已覆盖免迁` / `需迁独有` / `需技术封装(阶段3)` / `废弃`。

#### 6.5.1 逐模块处置总表

| 批次 | 模块 | 旧实体 | 已覆盖免迁 | 废弃 | 需迁独有 | 需技术封装 |
|---|---|---|---|---|---|---|
| 6.1 | bean+sys | 22 | 14 | 4 | **4→1**(仅 area,见 6.1.1) | — |
| 6.2 | oa | 1 | — | 1 | 0 | — |
| 6.3 | sms | 6 | 1 | 1 | **3**(+1待定) | **表驱动引擎**整体迁移；初判"非sms4j"，**终采方案B(sms4j 3.3.5 表驱动+SPI)，已落地，见 6.6.1** |
| 6.4 | face | 1+116文件 | — | — | **1**(Person,余待核) | **nla-common-facesdk**(≈64文件JNI) |
| 6.5 | video | 12 | — | — | **12** | **nla-common-video** |
| 6.6 | fs | 41 | — | — | **41** | **nla-common-freeswitch** |
| 6.7 | pay | 0表(7 SDK Bean) | — | — | 0 | **nla-common-pay** |
| 阶段7 | activiti | — | WarmFlow覆盖 | Activiti废弃 | 0 | nla-workflow |

- **6.1 免迁 14**：user/dept/role/menu/post + 4 关联表(user_role/role_menu/user_post/user_dept) + config/dict×2/log/oauthClient → 全部对应基线 `sys_*` 完整 CRUD。
- **6.1 废弃 4**：Tenant/TenantConnectMenu（基线禁用多租户）、DepartmentConnectMenu/PositionConnectMenu（基线用角色授权，不用部门/岗位直连菜单）。
- **6.1 需迁 4→1**：初判 Mini/MiniUser/UserSet/Area，**基线覆盖复核后仅 Area 需迁**（Mini/MiniUser→`sys_social`+`sys_user`、UserSet→`sys_user.status`+超管角色；见 6.1.1）。
- **6.3 需迁 3**：MobileMessage/MobileMessageTemplate/SmsConfig（短信业务层）；PublicNotice→SysNotice 免迁、Quartz→nla_job 废弃、ReadNoticeUser(公告已读)待迁移时判(基线 SysNotice 无已读机制,评估 SysMessage 或简化)。**关键：SmsConfig 是表驱动配置源(非 yml)，短信引擎 basic+core 共22类需整体迁移，详见 6.6**。

**净需迁业务表 ≈ 58 张**（bean1+sms3+face1+video12+fs41；bean 由初判 4 经基线覆盖复核实收窄为 1，见 6.1.1），fs 一家占 41（71%）。而非旧工程「数百文件」的直觉。

#### 6.5.2 三个核心洞察

1. **文件数 ≠ 迁移量**：bean 98文件→仅 4 独有表；face 116文件→业务表≈1(Person)+64文件是 JNI 封装(sdk26/pool17/proxy16/config5)；pay 27文件→0表(纯 SDK 配置对象)。文件数大头是 controller/service/bo/vo/mapper 与技术封装。

2. **阶段3 技术封装是 6.4~6.7 硬前置(关键路径)**：facesdk/video/freeswitch/pay 四个 `nla-common-*` 不建，四批次无法落地。而 **6.1 bean / 6.3 sms 不依赖阶段3，可立即做**。

3. **数据迁移是隐藏大头**：bean 的 user/dept/role/menu/dict 代码免迁，但**存量数据要迁到基线 `sys_*`**——字段映射、主键自增→雪花、密码格式(旧加密 vs 基线 BCrypt)、关联表重建、租户列剥离。6.1 需单列「数据迁移子任务」。

#### 6.5.3 重划后迁移路线

| 优先级 | 批次 | 依赖 | 体量 |
|---|---|---|---|
| 可立即做 | 6.3 sms(3短信表) | nla-common-sms(已就绪) | 易 |
| 可立即做 | 6.1 bean(4独有表+数据迁移) | 无 | 中(数据迁移重) |
| 需先阶段3 | 6.5 video(12表) | nla-common-video | 中 |
| 需先阶段3 | 6.4 face(JNI封装+Person) | nla-common-facesdk | 中(封装重) |
| 需先阶段3 | 6.6 fs(41表) | nla-common-freeswitch | 难(最大) |
| 需先阶段3 | 6.7 pay | nla-common-pay | 特殊(暂未开发) |

**废弃/免迁清单(明确不做)**：oa 全部、Tenant×2、部门/岗位直连菜单×2、sms 的 Quartz 与 PublicNotice、activiti(→WarmFlow)、bean 14 个 sys_* 覆盖表的代码(仅迁数据)。

### 6.6 `sms` 表驱动短信引擎（批次 6.3 关键补充）

**核查纠正**：6.5 初判 sms「用 nla-common-sms 作发送底座」有误。旧 sms 是**完全自研的表驱动引擎**，与基线 sms4j（yml 驱动）机制根本不同，**不可直接套用**。

**引擎架构（basic 9 + core 13 = 22 类）**：
- `sms-basic`：SmsModel/MessageTemplate/SendModel 接口 + Result/Param/NameValuePair 模型 + SmsTypeEnum/SignPlaceEnum/SmsConstant
- `sms-core`：`AbstractSmsHttpClientManager`(模板方法骨架) + 8 渠道 client(单例) + HttpClientUtils/SignatureUtils
- 业务层：`SmsSendManager extends AbstractSmsHttpClientManager`，实现 findSmsModelList/findMobileMessageTemplateLast 从表供数

**send() 核心流程**：查启用渠道 → 多个则随机负载 → 套模板变量替换 → 按 smsType 路由 8 client → **失败遍历所有渠道故障转移** → 落 MobileMessage 记录 + 验证码回写 Redis。

**与基线 sms4j(3.3.5) 的本质区别**：
| 维度 | 旧引擎 | 基线 nla-common-sms(sms4j) |
|---|---|---|
| 配置源 | **数据库表**(动态) | yml(启动加载) |
| 渠道 | 8 自研 client | sms4j 内置供应商 |
| 多渠道路由 | **随机负载 + 故障转移** | 无(单供应商) |
| 覆盖 | 含短信网/创蓝网/维纳多/商务领航/云通讯等 sms4j 不含 | 阿里/腾讯/华为等 |

**迁移策略（已决策）**：**方案B —— 以 sms4j 3.3.5 为发送底座，改造为「表驱动 + SPI 保留全部 8 渠道」**。
用户明确两点铁律：① **三表是运行时配置源**（渠道启停/账号密码/签名/余额/模板/发送记录全在表里，**不写 yml、不靠启动固定加载**）；② 5 个小众渠道仍在用，功能须完全等价，用 SPI 自定义供应商保留。

**三表在表驱动引擎中的作用（务必牢记，非 yml）**：
| 表 | 实体 | 作用 | 映射到 sms4j |
|---|---|---|---|
| `sms_sms_config` | SmsConfig | **渠道账号表**：每行=一个短信通道(smsType 渠道类型/account/password/sign 签名/signPlace 签名左右/balance 余额/isActive 启用)。运行时读此表决定「用哪些渠道、如何鉴权」 | 每行 → 一个 `BaseConfig` 子类实例，`configId`=id、`supplier`=smsType 对应供应商 |
| `sms_mobile_message_template` | MobileMessageTemplate | **模板表**：configId 绑渠道/code 模板号/type 模板类型/content 含变量占位/variable 变量定义(JSON)。发送时按 type+configId 取最新模板做变量替换 | 供 `SmsSendManager` 组 `LinkedHashMap` 传 `sendMessage(phone,templateId,params)` |
| `sms_mobile_message` | MobileMessage | **发送记录表**：每次发送落库(mobile/content/status/msgId/callbackStatus 回执/resendNum 重发/handleTime)。审计 + 回执追踪 | 发送后由 `SmsSendManager` 写入 |

→ **表驱动 = 改表即改行为**：新增/停用渠道、换账号密码、调签名，都是改 `sms_sms_config` 表；同 type 多条 isActive=1 即多渠道路由/负载的数据来源。**这正是 sms4j `SmsReadConfig` 的用途**。

**sms4j 3.3.5 关键 API（已从本地 jar `javap` 核实，非臆测）**：
- 表驱动接口 `org.dromara.sms4j.core.datainterface.SmsReadConfig`：
  `BaseConfig getSupplierConfig(String configId)` + `List<BaseConfig> getSupplierConfigList()` —— **实现它即可从数据库表供数**。
- 工厂 `org.dromara.sms4j.core.factory.SmsFactory`（全静态）：
  `createSmsBlend(SmsReadConfig)` 注册全部 / `createSmsBlend(SmsReadConfig, configId)` 注册单条 /
  `getSmsBlend(configId)` 按渠道精确取 / `getSmsBlend()` 负载均衡取 / `getListBySupplier(supplier)` /
  `reload(configId, SmsReadConfig)` 热重载单条 / `reloadAll(SmsReadConfig)` 热重载全部 / `unregister(configId)` 注销。
  → **改表后调 reload/reloadAll 即可热更新，无需重启**（对齐基线「AFTER_COMMIT 定向刷新缓存、禁止 delAll」铁律）。
- SPI 三件套（自定义供应商）：
  `{X}Config extends org.dromara.sms4j.provider.config.BaseConfig`（基类已含 configId/accessKeyId/accessKeySecret/signature/sdkAppId/templateId/weight/factory/proxy 字段）；
  `{X}SmsImpl extends org.dromara.sms4j.provider.service.AbstractSmsBlend<{X}Config>`（基类已内置 `SmsHttpUtils http`，只需实现 5 个 abstract sendMessage/massTexting）；
  `{X}Factory implements BaseProviderFactory<{X}SmsImpl,{X}Config>`（`createSms/getConfigClass/getSupplier`），启动时 `ProviderFactoryHolder.registerFactory(...)` 注册。

**8 渠道 → sms4j 3.3.5 精确映射（javap 核实内置供应商后修正，比初判乐观）**：
| 旧渠道(smsType) | 旧端点 | sms4j 内置 | 处置 |
|---|---|---|---|
| 阿里云大于(5) | dysmsapi.aliyuncs.com | `aliyun` AlibabaConfig(requestUrl/action/version/regionId) | ✅ 内置直用 |
| 腾讯云(8) | tencentcloudapi v20210111 | `tencent` TencentConfig(territory/service/sdkAppId) | ✅ 内置直用 |
| 云通讯/容联(7) | CCPRestSmsSDK | `cloopen` CloopenConfig(baseUrl) | ✅ 内置(实现期验证 SDK→HTTP 差异) |
| ~~创蓝网(2)~~ | 222.73.117.156/msg/HttpBatchSendSM | — | ❌ **已废弃**：用户明确「不要老的创蓝网了」，不迁移（smsType=2 保留空位不复用） |
| 网易易盾(6) | sms.dun.163.com/v2/sendsms(secretId+签名) | `netease`=网易**云信**(codeUrl/verifyUrl) | ❌ 易盾≠云信，SPI 自定义 |
| 短信网(1) | web.duanxinwang.cc/asmx/smsservice.aspx | 无 | ❌ SPI 自定义 |
| 维纳多(3) | yl.mobsms.net(SunJCE 加密) | 无 | ❌ SPI 自定义 |
| 商务领航(4) | access.xx95.net:8886/SendSmsEx(account 用 \| 分割) | 无 | ❌ SPI 自定义 |

→ 净结论（创蓝网废弃后剩 **7 渠道**）：**3 内置直用(aliyun/tencent/cloopen) + 4 SPI(dxw 短信网/wnd 维纳多/swlh 商务领航/wyyd 网易易盾)**，无待验证项。

**推进顺序（用户定）**：先 **b6 表驱动发送骨架**（domain 三实体 + mapper 查询 + 4 SPI 供应商 + DbSmsReadConfig/SmsChannelManager/SmsSendManager 主链），跑通核心可行性后再补 **b5 三表管理 CRUD**（bo/vo/service/controller）。

**方案B 架构分层（依赖流向 nla-message → nla-common-sms，符合 starter→common 铁律）**：
- **nla-common-sms（技术层，可复用）**：
  - 4 个 SPI 自定义供应商 `Dxw/Wnd/Swlh/Wyyd(易盾)`，各 `{X}Config+{X}SmsImpl+{X}Factory`；把旧 `SmsHttpClient` 的 HTTP 重试、签名左右位置(`handleSign`)、`parseTemplateCode` 逻辑搬进 `AbstractSmsBlend.sendMessage`（基类已提供 `SmsHttpUtils http`）；
  - `SmsSupplierRegistrar`（`@AutoConfiguration`）：启动 `ProviderFactoryHolder.registerFactory(4 个自定义 Factory)`；
  - aliyun/tencent/cloopen 直接复用 sms4j 内置，不重写（创蓝网 chuanglan 已废弃，见上表）。
- **nla-message（业务层）**：
  - 三表 CRUD（domain/bo/vo/mapper/service/controller，按 6.4 规则：LongIdEntity→BaseEntity+雪花、Date→LocalDateTime、去 tenant_id、add/edit→AddGroup/EditGroup）；
  - `DbSmsReadConfig implements SmsReadConfig`：`getSupplierConfigList()` 查 `sms_sms_config` 中 isActive=1 行 → 按 smsType 组装对应 `{X}Config`（account→accessKeyId、password→accessKeySecret、sign→signature、id→configId、smsType→supplier）；
  - `SmsChannelManager`：启动 `SmsFactory.createSmsBlend(dbSmsReadConfig)`；表变更后 `@TransactionalEventListener(AFTER_COMMIT)` 触发 `reload/reloadAll/unregister`（**定向刷新，禁止 delAll**）；
  - `SmsSendManager`（对齐旧同名类职责）：模板变量替换（含 VERIFICATION_CODE 自动生成 6 位、REDIS_CODE 取缓存时长）、按 type 选渠道（`getSmsBlend()` 负载 or `getListBySupplier` 遍历故障转移）、Redis 防重发、落 `sms_mobile_message`、验证码回写 Redis；
  - `SmsController`（extends ApiController、RestResult、@Validated 分组、@ApiLog）。

**迁移量修正**：sms ≠「3 表」，而是「3 表 + 表驱动适配层(DbSmsReadConfig/SmsChannelManager/SmsSendManager) + 4 个 SPI 供应商」。不依赖阶段3 的 4 个封装，可独立推进；体量中等（4 个 SPI 渠道是主要工作量，aliyun/tencent/cloopen 复用内置大幅减负）。

### 6.6.1 批次 6.3 交付结果（已落地）

按用户定的推进顺序 b6→b5→b7→b8 完成，`nla-admin -am` 全量编译 `MVN_EXIT=0`，已提交 `fb74071`（含 `nla-modules/nla-message/`、`nla-common-sms/supplier/`、`SmsSupplierAutoConfiguration.java`、`script/sql/nla_message.sql`、`.imports`）。

**nla-common-sms（技术层，新增 14 类 + 改 1 处）**：
- `supplier/` 目录：`SmsSignUtils`(签名左右位置 `handleSign`) + 4 个 SPI 供应商 `dxw`(短信网)/`swlh`(商务领航)/`wnd`(维纳多)/`wyyd`(网易易盾)，各 `{X}Config`+`{X}SmsImpl`+`{X}Factory`（12 类），把旧 `SmsHttpClient` 的 HTTP、签名、模板号解析逻辑搬进 `AbstractSmsBlend.sendMessage`；
- `config/SmsSupplierAutoConfiguration`(`@AutoConfiguration`)：启动 `ProviderFactoryHolder.registerFactory` 注册 4 个自定义 Factory；改 `META-INF/spring/...AutoConfiguration.imports` 追加该自动配置；
- 基线既有 `SmsAutoConfiguration`/`PlusSmsDao`/`SmsExceptionHandler`(+测试) 未动；aliyun/tencent/cloopen 复用 sms4j 内置，创蓝网废弃。

**nla-message（业务层，整模块新建 28 类）**：
- 三表 `domain`/`bo`/`vo`/`mapper`（`SmsConfig`/`MobileMessageTemplate`/`MobileMessage`，按 6.4 规则：`LongIdEntity`→`BaseEntity`+雪花、`Date`→`LocalDateTime`、去 `tenant_id`）；
- 三表管理 CRUD `I*Service`+`*ServiceImpl`+`*Controller`：`SmsConfig` 写后定向 `refresh`/`remove` 通道、`password` 只写不读（编辑留空保持原值，依赖 `updateStrategy=NOT_NULL`）；`MobileMessage` 记录型仅 list/export/remove/clean（无 add/edit）；URL `/sms/{config,template,record}`，权限 `sms:{config,template,record}:*`；
- 表驱动主链 `sms/`：`DbSmsReadConfig`(implements sms4j `SmsReadConfig`，实时读 `sms_sms_config`) + `SmsChannelManager`(`@EventListener(ApplicationReadyEvent)` 初始化 + `refresh`/`remove`/`refreshAll` 定向热更，对齐"AFTER_COMMIT 定向刷新、禁 delAll") + `SmsSendManager`(选活跃且含该 type 模板的渠道、随机负载 + 故障转移、落 `sms_mobile_message`) + `SmsSendResult` + `SmsSendBo` + `SmsChannelEnum`/`SmsConstant`。

**DDL** `script/sql/nla_message.sql`：三表，雪花主键无 `auto_increment`、审计 5 列；`sms_sms_config`/`sms_mobile_message_template` 使用 `String delFlag` / `del_flag char(1)` 逻辑删除（阶段 4 已校正，见 4.5），`sms_mobile_message` 追加型物理删除；三表显式 utf8mb4 / `utf8mb4_cs_0900_ai_ci`；`sms_sms_config` 新增 `app_id` 列承载腾讯云 sdkAppId / 容联云 appId；创蓝网 `sms_type=2` 空位保留不复用。

**收尾增量（本轮补齐 2 项，`nla-message -am` 编译 `MVN_EXIT=0`）**：
- **菜单权限 SQL 已生成**：`script/sql/nla_message.sql` 追加「短信管理」目录 + 3 菜单（渠道配置/短信模板/发送记录）+ 12 按钮 + 16 条 `sys_role_menu` 授权；`menu_id` 用 `1761400000000002000` 独立号段（现有菜单最大 `1761400000000001623`，job/ai/workflow 无 `sys_menu` 插入，无冲突）；perms 与 3 controller 的 `@SaCheckPermission` 逐一对齐（config/template 各 list+query+add+edit+remove+export，record 仅 list+export+remove）；超级管理员自动可见，普通角色 `1761300000000000003` 按种子约定授权。前端 Vue 页面（`sms/{config,template,record}/index`）由独立前端任务线补齐，不影响后端鉴权。
- **`MobileMessageVo.mobile` 已脱敏**：加 `@Sensitive(strategy = SensitiveStrategy.PHONE, perms = "sms:record:export")`，与 `SysUserVo.phoneNumber` 同构；语义 = 有导出权限者与超级管理员见原文、其余见掩码（Excel 导出经 fesod 读原始字段，与“可导出即可见原文”一致）。`nla-common-sensitive` 早在 pom 声明，无需改依赖。

**发送入口进度**：阶段 5.3 已通过 `CaptchaController` → `SmsLoginCodeService` 内部调用接入登录验证码（见 5.6），沿用 `/resource/sms/code`。注册/重置验证码业务尚未接入；没有新增通用短信发送 REST 接口。

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

### 7.4 Java 源码（品牌替换与功能偏离）

| 文件 | 上游 | 本工程 | 理由 |
|---|---|---|---|
| `nla-modules/nla-gen/.../util/GenUtils.java` | `RegExUtils.replaceAll(text, "(?:表\|若依)", "")` | `"(?:表\|NLA)"` | `replaceText` 用于从表注释剔除噪音词生成代码功能名，剔除的是本工程品牌词 |
| `nla-modules/nla-demo/.../controller/TestExcelController.java` | `map.put("author", "Lion Li")` | `"TZY"` | 填充 `excel/多列表.xlsx` 模板的 `{author}` 占位符 |

认证功能偏离另按交付批次记录：阶段 5.1 的授权类型、方法级公开入口、clientId 与社交解绑归属见 5.4；阶段 5.2 的 `LoginHelper` User-Agent 空值保护见 5.5；阶段 5.3 的 `CaptchaController` 表驱动短信接入、`SmsLoginCodeService` 账号检查、`SmsAuthStrategy` 共用 key 与原子消费、`SmsSendManager` 登录模板校验及 SecureRandom 见 5.6；阶段 5.4 的小程序配置/交换边界、绑定服务/API、`XcxAuthStrategy` 真实账号与完整权限登录、`XcxLoginBody` 输入约束、`XcxLoginUser.appid` 及 `LoginType.XCX` 共享锁定提示见 5.7。同步上游这些类时须保留对应行为，`SecurityConfig` 的 `AllUrlHandler.getUrls()` 路径匹配保持原逻辑。

### 7.5 明确不引入的上游内容

| 上游路径 | 文件数 | 理由 |
|---|---|---|
| `.gitee/PULL_REQUEST_TEMPLATE.zh-CN.md`、`.gitee/ISSUE_TEMPLATE/{bug,config,feature}.yml` | 4 | 上游自己 gitee 仓库的托管平台元数据，引入后 issue/PR 会指向 dromara 的仓库 |
| `.claude/agents/backend-*.md` | 6 | **已等价落地**为 `.qoder/rules/backend-*.md`，文件名逐一对应：`backend-common-infrastructure` / `backend-crud` / `backend-engineering` / `backend-javadoc` / `backend-module-enhancement` / `backend-query-permission` |
| `sys_menu` 中 `1761400000000000004`（PLUS官网） | 1 行 | 见 7.1 规则 3 |
| 上游 `.flattened-pom.xml` | 各模块 | 构建产物，已 gitignore，且本工程 revision 不同 |

### 7.6 品牌审计白名单

`.migration/audit-brand.ps1` 当前基线：
**`scanning 998 files` / `scanned(text)=993  hits=0  allowlisted=5  file-exceptions=1`**

> 基线曾是 `scanned=985 / allowlisted=6`，差的 3 份是一个**已修复的真实盲点**：
> 脚本按扩展名过滤文本文件，而 `Path.GetExtension('.gitignore')` 返回的是
> `.gitignore` 而非空串，导致 `.gitignore` / `.editorconfig` / `.gitattributes`
> 三份 dotfile 从未被扫过（已用 `probe-audit-blindspot.ps1` 量化：在 998 份范围内
> 共 7 份未被扫，其中 4 份是 `.xdb` / `.xlsx` 真二进制，跳过合理）。
> 修复方式：文件名只有一个前导点时按无扩展名文本处理，兼容将来的 `.dockerignore` 等。

> `scanned` 988→993、`file-exceptions` 6→1、`allowlisted` 7→5，三个数同时变动都是
> 7.6.1 那次 skill 改名的直接结果：目录例外清空后 skill 的 5 份文档开始被正常扫描，
> 同时 `skills/ruoyi-plus-ai-coding` 这条行内白名单被移除（原命中 4 处：3 处在
> `backend-engineering.md`、1 处在 `.gitignore`），新增 2 处真实上游链接。

审计范围含 `.qoder`：其下 **17 份文件全部入库**（6 份 `rules/backend-*.md`
+ 6 份 `skills/ponytail*/SKILL.md` + 5 份 `skills/nla-plus-ai-coding/**`），均属交付物。
`.qoder/` 现在**完全不受 ignore 约束**（用户已删除全部相关规则），`.gitignore` 里只留
一段注释说明这是有意为之，防止后人再把规则加回去。

**注意 `Grep` 工具尊重 `.gitignore`**，当某目录被忽略时会返回假阴性的 0 匹配，
必须走 `Get-ChildItem` 的脚本才能扫到 —— 这条教训就是靠它发现
`.qoder/rules/backend-*.md` 里 9 处真实品牌残留的。

行内白名单（`$allow`，2 条，都是真实第三方仓库地址）：

| 模式 | 命中数 | 理由 |
|---|---|---|
| `gitee\.com/dromara/warm-flow` | 3 | warm-flow 是第三方开源项目，这 3 处是工作流建表脚本头部引用其真实来源地址（`script/sql/` 下 mysql / oracle / sqlserver 各 1 处；`mapping.md` 里还有 1 处，但整份文件已被 `$allowFiles` 跳过，不计数） |
| `gitee\.com/JavaLionLi/plus-ui` | 2 | 配套前端工程的 `6.X-Vue` / `6.X-React` 分支地址，在 `skills/nla-plus-ai-coding/references/frontend.md` |

改写这 5 处会指向不存在的仓库，因此原样保留。

文件级例外（`$allowFiles`，1 份）：

| 路径 | 理由 |
|---|---|
| `docs/migration/mapping.md` | 迁移台账必须在每一行映射的一侧写明上游产物名，去掉品牌词就失去了存在意义。计划本身也把"docs 中的来源说明"排除在扫查之外 |

目录级例外（`$allowDirs`）：**已清空**。曾用于豁免本地安装的 ai-coding skill，
理由是该目录不入库、名字不会进仓库；`.qoder` 解除忽略后这个理由失效，
遂按 7.6.1 改名并修复，现在与其他目录一视同仁地扫描。
**除非某目录确实无法去品牌，否则不要再往这个列表里加东西。**

审计扫描的 11 个模式：`(?i)ruoyi`、`(?i)dromara(?!\.(warm|sms4j|mica|easy))`、`(?i)lion\s?li`、
`(?i)lionli`、`(?i)crazylionli`、`ry-vue`、`\bry_(vue|job|workflow|ai)\b`、`logback-plus`、
`若依`（脚本内写作 `\u82e5\u4f9d` 转义）、`Michelle\.Chung`、`(?i)plus-ui`。扫描范围排除 `\target\`。

**任何后续编辑完成后都应重跑该脚本**，`hits` 必须为 0；非 0 时脚本 `exit 1`，可直接做 CI 门禁。
脚本已是纯 ASCII（中文字面量改为 `\uXXXX` 转义），**不依赖 BOM**，见 10.1 教训 2。

#### 7.6.1 ai-coding skill 改名与修复

`.qoder/skills/ruoyi-plus-ai-coding/` 原是从上游带来的本地 skill。`.qoder` 解除忽略后
它会入库，与"目录名字不能用若依的名字"的要求冲突；更严重的是**它对本仓库是坏的**。
`probe-skill-brand.ps1` 扫出 29 处命中（按行计），按性质分四类：

| 类 | 处数 | 性质 | 处置 |
|---|---|---|---|
| **A** | 14 | 模块路径在本仓库不存在：`ruoyi-modules/ruoyi-gen/src/main/resources/fm/`、`ruoyi-common`、`ruoyi-system`、`ruoyi-workflow`、`ruoyi-demo`、`ruoyi-common-mybatis` | 改为对应 `nla-*` 路径（已逐个 `Test-Path` 确认存在） |
| **B** | 1 | 包名错：`org.dromara.common.mybatis.core.domain.BaseEntity`，照抄编译不过 | 改为 `cn.com.nla.common.mybatis.core.domain.BaseEntity`（已核对真实 `package` 声明） |
| **C** | 12 + 目录名 | skill 自身标识：`name:`、`display_name`、标题、`$ruoyi-plus-ai-coding` 调用示例 | 目录与标识统一改为 `nla-plus-ai-coding` |
| **D** | 2 | 真实上游前端工程链接 | 原样保留并进白名单 |

**A 与 B 是正确性 bug，不是品牌问题**：`.qoder/rules/backend-engineering.md` 正是把 agent
派到 `references/backend.md` 去的，而那份文档指向的 `ruoyi-*` 路径全部不存在，
agent 照做只会读文件失败或写出错误 import。

执行脚本 `.migration/rename-ai-skill.ps1`（纯 ASCII，`protect → replace → restore` 三段式，
每条规则带最小命中数断言，`rules below minimum` 非 0 即 `exit 1`；支持干跑，`-Apply` 才写盘）。
实际替换次数（按出现次数统计，故大于上表按行计的处数）：

| 规则 | 次数 | 类 |
|---|---|---|
| `ruoyi-plus-ai-coding` → `nla-plus-ai-coding` | 13 | C |
| `RuoYi Plus` → `NLA Plus` | 2 | C |
| `org.dromara.common` → `cn.com.nla.common` | 1 | B |
| `ruoyi-modules` → `nla-modules` | 6 | A |
| `ruoyi-gen` → `nla-gen` | 3 | A |
| `ruoyi-common` → `nla-common` | 4 | A |
| `ruoyi-system` → `nla-system` | 4 | A |
| `ruoyi-workflow` → `nla-workflow` | 2 | A |
| `ruoyi-common-mybatis` → `nla-common-mybatis` | 1 | A |
| `ruoyi-demo` → `nla-demo` | 1 | A |

规则顺序有意把长模式排在前面，避免 `ruoyi-common` 抢先破坏 `ruoyi-common-mybatis`。
标题与 `display_name` 用 ASCII 子串 `RuoYi Plus` 匹配，这样脚本里不必写中文字面量。
`backend-engineering.md` 的 3 处 reference 路径一并更新，改后 `Test-Path` 确认三个目标文件都在。

验收：`.qoder` 下除 2 条白名单链接外品牌词残留 **0**，审计 `hits=0`，编码守卫 18 份 `failures=0`。

> ⚠️ **IDE 侧影响**：skill 的调用名由 `$ruoyi-plus-ai-coding` 变为 `$nla-plus-ai-coding`，
> 需要重新加载工作区才能生效。

### 7.7 排序规则统一到 `utf8mb4_cs_0900_ai_ci`

上游建表脚本在字符集声明上不一致，本工程忠实复制后继承了这个差异：

| 脚本 | `COLLATE` 声明 |
|---|---|
| `nla_system.sql` | **完全不写**，建表时继承库默认值 |
| `nla_job.sql` | 同上 |
| `nla_workflow.sql` | 同上 |
| `nla_ai.sql` | 上游**每张表都显式写** `COLLATE=utf8mb4_unicode_ci`，**本工程已统一改写** |

混用不是理论风险，已在目标服务器 `1.82.217.118:3401`（MySQL 8.0.23）实测：

```sql
SELECT CAST('abc' AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_cs_0900_ai_ci
     = CAST('abc' AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci;
-- ERROR 1267 (HY000): Illegal mix of collations
--   (utf8mb4_cs_0900_ai_ci,EXPLICIT) and (utf8mb4_unicode_ci,EXPLICIT) for operation '='
```

**当前约定**：全库统一 `utf8mb4` + `utf8mb4_cs_0900_ai_ci`（用户拍板，已写入 README）。
`nla_ai.sql` 的 22 处显式声明已由 `.migration/fix-ai-collation.ps1` 改写为该值，
改写后 `utf8mb4_unicode_ci` 残留 **0** 处、`utf8mb4_cs_0900_ai_ci` 命中 **22** 处（23 张表中
`sai_user` 上游本就不写 `COLLATE`，继承库默认值），`verify-encoding.ps1` 复核中文完好（cjk=1844 未变）。

#### 7.7.1 更正：`cs` 是捷克语代码，不是 case-sensitive

命名规则是 `utf8mb4_<语言>_0900_<ai|as>_<ci|cs>`，大小写敏感性只看**结尾**的 `_ci`/`_cs`。
迁移过程中一度误判 `utf8mb4_cs_0900_ai_ci` 为大小写敏感，已实测纠正：

| 排序规则 | `SELECT 'a' = 'A'` | 结论 |
|---|---|---|
| `utf8mb4_cs_0900_ai_ci` | **1** | 大小写**不**敏感（`cs` = Czech） |
| `utf8mb4_unicode_ci` | 1 | 大小写不敏感 |
| `utf8mb4_0900_as_cs` | 0 | 这才是大小写敏感 |

所以选它不影响登录、字典查询这类依赖大小写不敏感的逻辑。
对 CJK 文本，两者都是 UCA 排序、按码点权重比较，中文排序行为实质相同。

#### 7.7.2 ⚠️ `regen-sql.ps1` 尚未同步这条规则

它是 14 份 SQL 的唯一事实来源，**下次重生 SQL 会把 `nla_ai.sql` 的 22 处改回
`utf8mb4_unicode_ci`**，静默重现 ERROR 1267。该脚本仍含中文字面量、仍靠 BOM
（见 10.1 遗留风险），改它之前必须先转纯 ASCII。
**在那之前：重生 SQL 后必须重跑 `fix-ai-collation.ps1 -Apply`，并用 `probe-collation.ps1` 验收。**

**阶段 4 重写业务表 DDL 时同样必须显式写 `COLLATE=utf8mb4_cs_0900_ai_ci`**，
不要再依赖库默认值 —— 否则部署环境的库排序规则一旦不同就会重现这个问题。

### 7.8 工作流分库（WarmFlow 动态表名方案）

> 用户需求（原文）：**工作流需要使用分库，业务库在 `nla_workflow`**。上游 RuoYi-Vue-Plus
> 无此能力（workflow 表与 `sys_*` 同在主库），属有意偏离。

#### 7.8.1 WarmFlow 不支持分库（实测三条证据）

扒 `warm-flow-*` 全部 jar（1.8.9，`D:\develop\Maven\repository\org\dromara\warm`）：

| # | 证据 | 检测方式 |
|---|---|---|
| 1 | 配置项无 `schema`/`database`/`table-prefix`，仅 `data-source-type`（ORM 方言）、`tenant-handler-path`（`tenant_id` 行级多租户） | 解析 `plugin-modes-sb` 的 `configuration-metadata.json`，19 个属性无一涉及库名 |
| 2 | 8 张引擎表名硬编码在实体 `@TableName`，裸表名无库前缀 | 扫 `orm/entity/Flow*.class` |
| 3 | 全部 jar **0 处** `SqlSessionFactory`，复用宿主的，宿主连哪个库就发哪个库 | 全 jar 字节搜索 |

对比：Activiti（`activiti.datasource.jdbc-url`）、Flowable（`setDataSource`+schema）都原生支持独立引擎数据源，**WarmFlow 无此扩展点**。

#### 7.8.2 方案选型

| 方案 | 覆盖引擎三方 mapper | 事务 | 结论 |
|---|---|---|---|
| `@DS` 切数据源 | ❌ 引擎 mapper 在 jar 里加不了注解；官方也「不建议 mapper 加 `@DS`」；19 处 `FlowEngine.xxxService()` 走默认库 | 跨库非原子 | 否决 |
| `@TableName(schema=…)` | ❌ 引擎实体在三方 jar | — | 否决 |
| 每库独立 `SqlSessionFactory` | ❌ WarmFlow 复用宿主，无法隔离 | — | 否决 |
| **动态表名拦截器** | ✅ 改写在**共享 SQL 链**上，引擎+自有 mapper 全覆盖 | 单连接原子 | **采用** |
| 拆独立微服务 | ✅ 但监听器回调 `sys_user` 强耦合 | — | 不划算 |

采用 MP `DynamicTableNameInnerInterceptor`，把 12 张表（8 引擎 + 4 自有 `flow_category`/`flow_spel`/`flow_instance_biz_ext`/`test_leave`）改写成 `nla_workflow.<表>`，`sys_*` 不加前缀落主库。同实例跨 schema 是 MySQL 原生能力：一条连接、事务原子、业务零注解。此为社区 MP 分库主流做法（掘金「动态表名的正确打开方式」：动态表名与多数据源不冲突）。

#### 7.8.3 实现与配置化

| 文件 | 职责 |
|---|---|
| `config/WarmFlowConfig.java` | `InitializingBean`，把动态表名拦截器插在分页插件之前（MP 官方顺序）；`resolveTables()` 决定生效白名单 |
| `config/WorkflowProperties.java` | `@ConfigurationProperties("nla.workflow")`，含 `schema` + `tables` |
| `application-{dev,prod}.yml` | `nla.workflow.schema: nla_workflow` + `tables` 显式 12 张 |

**配置化取舍**：采纳社区「白名单放 yml」，但**不采纳 ThreadLocal 动态后缀**——那是为「按时间分表」设计，而 WarmFlow 大量异步回调（`WorkflowGlobalListener.publishEvent`、`AFTER_COMMIT` 缓存刷新），ThreadLocal 在线程切换时丢上下文会让 `flow_*` 落回主库（社区「避坑指南」亦警告）。本工程用**静态 schema 前缀**（启动固化进拦截器闭包）天然免疫。两点增强：① `tables` 留空回退内置默认 12 张（防漏配静默失效）；② 精确匹配而非社区 `startsWith`（避免误伤 `flow_xxx_bak`）。

#### 7.8.4 验证

- **单测 `WarmFlowConfigTest` 9/9 绿**：12 表全限定、主库表不动、增删改/join/子查询改写、同形列名不误伤、空库名回退、幂等、yml 配置覆盖默认清单。
- **真实启动冒烟**（`@Value` 版）：`Started in 21.33s`，日志 `工作流分库已启用：12 张表限定到库 [nla_workflow]，插入拦截器链位置 1`，`sys_oss_config` 落主库成功，零异常。
- ⚠️ **配置化后（`@ConfigurationProperties` 版）的真实 yml 绑定尚未冒烟复验**：核心逻辑已被单测覆盖，绑定 `List<String>` 为 Spring Boot 基础能力，风险低；如需实证，重启冒烟查 actuator `configprops` 的 `nla.workflow.tables`（日志「12 张」无法区分 yml 绑定与兜底，须用 configprops 或哨兵表区分）。


#### 7.8.5 为何只有工作流需要分库（job/ai 对照核查）

`nla-admin` 同样打包了 `nla-job`、`nla-ai`，对应表在独立库 `nla_job`（`sj_*`）、`nla_ai`（`sai_*`），乍看是同类跨库隐患。实测**不是**——部署形态与工作流根本不同：

| 模块 | admin 内的形态 | 谁访问业务表 | 结论 |
|---|---|---|---|
| `nla-workflow` | WarmFlow 引擎 + 8 个 Mapper **直连** `flow_*` | **admin 进程本身** | 需动态表名改写（见 7.8.3） |
| `nla-job` | 仅 SnailJob **客户端执行器**（`*Task`/`*Executor`）+ `BillDTO`（record，无 `@TableName`），**0 个 Mapper** | 独立进程 `nla-snailjob-server`（自带数据源 → `nla_job`） | admin 不碰 `sj_*`，无需处理 |
| `nla-ai` | 仅 `SnailAiController`，注入 `OpenApiUserClient` **SDK**（HTTP/RPC），**0 个 Mapper** | 独立进程 `nla-snailai-server`（自带数据源 → `nla_ai`） | admin 不碰 `sai_*`，无需处理 |

证据：两模块 `src` 下无任何 `@TableName`/`BaseMapperPlus`/`@DS`/`Mapper<`；`nla-snailjob-server` 的 `application-{dev,prod}.yml` 数据源为 `jdbc:mysql://localhost:3306/nla_job`，`nla-snailai-server` 为 `.../nla_ai`。**故动态表名拦截器只需覆盖 `flow_*`/`test_leave` 共 12 张，无需扩展到 `sj_*`/`sai_*`。**

---

## 8. 阶段进度台账

| 阶段 | 内容 | 状态 | 备注 |
|---|---|---|---|
| **0** | 前置环境 | ✅ 完成 | JDK 21 已就位（`D:\develop\Jdk\jdk-21.0.12.1`）；Maven `3.9.9` 全路径调用；依赖可拉取（阿里云 + 中央仓库） |
| **1** | 工程骨架 | ✅ 完成 | 根 POM、`nla-common-bom`、全部模块 POM、`NlaApplication`、9 份 admin 资源、`script/`、`.editorconfig`/`.gitattributes`/`mvnw` |
| **2.1** | common 子模块引入 | ✅ 完成 | **25 个全引入**，见第 3 节的取舍推翻记录 |
| **2.2** | 基座类映射落地 | ✅ 完成 | `core`/`mybatis`/`web` 三项基座（`R`、`HttpStatus`、`ServiceException`、`BaseEntity`、`PageQuery`、`PageResult`、`BaseController`、常量与工具类）随基线原样引入 |
| **2.3** | starter 映射 | 🟡 基座就位 | 基线侧的 10 个 `nla-common-*` 已引入；旧 starter 的 744 文件**尚未迁移**，调用点改造未开始 |
| **3** | 自建技术封装 | 🟡 进行中·5/6 | **freeswitch + video + facesdk + socketio + mq 已建成**（见 5.1.1~5.1.5）；mq 20 项测试全通过，全工程 46/46 模块编译 GREEN；余 pay 待建，沿用最低优先级、暂未开发约定；video 外部协议、facesdk Linux/GPU、socketio 真实 Redis 多节点及 mq 真实 broker/delay/confirm 验收待对应环境 |
| **4** | 数据层重写 | 🟡 进行中 | `nla-face` 已落地（4.4，9 项测试），短信删除标志/排序规则已校正（4.5，8 项测试），`nla-video` 12 表已落地（4.6，47 项测试），`nla-callcenter` 41 表已落地（4.7，121 项测试）；全工程 49/49 编译 GREEN；sys_area 已在 6.1.1 交付，参考数据导入/搜索评估与真实 MySQL 验收待推进 |
| **5** | 认证鉴权与租户 | 🟡 进行中·5.1～5.5 已交付 | 客户端契约、社交解绑归属、密码/验证码/锁定/账号状态、表驱动短信原子消费、小程序绑定/完整权限登录及扫码网页登录后端已交付（见 5.4～5.8，230 项测试，全工程 49/49 编译）；二维码前端/真实微信与 Redis 联调、小程序资料及手机号授权与外部验收待推进；**方法级权限注解属架构基线变更**，多租户保持禁用 |
| **6** | 业务模块迁移 | 🟡 进行中 | 7 批次，见第 6 节；**6.1 bean 已完成**（仅 `sys_area` 需迁，见 6.1.1）、**6.2 oa 废弃**（WarmFlow `TestLeave` 覆盖，见 6.4）、**6.3 sms 已完成**（3 表 CRUD + 表驱动适配 + 4 SPI，`nla-admin -am` 全量编译 `MVN_EXIT=0`，见 6.6.1），余 6.4~6.7 待推进 |
| **7** | 工作流与调度替换 | ⬜ 未开始 | Activiti → WarmFlow **重写**；XXL-JOB → SnailJob。**历史流程实例数据不可迁移，在途流程需用户确认兜底方式** |
| **8** | client 聚合层扁平化 | ⬜ 未开始 | 43 个 `@FeignClient` 全废弃 |
| **9** | 部署与验证 | 🟡 部分完成 | Dockerfile / docker-compose / nginx / `.run` 已按新模块重建；JUnit 4 → 5 未做；全量回归未做 |

### 8.1 本轮（阶段 0 + 1 + 2）验收结果

| 验收项 | 结果 |
|---|---|
| `mvn -B -DskipTests clean install` | ✅ **BUILD SUCCESS 40/40**，`MVN_EXIT=0`，耗时 2:52 |
| 全仓品牌残留 | ✅ `scanned(text)=993  hits=0  allowlisted=5  file-exceptions=1`（含 `.qoder` 全部 17 份与 3 份 dotfile，目录级例外已清空，见 7.6） |
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
5. **短信数据层删除标志不一致已修正** —— SmsConfig / MobileMessageTemplate 已统一为
   String delFlag，DDL 为 char(1)，8 项数据契约测试通过（见 4.5）。已有数据库的列升级
   和排序规则验收尚未执行，不能用初始化脚本重跑代替升级。

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
| `sys_oa-基类sql.sql` | **oa 已废弃**（见 6.4），请假由 `nla-workflow` 的 `test_leave` 覆盖，随旧模块清理 |
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
| `test-workflow.ps1` | 跑 `WarmFlowConfigTest` 分库契约测试（`-pl nla-modules/nla-workflow`，无 `-am`，需上游模块已 install），日志落 `.migration/test-workflow.log` | ✅ |
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
| `verify-tracked.ps1` | 跟踪完整性校验：对比 `git ls-files` 与新目录的磁盘实际文件，分别列出"在盘但未跟踪"与"已跟踪但已从磁盘消失"（后者用于发现误删）。已内置 `core.quotepath=false` 与 `[Console]::OutputEncoding = UTF8` 两个必要修正，否则 CJK 路径会产生假阳性 | ✅ |
| `probe-audit-blindspot.ps1` | 列出审计范围内因扩展名不被识别而**未被扫描**的文件及其扩展名直方图，用于定期反查审计盲点 | ✅ |
| `probe-bom.ps1` | 盘点 `.migration/` 每个脚本的字节数、BOM 有无、是否含 CJK 字面量，并标出 `NEEDS-BOM`（即无 BOM 却含中文的危险脚本） | ✅ |
| `probe-codepoints.ps1` | 从现有文件字节里提取 CJK 字面量的 `\uXXXX` 码点，用于把脚本改成纯 ASCII 时不靠记忆拼转义 | ✅ |
| `probe-script-ledger.ps1` | 将本节索引与 `.migration/` 实际文件对账，分别列出"已登记但丢失"与"存在但未登记" | ✅ |
| `probe-skill-brand.ps1` | 绕开审计的行内/目录白名单，对 ai-coding skill 目录做**原始**品牌暴露量化，按文件与模式列出命中（改名前的调查工具，见 7.6.1） | ✅ |
| `rename-ai-skill.ps1` | ai-coding skill 改名与修复的唯一事实来源：目录 `ruoyi-plus-ai-coding` → `nla-plus-ai-coding`，10 条带 min 断言的替换规则，保护 2 条真实上游链接（见 7.6.1） | ✅ |
| `probe-collation.ps1` | 全仓扫描 `utf8mb4_unicode_ci`，按文件列出命中数并**区分可改文件与老项目只读文件**（老项目命中必须保持不动，见 7.7） | ✅ |
| `fix-ai-collation.ps1` | 把 `nla_ai.sql` 的 22 处显式 `COLLATE` 统一为 `utf8mb4_cs_0900_ai_ci`；带 min 断言与残留 0 校验，保留原 BOM 状态，`-Apply` 才写盘（见 7.7）。⚠️ `regen-sql.ps1` 重生后必须重跑 | ✅ |

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

- **`Grep` 尊重 `.gitignore`** —— 对被忽略的目录会返回假阴性的 0 匹配，即使文件里确实有内容。
  本项目 `.migration/` 至今被忽略，`.qoder/` 也曾一度被忽略（当时就是靠 `Get-ChildItem`
  脚本才发现 `rules/backend-*.md` 里 9 处真实品牌残留）。审计这类目录一律走脚本（见 7.6）。
- **`-like '??*'` 里的 `?` 是单字符通配符** —— 想筛 `git status --porcelain` 的未跟踪行
  不能用它，会匹配全部行；改用 `.Substring(0,2) -eq '??'`。
- **`Get-Content` 默认按 ANSI/GBK 解码无 BOM 的 UTF-8 文件** —— 实测本文件
  （纯 LF、无 BOM）被 `Get-Content` 读成 **655 行**，而加 `-Encoding UTF8` 得 801 行、
  `[IO.File]::ReadAllText` 按 LF 切得 802 段（含末尾空段），凭空少了 146 行。
  **读数、比对、写回一律显式指定 UTF-8**（`Get-Content -Encoding UTF8` 或
  `[IO.File]::ReadAllText/ReadAllBytes`）；`Set-Content` 默认编码同样会损坏中文。
  已用 `verify-encoding.ps1` 对 18 份关键交付物做字节级往返比对，全部 `roundtrip=True`、
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
