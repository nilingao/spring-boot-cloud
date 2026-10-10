# 阶段 5.2：密码、验证码与账号状态契约

本批执行现有密码、短信和邮箱登录策略的实际逻辑，补齐图形验证码生成、BCrypt 校验、失败计数、账号状态和 JWT 会话测试，并核对旧短信、小程序与二维码登录的功能映射。5.2 批次唯一生产改动是 `LoginHelper` 对缺失 User-Agent 的空值处理；`AllUrlHandler.getUrls()` 的鉴权路径匹配沿用基线。本文的短信缺口记录对应 5.2 当时的状态，后续发送接入、统一缓存 key 与原子消费已在 [阶段 5.3](sms-login.md) 交付。

## 当前可验证的登录行为

入口继续使用阶段 5.1 的 `/auth/login`、客户端授权列表和响应结构，见 [authentication.md](authentication.md)。策略级请求字段如下：

| grantType | 业务字段 | 本批验证范围 |
|---|---|---|
| password | username、password；图形验证码启用时还需 code、uuid | 真实 BCrypt 成功/错误密码、账号缺失/停用、输入约束、图形验证码生成及校验、锁定与会话生成 |
| sms | phoneNumber、smsCode | 缓存已有验证码时的成功、错误、缺失/过期、账号缺失/停用和跨策略锁定；不代表短信发送链路已经接入 |
| email | email、emailCode | 同上，执行真实邮箱登录策略；邮件发送与 SMTP 未验收 |
| xcx | appid、xcxCode | 基线存在策略模板，但密钥和 openid 用户查询未实现，本批不将它标为可用登录功能 |
| social | source、socialCode、socialState | 已有授权/绑定登录策略，真实第三方授权留后续验收 |

`PasswordLoginBody` 沿用 username 2～30 字符、password 5～30 字符且非空的校验；clientId、grantType 非空。账号查询结果为空返回 `user.not.exists`，status 为 `1` 返回 `user.blocked`，这两类拒绝发生在密码或短信/邮箱码校验之前。用户查询在本批测试中使用 Mapper 替身，未验收真实数据库查询、排序规则或逻辑删除 SQL。

`LoginHelper.fillRequestContext` 原来在 User-Agent 缺失时直接访问解析结果，造成正确密码或验证码登录空指针。本批只为浏览器/操作系统填充增加空值判断；缺失或空白请求头不阻断认证，已解析的信息仍可填充，IP、客户端设备与身份/权限会话照常建立。

## 图形验证码和失败计数

图形验证码从 `/auth/code` 获取，响应为 captchaEnabled、uuid、img。验证码开关关闭时 uuid/img 为空；启用时 math/char 两种生成方式均验证实际 160×60 图片及缓存答案，缓存期限沿用 `Constants.CAPTCHA_EXPIRATION`。测试直接执行生成方法，未执行 Redis 限流切面。

密码策略按 `GlobalConstants.CAPTCHA_CODE_KEY + uuid` 读取答案，然后删除该缓存项；答案大小写不敏感。答案缺失报 `CaptchaExpireException`，不匹配报 `CaptchaException`。本批验证了顺序消费、重放拒绝、错误验证码也被消费及 uuid 隔离；读后删除的并发原子性不在本批验收范围。

`SysLoginService.checkLogin` 沿用以下规则：

- 失败计数 key 为 `CacheNames.PWD_ERR_CNT_KEY + username`，密码、短信和邮箱策略共享同一账号计数，不同账号隔离。
- 当前配置最大错误次数为 5、缓存期限为 10 分钟；每次认证错误写回计数与期限，第 5 次错误进入锁定。
- 计数达到阈值时先拒绝，再决定是否执行校验回调；正确密码或正确短信/邮箱码不能绕过锁定，锁定期间的拒绝不重新写期限。
- 阈值以下的认证成功清除失败计数；图形验证码错误以及短信/邮箱答案过期不会作为一次密码/验证码不匹配递增计数。
- 测试捕获真实 `LoginInfoEvent`，核对错误和锁定事件的用户、失败状态、IP 与客户端标识；没有加载生产日志落库监听器。

Redis 操作使用每测隔离的内存替身，测试检查写入的 Duration，并通过移除缓存项模拟过期。未连接真实 Redis、未等待真实期限，也未验证多实例下读改写计数的并发行为。BCrypt 使用真实实现和独立测试哈希，不导入旧密码或账号。

