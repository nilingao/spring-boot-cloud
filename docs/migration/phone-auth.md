# 阶段 5.6：小程序手机号授权

后端已接通微信小程序 `getuserphonenumber` API：用户在小程序点击"授权手机号"按钮获取一次性 `phoneCode`，服务端以 `stable_token` + `phoneCode` 换取真实号码并更新 `sys_user.phoneNumber`。入口沿用 `/auth/xcx/phone`，受 Sa-Token 会话保护。

## 配置与启用

功能依赖阶段 5.4 已有的 `xcx.apps` 配置（appid、secret、client-ids），不新增独立开关。小程序登录会话（`XcxLoginUser`）中存在 `appid` 即表明当前用户已通过小程序认证，可直接调用手机号授权。

```yaml
xcx:
  enabled: true
  apps:
    wx0123456789abcdef:
      enabled: true
      secret: ${XCX_APP_SECRET}
      client-ids: [mini-client]
```

## 前端调用

小程序先调用 `wx.login` 并通过 `/auth/login` (grant `xcx`) 完成登录获得系统 token，再在需要收集手机号的页面调用 `wx.getPhoneNumber()` 获取 `phoneCode`，最后使用系统 token 请求：

```http
POST /auth/xcx/phone
Authorization: Bearer <xcx token>
clientid: mini-client
Content-Type: application/json

{"appid":"wx0123456789abcdef","phoneCode":"getPhoneNumber 返回的 code"}
```

成功返回 `R<Void>`（code=200），当前系统账号的 `phone_number` 列已更新。

## 业务规则

- 会话必须是小程序登录（`XcxLoginUser.getAppid()` 非空）；普通密码/短信 token 不能调用。
- 请求 `appid` 必须与会话中 `appid` 精确匹配，防止跨 app 授权。
- `WechatPhoneClient` 先 POST `/cgi-bin/stable_token`（force_refresh=false）获取 access_token，再 POST `/wxa/business/getuserphonenumber?access_token=xxx`。连接超时 5 秒，每完整请求最长 10 秒，响应限 64 KB。
- 供应商失败、token 无效、`errcode≠0`、缺少 `phone_info`、号码为空或超长均返回统一业务错误，不传播 secret 或原始响应。
- 更新前执行手机号唯一性校验（`checkPhoneUnique`），已有其他账号使用同一号码时拒绝。
- 更新走 `updateUserProfile`（逻辑 `setIfPresent`），不触碰审计列以外字段。
- 不新增数据库表或列，不迁旧 `bean_mini.phone` 数据。

## 代码交付

| 文件 | 模块 | 作用 |
|---|---|---|
| `XcxPhoneBody.java` | nla-api | 请求体：appid + phoneCode |
| `WechatPhoneClient.java` | nla-admin | stable_token + getuserphonenumber 供应商协议 |
| `XcxBindingController.java` | nla-admin | `/auth/xcx/phone` 端点，校验会话/配置/唯一性后更新 |

## 验证

```powershell
mvn -o -B -pl nla-admin -am '-Dtest=WechatPhoneClientTest,LoginStrategyContractTest,AuthSecurityContractTest,SecurityConfigTest,SaTokenFunctionTest,SysSocialOwnershipTest,SmsDataContractTest,WechatMiniClientTest,SysXcxBindingTest,WechatQrClientTest,QrSceneStoreTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -o -B -DskipTests compile
```

`WechatPhoneClientTest` 9 项：正常获取、token 失败不调手机号 API、API 错误脱敏、缺少 phone_info、空/超长号码拒绝、网络异常不泄露、无效 JSON 拒绝、限长订阅器超限取消和正常接收。与既有阶段 5.1~5.5 回归共同执行，**跨模块合计 239 项全通过（nla-admin 单模块 206 项），无失败、错误或跳过**；根工程 **49/49 模块编译成功**。日志 `.migration/test-phone-contract.log`、`.migration/build-phone.log`。

真实微信 API 联调（phoneCode 一次性消费、access_token 限频、多 app 隔离）、Redis 会话刷新与完整应用启动留对应环境验收。阶段 5 后端可编码任务已全部交付，二维码前端联调、真实第三方授权和完整应用启动属外部验收。pay 继续暂缓。
