---
trigger: model_decision
description: 命名规范：业务模块包结构（controller.api、service.api、service.manage、mapper.sql、mapper.es、convert）；必须沿用的历史拼写（excption、dome、spingbootstartermybatis、srpingbootstartersecurityoauth、streamrabbitmq、hedel、seate-config、ActivitiServcieImplTest）；类命名后缀与存放位置（Entity、Param、Vo、Mapper、Service、ServiceImpl、ManageService、Convert、Controller、ServiceFeign、Application）；@RestController 显式 Bean 名格式；URL 前缀与 action 词表及 HTTP 方法映射；数据库表名、关联表、列名、索引前缀命名；ConstEnum 静态内部枚举固定写法。加载场景：新建或重命名类、包、接口 URL、表、字段、索引，定义枚举，判断某个拼写是否可改。
---

# 命名规范（包 / 类 / Bean 名 / URL / 数据库 / 枚举 / 历史拼写）

> 规范分册之一（原 `project-conventions.md` 按章拆分而来，结论均基于 `5.0.0-dev` 分支代码实证）。常驻铁律与全部分册索引见 `.qoder/rules/project-baseline.md`；查不到的机制见 `gaps-and-fs-tables.md`，禁止编造。

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

