# 阶段 5.1：登录与客户端访问契约

本批在现有 Sa-Token 基座上落实认证入口、客户端绑定、方法级权限与社交解绑归属。新工程继续使用 `/auth/login`、`Authorization: Bearer <access_token>` 和 `clientid`；不增加旧 OAuth2 `/oauth/token` 兼容端点，不迁旧测试账号、token 或租户数据。

## 请求与响应

登录 JSON 的 `clientId` / `grantType` 必填；客户端必须存在、启用，并明确允许该完整授权类型。`sys_client.grant_type` 是逗号分隔的类型列表，配置值可去除首尾空白，大小写保持精确匹配。请求 `word` 不得因为配置含 `password` 而放行，请求 `password,email` 也不能作为一个策略放行。

成功响应沿用 `R<LoginVo>`：`data.access_token`、`data.client_id`、`data.expire_in`；没有增加刷新令牌实现。错误沿用既有响应语义：HTTP 响应状态保持基线行为，前端根据 JSON `code` 判定，未登录/客户端冲突为 401，无菜单权限、角色或客户端路径/IP 授权为 403，登录策略与客户端配置拒绝为 500。

普通受保护请求至少提供一个 `clientid` 请求头或请求参数。提供的全部值必须与当前 token 的客户端扩展字段一致，包括重复参数与重复请求头；一个正确值不能覆盖另一个冲突值，空值也不会被忽略。token 缺少客户端扩展字段时返回 401。Cookie 登录继续禁用。

## 路由和业务边界

| 端点/规则 | 本批行为 |
|---|---|
| POST /auth/login | 方法级 SaIgnore；验证请求、客户端状态及完整授权类型后分派既有策略 |
| POST /auth/register | 方法级 SaIgnore；保留注册开关与输入校验 |
| GET /auth/binding/{source} | 方法级 SaIgnore；保留第三方授权地址生成 |
| POST /auth/logout | 方法级 SaIgnore；保留既有退出的幂等行为 |
| POST /auth/social/callback | 受统一登录、clientid、路径与 IP 规则约束；保留端点内登录检查 |
| DELETE /auth/unlock/{socialId} | 同上；用户 ID 来自 LoginHelper，用同一条 SQL 按绑定 ID 与所属用户删除 |
| 已注册路径 | 沿用 `AllUrlHandler.getUrls()` 的基线路径匹配和排除配置 |
| SaCheckPermission / SaCheckRole | 由真实 SaInterceptor 读取当前会话权限，沿用既有 SaPermissionImpl 和异常处理器 |
| security.excludes / 方法级 SaIgnore | 继续作为显式公开路径机制；调用方按公开端点用途配置 |

取消认证控制器的类级 SaIgnore，避免新增受保护端点继承匿名例外。社交解绑新增 `ISysSocialService.deleteByIdAndUserId`，对空 ID、空用户、不存在或他人绑定返回 false；没有先查归属再按 ID 删除，绑定在删除前被重新归属时仍按删除时的用户条件拒绝。保留原通用删除方法供已有内部调用使用，当前认证端点改用有归属条件的方法。

客户端路径和 IP 限制继续来自 token 的登录时配置快照；空配置沿用不额外限制的语义。本批不使客户端策略修改即时更新既有 token。IP 来源沿用 ServletUtils 的代理头解析，真实部署需核对反向代理对转发头的处理，不能把本地请求测试当作生产代理配置验收。

当前基线禁用多租户，业务表不新增 tenant_id；企业 companyId 是业务所属条件，不能代替当前登录者的权限。face/video/callcenter 的业务权限与关联校验仍在对应阶段 6 实施。

## 验证方式与限制

```powershell
mvn -o -B -pl nla-admin -am '-Dtest=AuthSecurityContractTest,SecurityConfigTest,SaTokenFunctionTest,SysSocialOwnershipTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

AuthSecurityContractTest 使用精简 Spring MVC 上下文，执行真实 AuthController、SecurityConfig、AllUrlHandler、SaInterceptor、JWT 简单模式、LoginHelper、SaPermissionImpl 和异常处理器；Sa-Token DAO 是内存实现。用户、客户端、密码策略、供应商和通知服务为测试替身，登录测试验证策略分派与真实令牌访问流程，不代表真实密码、验证码或第三方供应商登录已验收。覆盖授权名称匹配、客户端缺失/禁用、客户端参数冲突、公开端点、已注册路径（含单段路径变量）、角色/权限、Cookie 拒绝、非法/已撤销 JWT、雪花用户/部门 ID 精度、客户端超时、路径/IP 限制和社交解绑会话用户传递。

SysSocialOwnershipTest 从交付 `nla_system.sql` 提取 sys_social 表结构，不执行 DROP 或种子语句，以 H2 MySQL 模式和真实 Mapper 验证本人/他人/空条件/重新归属的解绑行为。固定测试数据只存在于内存数据库，H2 不代表真实 MySQL 排序规则、并发负载或生产连接验收。

本批验证结果（JDK 21 / Maven 3.9.9）：**50 项测试全部通过，无失败、错误或跳过**，其中 AuthSecurityContractTest 37 项、SysSocialOwnershipTest 4 项、SecurityConfigTest 3 项、SaTokenFunctionTest 6 项。根工程 `mvn -o -B -DskipTests compile` **49/49 模块成功**。日志为 `.migration/test-auth-security-contract.log` 与 `.migration/build-auth-security-reactor.log`。

`nla-admin -am` 编译依赖过滤检查未出现 H2，H2 保持 test scope，日志 `.migration/deps-auth-security.log`。

下一批阶段 5.2 核对真实密码、验证码、重试锁定与账号状态契约，以及旧短信/小程序/二维码登录的功能映射；第三方绑定、Redis 会话/权限刷新、生产反向代理和完整应用启动仍需后续验收。阶段 5 尚未整体完成，pay 继续暂缓。
