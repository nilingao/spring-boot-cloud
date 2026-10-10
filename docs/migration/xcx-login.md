# 阶段 5.4：微信小程序登录与账号绑定

`XcxAuthStrategy` 已从占位模板改为：服务端配置选择小程序 → JustAuth 使用 wx.login 的 code 换取身份 → 按 appid/openid 查找唯一有效绑定 → 检查系统账号和共享登录锁定 → 组装完整权限并签发 JWT。绑定关系使用现有 `sys_social` 和 `sys_user`，不重建旧 mini 表或迁移旧账号。

## 配置与登录

功能默认关闭，通过部署配置启用。以下 appid 仅是示例，应替换为实际小程序 ID，密钥来自服务端环境；该客户端还须在系统客户端管理中启用并授权 `xcx` grant。

```yaml
xcx:
  enabled: ${XCX_ENABLED:false}
  apps:
    wx0123456789abcdef:
      enabled: true
      secret: ${XCX_APP_SECRET:}
      client-ids:
        - web-client
```

可配置多个小程序，每个 appid 分别指定 secret 和客户端白名单。功能关闭、app 未配置/停用、密钥空白或实际客户端不在白名单时，先拒绝再决定是否请求微信。该配置独立于已有 `justauth.type` 第三方网页授权配置，不接收请求中的密钥。

调用 `wx.login` 取得一次性 code 后，使用既有 `/auth/login`：

```json
{
  "clientId": "web-client",
  "grantType": "xcx",
  "appid": "wx0123456789abcdef",
  "xcxCode": "本次 wx.login 返回的 code"
}
```

appid 必须非空并符合微信小程序 ID 格式，code 非空且不超过 512 字符。入口沿用客户端启用状态、完整 grant 名匹配和 `/auth/login` 响应结构；实际配置选择以入口确认的客户端为准。系统用户 ID 和 openid 从服务端确认的身份与绑定查询取得，客户端传入的 userId/openid 不参与认定。

供应商失败、异常、缺少用户/token/openid 或无效身份明确拒绝；不直接向用户传递可能含凭证的供应商原始错误。unionId 可以缺失，只作绑定元数据，不用于跨 app 自动合并账号。session_key 不保存至绑定表、会话或响应。

## 绑定与解绑

先使用现有密码或短信登录取得系统会话，再从小程序取得新的 wx.login code，调用受保护的接口：

```http
POST /auth/xcx/bind
Authorization: Bearer <当前系统 token>
clientid: web-client
Content-Type: application/json

{"appid":"wx0123456789abcdef","xcxCode":"新的 wx.login code"}
```

该接口沿用 token、clientid、客户端路径/IP 策略；绑定使用 token 所属用户与实际 token 客户端，body 不接受绑定目标用户。绑定客户端也须在该 app 的 client-ids 中。服务端再次查询系统账号，缺失或停用时不写绑定。

- source 为 `WECHAT_MINI_PROGRAM:{appid}`，auth_id 为 `WECHAT_MINI_PROGRAM:{appid}:{openid}`；相同 openid 在不同 app 中可以分别绑定，不混用网页微信身份。
- 一个 app 内同一身份不能抢占他人的绑定；同一用户重复绑定同一身份幂等，不追加记录。已绑定其他身份时要求先解绑，不能静默换绑。
- 小程序绑定方法使用 `@Lock4j(keys="#appid")`，同一 app 的绑定写入通过系统既有锁基座串行。该锁覆盖本服务调用，不是数据库唯一约束；绕过服务的写入或锁失效仍可能制造重复记录。登录遇到重复/损坏绑定会拒绝，不能任意选择第一条。
- 新查询显式过滤 `del_flag='0'`，并在数据库候选结果上精确比较 source/openid，防止默认排序规则忽略大小写造成身份混淆。auth_id 损坏也拒绝登录和重绑。
- access_token 按现有表的非空要求写空串，不把微信 session_key 当 OAuth token 存储。昵称/用户名取系统账号，不使用未经验证的前端资料。

解绑沿用 `/auth/unlock/{socialId}`，在同一 SQL 中验证绑定主键及当前用户归属。解绑后新小程序登录查不到绑定；已有系统会话的失效仍遵循现有退出/撤销机制，本批没有新增解绑时批量踢出逻辑。

## 登录状态与验收

绑定用户不存在/停用时拒绝；密码、短信、邮箱产生的账号锁定也限制小程序登录。`LoginType.XCX` 复用现有账号重试提示，微信供应商拒绝不作为某个系统账号的密码错误递增；已确认凭证且锁定阈值以下时沿用基线清除错误计数。小程序上下文在普通 `LoginUser` 的完整部门、角色、岗位、菜单权限和数据权限之上增加 appid/openid，客户端有效期、路径/IP 策略沿用现有 token 参数。

```powershell
mvn -o -B -pl nla-admin -am '-Dtest=WechatMiniClientTest,SysXcxBindingTest,LoginStrategyContractTest,AuthSecurityContractTest,SecurityConfigTest,SaTokenFunctionTest,SysSocialOwnershipTest,SmsDataContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -o -B -DskipTests compile
```

验证执行真实配置绑定、供应商响应处理、H2 交付 DDL/绑定 Mapper、认证策略、权限组装、JWT 与 MVC 访问控制；微信请求、系统用户/权限数据源及 Redis 使用替身，H2 测试直接调用绑定服务，没有执行真实分布式锁。真实微信 code 一次性消费、Redis 锁竞争、MySQL 排序规则与完整应用启动留对应环境联调，不将替身测试认定为线上可用验收。

JDK21 / Maven 3.9.9 验证结果：`WechatMiniClientTest` **20 项**、`SysXcxBindingTest` **12 项**；既有登录策略新增 **12 项**（共 85）、MVC 新增 **4 项**（共 41）。本批新增 **48 项**，与既有认证及短信数据层回归共同执行，**179 项全通过，无失败、错误或跳过**；根工程 **49/49 模块编译成功**。日志 `.migration/test-xcx-contract.log`、`.migration/build-xcx-reactor.log`。

本批只交付小程序账号绑定与登录。旧小程序用户资料/手机号授权、自动注册、微信码生成、扫码网页场景状态、MQ/Socket.IO 通知链路仍待推进；不默认开放未配置的 app，不自动合并 unionId 账号，旧测试数据不迁。`AllUrlHandler` 的路由匹配沿用原逻辑，多租户保持禁用，pay 暂缓，阶段 5 仍在进行中。
