# 阶段 5.7：小程序资料授权与自动注册

后端已接通微信小程序资料授权（昵称/性别/头像回写 `sys_user`）与未绑定身份的自动注册（按 app 显式开启）。入口沿用 `/auth/xcx/profile`（受保护）与 `/auth/login`（grant `xcx`），受 Sa-Token 会话保护。

## 配置与启用

资料授权依赖阶段 5.4 已有的 `xcx.apps` 配置，不新增独立开关。自动注册按 app 新增 `auto-register`（默认 false）：开启后小程序登录遇到未绑定身份时自动创建系统账号并绑定；未配置或未开启保持既有拒绝语义。

```yaml
xcx:
  enabled: true
  apps:
    wx0123456789abcdef:
      enabled: true
      secret: ${XCX_APP_SECRET}
      client-ids: [mini-client]
      auto-register: true   # 默认 false；开启后未绑定身份登录自动建号
```

## 前端调用

用户在小程序使用昵称输入组件（`type="nickname"`）与头像选择（`chooseAvatar`）后，以小程序会话 token 请求：

```http
POST /auth/xcx/profile
Authorization: Bearer <xcx token>
clientid: mini-client
Content-Type: application/json

{"appid":"wx0123456789abcdef","nickName":"昵称","gender":"1","avatar":1234567890}
```

昵称/性别/头像至少提供一个，只更新提供的字段。`avatar` 是 OSS ID：`chooseAvatar` 返回的临时图片必须先经 `/resource/oss/upload` 上传后回传 ossId，**不接受微信头像 URL 直存**（`sys_user.avatar` 为 Long，与旧 `bean_mini.avatar_url` 直存 URL 属有意偏离）。

自动注册无需额外调用：`wx.login` → `/auth/login`（grant `xcx`）时若身份未绑定且该 app 开启 `auto-register`，服务端直接建号、绑定并签发会话。

## 业务规则

- 会话必须是小程序登录（`XcxLoginUser` 且 appid 非空），请求 appid 与会话精确匹配；普通密码/短信 token 与跨 app 请求拒绝。
- 资料回写走 `updateUserProfile`（setIfPresent 语义），只触碰昵称/性别/头像，不触碰手机号，不同步 `sys_social` 元数据。
- 自动注册账号：`userName` 为 `m` + 16 位随机字符（查重最多重试 3 次）、昵称固定“微信用户”、`userType=sys_user`、密码为 32 位随机串的 BCrypt 哈希（用户不可知，阻断密码登录）、`createBy/updateBy=0L`；随后复用既有 `bind` 建立 appid 作用域的 `sys_social` 绑定。
- `@Lock4j(keys="#appid")` 锁内双检：已有合法绑定直接返回其 userId（幂等，不重复建号）；多行或 authId 不匹配的损坏绑定拒绝自动覆盖并提示人工处理，不静默修复。
- 与 `bind` 一致不加事务：用户创建成功但绑定失败可能留下孤立账号，需人工删号或改绑；注册账号默认无任何角色授权，仅可访问匿名或自身用户端接口。
- 不新增数据库表或列；旧隐式建号（无需确认）的语义不再保留，必须按 app 显式开启。

## 代码交付

| 文件 | 模块 | 作用 |
|---|---|---|
| `XcxProfileBody.java` | nla-api | 请求体：appid + 昵称（@Xss/≤30）/性别 [012]/头像 OSS ID（@Positive） |
| `XcxProperties.java` | nla-admin | App 新增 `autoRegister`（默认 false） |
| `SysXcxBindingService.java` | nla-system | `autoRegister`（锁内双检/唯一名/复用 bind）+ `generateUserName` |
| `XcxAuthStrategy.java` | nla-admin | 构造 4→5 参，login 按 app 配置分叉 autoRegister/findUserId |
| `XcxBindingController.java` | nla-admin | `/auth/xcx/profile` 端点；公共 `requireXcxApp` 会话校验（phone 一并接入） |

## 验证

```powershell
mvn -o -B -pl nla-admin -am '-Dtest=WechatPhoneClientTest,LoginStrategyContractTest,AuthSecurityContractTest,SecurityConfigTest,SaTokenFunctionTest,SysSocialOwnershipTest,SmsDataContractTest,WechatMiniClientTest,SysXcxBindingTest,WechatQrClientTest,QrSceneStoreTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -o -B -DskipTests compile
```

本批新增 12 项：`SysXcxBindingTest` 自动注册 9 项（真实 H2/Mapper：创建+绑定+随机密码、幂等、损坏/破损绑定拒绝、逻辑解绑后新建、用户名查重重试与放弃、无效身份参数化）、`LoginStrategyContractTest` 策略分叉 2 项、`AuthSecurityContractTest` profile MVC 1 项。与既有阶段 5.1~5.6 回归共同执行，**跨模块合计 251 项全通过（nla-admin 单模块 209 项），无失败、错误或跳过**；根工程 **49/49 模块编译成功**。日志 `.migration/test-phone-contract.log`、`.migration/build-phone.log`。

测试以替身验证建号字段策略、幂等与拒绝逻辑；真实雪花回填、多实例 Lock4j 竞争、真实微信昵称/头像组件、chooseAvatar 上传链路、真实 Redis/MySQL 与完整应用启动留对应环境验收。
