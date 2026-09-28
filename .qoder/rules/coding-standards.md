---
trigger: model_decision
description: 核心编码规范：RestResult 统一响应封装写法、RespCode 响应码语义与扩展约束、主键策略（LongIdEntity 自增）、审计字段与 DefaultDBFieldHandler 自动填充、时间字段 Date 加 @DateTimeFormat 与 @JsonFormat 双注解、状态与布尔字段用 Integer 0/1、金额字段现状、Lombok 固定注解组合、NotNullMap 轻量返回、PageResult 分页与 MyBatisUtils.selectPage、LambdaQueryWrapper 查询构造器约定、依赖注入方式、TreeUtil 树结构构建；以及注释与文档规范（JavaDoc 中文、@ApiModelProperty、Mapper XML 注释、README 维护要求）。加载场景：编写或修改 Entity / Param / Vo 字段、写 Service 查询与返回值、组装分页或树、补注释与文档。
---

# 核心编码规范与注释文档规范

> 规范分册之一（原 `project-conventions.md` 按章拆分而来，结论均基于 `5.0.0-dev` 分支代码实证）。常驻铁律与全部分册索引见 `.qoder/rules/project-baseline.md`；查不到的机制见 `gaps-and-fs-tables.md`，禁止编造。

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