## 旧登录功能映射与缺口

| 旧功能/关键源类 | 新工程对应 | 当前结论与后续工作 |
|---|---|---|
| `CodeTokenGranter`，grant `code`，key/verificationCode/username/password | `PasswordAuthStrategy`，grant `password`，uuid/code/username/password | 图形验证码加密码能力由基线覆盖；采用新 JSON 契约，不增加旧 OAuth2 表单入口 |
| `SmsCodeTokenGranter`，grant `sms`，phone/SmsCodeCode | `SmsAuthStrategy`，phoneNumber/smsCode | 登录策略已验证；旧策略校验成功会删除短信码，新基线策略目前只读取并比较，消费语义需在短信接入批次处理 |
| 旧 `SmsSendManager` 和发送类型缓存 | `nla-message/sms/core/SmsSendManager` | 表驱动渠道/模板引擎已迁；它写 `redis:verificationCode:{type}_{mobile}`，登录类型为 1；基线策略读 `GlobalConstants.CAPTCHA_CODE_KEY + phoneNumber`，两种 key 未贯通 |
| 新基线 `/resource/sms/code` | `CaptchaController.smsCode` | 仍调用固定 `SmsFactory.getSmsBlend("config1")`、空模板号和 4 位码，未使用表驱动发送引擎；不能把登录策略测试当作发送可用证明 |
| `WxMiniGranter` / `WxMiniAuthenticationProvider` / `WxMiniUserServiceImpl`，grant `wx_mini` | `XcxAuthStrategy`，grant `xcx`；`sys_social` + `sys_user` | 策略模板存在，但 app secret 是占位文本，`loadUserByOpenid` 返回空 SysUserVo；尚未实现 appid/openid 绑定查询、账号状态、旧用户信息处理和场景联动 |
| `WxMiniWebGranter` / `WxMiniUserOpenIdServiceImpl`，grant `wx_mini_web` | 已确认的绑定模型与 Sa-Token 登录参数 | 没有完成等价的扫码网页登录；后续从服务端确认的场景/绑定中构造系统用户会话 |
| `MiniController.getQRCode` / `WxMiniQrEvent` / `WxMiniLoginEvent` / `QRDataListener` | `nla-common-socketio` + `nla-common-mq` 技术封装 | 旧链路包含微信码生成、3 分钟场景缓存、小程序授权、MQ 通知、网页会话签发与 Socket.IO 房间推送；技术封装就绪不等于二维码业务已迁移 |

旧 `bean_mini` / `bean_mini_user` 的数据模型覆盖仍按台账使用 `sys_social` 与 `sys_user`，不新增旧表、不迁旧测试账号、token 或租户数据。数据模型覆盖不代表小程序登录实现已交付。

## 验证与下一批

```powershell
mvn -o -B -pl nla-admin -am '-Dtest=LoginStrategyContractTest,AuthSecurityContractTest,SecurityConfigTest,SaTokenFunctionTest,SysSocialOwnershipTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

`LoginStrategyContractTest` 执行真实 PasswordAuthStrategy、SmsAuthStrategy、EmailAuthStrategy、SysLoginService（包括虚拟线程权限组装与失败事件发布）、LoginHelper、JWT 简单模式、JSON/Bean Validation、BCrypt 和图形验证码生成。Mapper、权限/部门/角色/岗位数据源以及 Redis 操作使用替身；Sa-Token DAO 为内存实现。Spring MVC 路由/客户端检查由既有阶段 5.1 契约测试共同回归。

本批 JDK 21 / Maven 3.9.9 验证结果：新增 **41 项**，与阶段 5.1 的 50 项共同运行，**91 项全部通过，无失败、错误或跳过**；根工程 `mvn -o -B -DskipTests compile` **49/49 模块成功**。日志 `.migration/test-login-strategy-contract.log`、`.migration/build-login-strategy-reactor.log`；编码、变更范围和报告核对脚本 `.migration/audit-login-strategies.py`。

阶段 5.3 已接通表驱动短信发送与登录验证，统一登录验证码缓存契约与原子消费，补齐无渠道、模板缺失、发送失败、停用账号、重发与验证码复用场景，详见 [sms-login.md](sms-login.md)。小程序账号绑定、二维码状态和通知链路仍是独立待迁项；真实 MySQL/Redis/供应商、第三方授权和完整应用启动留对应环境验收。阶段 5 保持进行中，pay 继续暂缓。
