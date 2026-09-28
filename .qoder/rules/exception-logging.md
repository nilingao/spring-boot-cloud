---
trigger: model_decision
description: 错误处理与日志：异常体系（RespException 为唯一被全局处理器识别的自定义异常及其余异常类）；全局异常处理器 ApiController 的实现细节（HTTP 状态一律 200、DuplicateKeyException 唯一键中文提示依赖实体注解与索引前缀、禁止另建 @ControllerAdvice）；LoggerAspect 请求日志切面（DEBUG 级自动打印参数）；@ApiLog 操作日志由 LogsAspect 异步落库 sys_logs 与 LogsTypeEnum；@TransactionalEventListener(phase = AFTER_COMMIT) 异步事件驱动的缓存一致性模式（回滚不刷新、定向刷新、禁止 delAll）；MQ 异步；日志文件输出与目录管理规范（logback.xml、SkyWalking gRPC 上报、@Log4j2 而非 @Slf4j）。加载场景：抛异常或返回错误码、打日志、加操作日志、写缓存刷新逻辑、排查异常提示信息、配置日志输出。
---

# 错误处理、异常体系与日志规范

> 规范分册之一（原 `project-conventions.md` 按章拆分而来，结论均基于 `5.0.0-dev` 分支代码实证）。常驻铁律与全部分册索引见 `.qoder/rules/project-baseline.md`；查不到的机制见 `gaps-and-fs-tables.md`，禁止编造。

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

