# 阶段 5.3：表驱动短信发送与登录

`GET /resource/sms/code?phoneNumber=13800138000` 已接入 `SmsLoginCodeService` → `nla-message/SmsSendManager`。发送前检查手机号格式和账号是否存在、停用；渠道账号与登录模板来自业务表，发送入口不再使用固定 config1、空模板号或另造一份验证码。登录继续使用 `/auth/login` 的 JSON 契约，见 [authentication.md](authentication.md)。

## 请求与配置

登录请求示例（clientId 须使用已授权 sms 的客户端，smsCode 使用收到的短信码）：

```json
{
  "clientId": "web-client",
  "grantType": "sms",
  "phoneNumber": "13800138000",
  "smsCode": "123456"
}
```

`sms_sms_config` 账号须启用，且存在该账号的 type=1 登录模板。逻辑删除过滤和最新模板选择沿用既有查询。模板变量示例：

```json
{"VERIFICATION_CODE":"","REDIS_CODE":""}
```

验证码变量为空时由 `SecureRandom` 生成 6 位数字；缓存分钟数为空时取 5，也可配置正整数。内容型渠道的 content 使用 `VERIFICATION_CODE` 等占位符；模板型渠道的 code 填供应商模板编号，多渠道编号可用 `5:模板编号,8:模板编号`，参数名须与供应商模板约定一致。模板原有显式变量值能力保留；实际登录配置应将验证码值留空。

登录模板必须属于 type=1，声明验证码变量和正缓存期限。模板缺失、变量 JSON 无效、期限非法或缺少所需变量时跳过该渠道，继续尝试其他渠道；全部不可用则失败，不发送不可校验的短信。供应商模板参数/编号与真实内容仍需渠道环境联调确认。

## 缓存、消费与错误

发送与校验共同使用 `SmsConstant.verificationCodeKey`：登录 key 为 `redis:verificationCode:1_{phoneNumber}`。注册等用途按 type 隔离，不作为登录凭证；旧基线 `CAPTCHA_CODE_KEY + phoneNumber` 不再作为短信登录答案，不做双写或旧缓存迁移。图形验证码和邮箱验证码继续沿用原契约。

- 发送成功、发送记录写入后，缓存供应商实际发送的验证码，期限取该模板的分钟数。无渠道、无模板、未注册通道、供应商失败或异常均不缓存验证码；故障转移成功时仅缓存成功渠道的验证码，记录最终一次尝试。
- 验证码仍有效时保留原码、不再次发送、不追加记录，返回成功响应与等待提示。`SmsSendResult.success=true` 也包含这一防重发结果，不能据此认定刚下发了短信。
- 公共发送接口保持 HTTP 200 + `R.code`，成功/防重发为 200，业务失败为 500，data 为空；不将验证码放进响应。原手机号 60 秒限流注解保留。
- 登录先检查账号存在/状态和账号重试锁定，再读取短信码。输入错误保留验证码并递增既有账号失败计数；缺失、过期不递增。停用或锁定账号不能消费验证码或签发会话。
- 答案匹配后，通过同一 Redisson 客户端和默认 codec 的 `RBucket.compareAndSet(code, null)` 原子比较删除。只有消费成功的请求继续组装权限和签发 JWT；已经消费、过期或被其他值替换时拒绝，也不会删除替换后的新码。错误码不执行比较删除。

验证码在校验通过时、权限组装及会话签发之前消费；若后续登录步骤失败，需要重新获取验证码。消费保障由 Redis/Redisson 原子操作提供；发送端的防重发检查→供应商调用→写缓存仍沿用单实例 synchronized，并未实现多实例发送锁。密码失败计数的并发读改写也沿用基线。

## 验证

```powershell
mvn -o -B -pl nla-admin -am '-Dtest=LoginStrategyContractTest,AuthSecurityContractTest,SecurityConfigTest,SaTokenFunctionTest,SysSocialOwnershipTest,SmsDataContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -o -B -DskipTests compile
```

`LoginStrategyContractTest` 新增 32 项发送/登录测试，共 73 项。测试执行真实 Controller、发送服务、发送引擎、短信认证、重试检查、权限组装与 JWT；用户/渠道/模板查询、发送记录 Mapper、供应商和 Redis 使用替身。覆盖发送与验证码一致、模板期限、重发保留、过期后重发、错误码、验证码复用、停用/缺失/锁定账号、无渠道/无模板/未注册通道/供应商拒绝和异常、无效模板、故障转移，以及比较删除失败时不签发会话、不误删新码。

与阶段 5.1/5.2 回归和 `SmsDataContractTest` 的 8 项真实 H2/Mapper 测试共同执行，**131 项全通过，无失败、错误或跳过**；JDK21 根工程 **49/49 模块编译成功**。日志 `.migration/test-sms-login-contract.log`、`.migration/build-sms-login-reactor.log`。

过期和并发竞争通过缓存替身模拟；未连接真实 MySQL、Redis 或短信供应商，未验收真实限流切面、TTL、多实例竞争、生产发送记录落库或完整应用启动。`AllUrlHandler` 的鉴权路径匹配沿用原逻辑，多租户保持禁用，旧账号/token/测试数据不迁。小程序绑定、二维码业务及外部环境验收仍待推进，阶段 5 保持进行中，pay 继续暂缓。
