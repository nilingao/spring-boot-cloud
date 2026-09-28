---
trigger: model_decision
description: 安全与校验：Spring Security 加 OAuth2 加 JWT 认证框架与多登录方式（密码、短信、微信小程序）；权限模型为网关集中鉴权、URL 与角色规则存 Redis Constant.ALL_URL_KEY、全项目零处方法级权限注解（禁用 @PreAuthorize、@Secured、@RolesAllowed）；参数校验分组（BaseModel 内部注解 page/list/add/edit/delete/detail/export/tree/updateStatus）与 fs 新规范平铺校验写法、@Validated 与 @RequestBody 与 @RequestParam 用法；数据脱敏（手机号）；防重提交现状；Sentinel 限流接入；分布式锁现状；多租户 tenant_id 处理；文件上传限制；密码密钥只写不读约定。加载场景：写参数校验、处理登录与权限、加限流、涉及租户隔离或敏感字段脱敏、上传文件。
---

# 安全权限与数据校验规范

> 规范分册之一（原 `project-conventions.md` 按章拆分而来，结论均基于 `5.0.0-dev` 分支代码实证）。常驻铁律与全部分册索引见 `.qoder/rules/project-baseline.md`；查不到的机制见 `gaps-and-fs-tables.md`，禁止编造。

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

