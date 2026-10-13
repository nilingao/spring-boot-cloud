# nla-callcenter

呼叫中心业务模块：5 个管理域 CRUD（路由组/路由网关/外呼路由/注册网关/FS平台）、5 个协议 SPI 实现、坐席 SIP 服务、FreeSWITCH XML 拉取与运行时缓存装配，算法底座为 `nla-common-freeswitch`（阶段 3.1 交付的 SIP/ESL 封装，协议栈迁移自旧 spring-boot-fs）。模块已登记 reactor 与根 POM 坐标管理，并已接入 `nla-admin`。阶段 4 的数据层（41 套 Entity/Bo/Vo/Mapper）说明见下文；管理 CRUD 与 XML 链见「业务层（批次 6.6 第一轮）」；Socket.IO 坐席事件推送留批次 6.6 第二轮。

## 新库初始化

[`nla_callcenter.sql`](../../script/sql/nla_callcenter.sql) 用于 MySQL 8 新库初始化，表结构部分只有 `create table if not exists`；41 张表显式采用 utf8mb4 / utf8mb4_cs_0900_ai_ci。没有旧测试数据、节点凭据或 FreeSWITCH 自身的 cdr/sip/registrations 等运行表；文件末尾附「呼叫中心」菜单与角色授权种子。已有同名表不会自动升级；此次未执行外部数据库脚本。

| 分组 | 实体（表名保留旧 fs_*） |
|---|---|
| 媒体、平台和网关（7） | FsMediaServer、Platform、GateWay、RouteGroup、RouteGatewayGroup、RouteGateway、RouteCall |
| 企业（5） | Company、CompanyConference、CompanyDisplay、CompanyPhoneGroup、CompanyPhone |
| 技能组与技能（8） | Group、GroupAgentStrategy、GroupStrategyExp、GroupMemoryConfig、GroupOverflow、Skill、SkillAgent、SkillGroup |
| 座席（5） | Agent、UserAgent、AgentGroup、AgentSip、AgentStateLog |
| 通话和 IVR（6） | CallDetail、CallDevice、CallDtmf、CallLog、GroupMemory、IvrWorkflow |
| 溢出、播放和推送（5） | OverflowConfig、OverflowExp、OverflowFront、Playback、PushLog |
| 呼入路由（5） | VdnSchedule、VdnCode、VdnPhone、VdnConfig、VdnDtmf |

FsMediaServer 为旧 fs.MediaServer 消歧，区别于 video.VideoMediaServer。

全部实体继承 BaseEntity，五个审计列为 create_dept/create_by/create_time/update_by/update_time，允许无登录态初始化的空值；旧 Date 改 LocalDateTime。除 FsMediaServer 使用 String id / INPUT 外，其余主键是 Long / ASSIGN_ID，DDL 无自增。Bo 新增不能指定 Long 主键，修改必须传主键；字符串主键新增/修改均必填。Bo 不接受审计、删除和生成列。未新增租户列或物理外键。

## 类型与关联修正

- Company.balance 从旧 DDL 的 double 改为 decimal(19,4)，与 BigDecimal 对齐；Bo 限 15 位整数、4 位小数。不包含支付业务。
- 补齐旧 DDL 已有但实体遗漏的 Company.conferenceLimit 和 Agent.display。
- UserAgent.agentId / userId 改为 Long / bigint，必填且无默认值。旧 Mapper 实际按 UserAgent.agentId = Agent.id 关联，不能把座席工号 Agent.agentId 当成数据库主键；userId 对应系统用户数据库 ID。这样避免 varchar 与 bigint 隐式比较导致大于 2^53 的雪花 ID 精度丢失。新增 idx_user_agent_company_user(company_id,user_id,del_flag) 支撑用户查询。
- OverflowConfig.overflowValue 改为 Long / bigint，可承载技能组、IVR、VDN 的雪花目标 ID；协议 GroupOverFlowInfo 的字符串目标转换在运行时装配链完成（`MapstructUtils` 隐式同名转换）。
- Agent.agentId/agentKey/agentCode 和 AgentSip.sip 保留字符串。VdnConfig.routeValue 是混合号码/路由目标，保留 String；VdnSchedule 的日期、时间仍是字符串规则。CallLog/CallDevice/CallDetail 等时间戳、计数、时长保留 Long，`fristQueueTime` / `frist_queue_time` 原拼写保留。
- GateWay.username 保留旧 Integer / int，routeId 保留 String。

## 删除与唯一约束

全部 41 张表采用 String delFlag / char(1)，`@TableLogic(value="0",delval="1")`，默认 0。历史通话/事件表也遵循此约定，后续业务阶段需定义留存与物理清理策略。

保留 16 张表的 19 个原业务唯一索引名称、字段及原全局/企业范围，各唯一键追加数据库生成列 active_marker：有效行是 1，删除行是 NULL，支持多轮删除/重建。生成列不暴露到 Java 模型。原 company_name/company_code/uniq_skill_name 等遗留名称保留；VdnPhone 的 uni_idx_phone 原字段为 (vdn_id,company_id)，没有改成 phone。

FsMediaServer 的字符串主键自身仍唯一，删除后不能用相同 id 再 insert；需恢复原记录或使用新身份。数据层不自动级联、校验关联存在性或清理引用，后续 Service 必须按企业范围校验引用并事务性处理有效关联。逻辑删除保留物理行不代表业务删除流程已完成。

## 查询与敏感数据

- AgentMapper.selectByUserId(companyId,userId) 返回 AgentVo 列表，过滤有效企业、座席和同企业用户绑定；exists 避免重复绑定产生重复座席，按 create_time / id 倒序稳定排序。选择首条的业务规则留后续。
- AgentMapper.selectBySip(companyId,sip) 返回有效同企业座席，过滤企业、座席、SIP 三张表的删除状态；SIP 关联的是 Agent.id。
- GroupOverflowMapper.selectConfigsByGroup(companyId,groupId) 返回 OverflowConfigVo 列表，过滤企业、技能组、配置和关联四张表，要求配置与组属于同企业，按关联 level_value / id 排序，返回配置主键而非关联主键。
- AgentMapper 新增 findUserId / findUserModel 两条只读查询（批次 6.6 第一轮），分别按系统用户 ID 与 SIP 号取座席鉴权数据，供坐席服务与 XML 拉取使用；同样全参数 `#{}` 绑定并过滤参与表的删除状态。
- 全部参数使用 `#{}` 绑定；null、未知企业和错企业关联不匹配。查询没有 join sys_user，不承担系统用户存在性、账号状态或当前登录者权限校验；这些留业务层。上述查询不构成公开鉴权边界。
- passwd/password/sipPwd/secret/secretKey/notifyUrl/cdrNotifyUrl 在实体、Bo 中 JSON 仅写并排除 toString，Vo 无这些字段；内部实体读取仍保留协议所需凭据。Controller 必须返回 Vo。
- phone/caller/called/主被叫显示号码等字段在实体、Bo、Vo 的 toString 中排除，Vo 标注 `@Sensitive(PHONE)`。实际输出脱敏依赖既有 JsonValueEnhancer / SensitiveJsonFieldProcessor 和 SensitiveService 权限判定；没有 SensitiveService 时保留原值。裸 JSON 序列化、Vo getter 和数据库仍是原号码，生产响应链与权限服务必须在后续业务接入时验收。
- Agent.sipPhoneList 只放在 Vo，普通 CRUD 不计算。旧统计、缓存加载、策略引擎和协议 DTO 未直接复制。

## 业务层（批次 6.6 第一轮）

- 管理 CRUD：5 个管理域各有 `I*Service`/`*ServiceImpl`/Controller——路由组、路由网关、外呼路由、注册网关、FS平台，提供 list/分页、getInfo、add、edit、remove 与 optionselect 下拉。写路径统一做业务键查重（编辑排除自身）与状态/引用约束校验；路由组提供网关编组编辑（`RouteGroupGatewayEditBo`）。
- 协议 SPI：`spi/` 下 5 个 `*ServiceImpl` 实现 `nla-common-freeswitch` 的座席视图（AgentVoServiceImpl）、CDR 落库（CallCdrServiceImpl）、话单内存（GroupMemoryInfoServiceImpl）、平台信息（ParentPlatformServiceImpl）与坐席消息（SendAgentMessageImpl）契约，另有 `AgentService`/`AgentServiceImpl`（坐席 SIP 登录/登出/外呼/挂断）与包内转换器 `FsSpiConvert`。SPI 只读写本模块表，不再依赖旧微服务的 Feign 与静态 ServiceLocator。
- 运行时缓存装配：`config/fs/runtime/FsRuntimeConfigServiceImpl` 启动或企业变更时把 22 张管理表装配进 `RedisService` 静态门面的各 Info 缓存（企业/技能组/溢出策略/坐席策略/VDN/放音/会议），溢出策略实例化 3 种排队策略与 4 种坐席分配策略；管理域 Service 在事务提交后经 `FsRuntimeConfigRefreshPublisher` 发布 `FsCompanyConfigChangedEvent`，监听器定向刷新对应企业（详见 mapping.md 6.10）。
- XML 拉取：`FsXmlController` 暴露 3 个 FreeSWITCH 回拉端点（拨号计划/用户目录/配置项），渲染走 `nla-common-freeswitch` 的 Velocity 模板；`CompanyRunner` 启动时全量装配缓存。

## 门控

`freeswitch.enabled` 缺省不配置即关闭：`FsXmlController`、运行时装配服务、监听器与 `CompanyRunner`、5 个协议 SPI 实现与 `AgentServiceImpl` 都带 `@ConditionalOnProperty(prefix = "freeswitch", name = "enabled", havingValue = "true")`，协议组件在关闭时完全不创建。管理 CRUD 的 Service/Controller 与事件发布器不受门控，始终可用；停用时事件发布无监听者，管理功能不受影响。`nla-admin` 未新增 `freeswitch:` 配置块，`security.excludes` 已放行 `/fs/xml/**`（FS 服务器侧无登录态）。

## 接口与权限

5 个管理控制器统一路由 `/fs/{routeGroup,routeGateway,routeCall,gateway,platform}`，权限 `fs:{business}:{list,query,add,edit,remove}`（optionselect 复用 list，changeSelected 复用 edit）；写操作带 `@Log` 与 `AddGroup`/`EditGroup` 校验。菜单种子见 [`nla_callcenter.sql`](../../script/sql/nla_callcenter.sql) 的 `1761400000000005000` 号段（1 目录 + 5 菜单 + 20 按钮 + 26 条角色授权），前端页面由独立前端任务线补齐。

XML 拉取端点 `/fs/xml/{find_dialplan_xml,find_directory_xml,find_configuration_xml}` 匿名放行且挂门控，协议未启用时返回 404。

## 尚未交付

Socket.IO 坐席事件推送留批次 6.6 第二轮；真实 FreeSWITCH/SIP 联调（XML 拉取、坐席登录、外呼、运行时缓存命中率）、`nla-admin` 启动冒烟、`nla_callcenter.sql`（含菜单）在真实 MySQL 8 执行与前端 `fs/**` 页面均未验收。迁移状态以 [`docs/migration/mapping.md`](../../docs/migration/mapping.md) 6.10 为准。

## 验证

```powershell
.migration\fs-contract-test.ps1
.migration\fs-full-test.ps1
```

共 127 项测试：`CallcenterDataContractTest`(121) 加载交付 DDL（只回放 `create table` 段，菜单与角色授权属于 MySQL 交付脚本，并断言建表数与实体数一致）、解析真实 Mapper XML，对全部 41 个 Mapper 执行全字段 CRUD、审计填充、逻辑删除、生成映射、分组校验、唯一键冲突与三轮删除重建、企业隔离与凭据不回显。`FsCommonContractTest`(4) 锁定事件发布器去重/空值忽略、事件非空约束与下拉选项构造。`FsRuntimeConfigRefreshTest`(2) 用内存事务管理器验证 `@TransactionalEventListener(AFTER_COMMIT)` 语义（提交触发刷新、回滚不触发），不依赖数据库与协议栈。三者均不启动 Spring Boot、不连 SIP/ESL/Redis。全量回归入口 `.migration\fs-full-test.ps1`。

H2 2.4.240 的 MySQL 模式保留列定义和生成列表达式，剥离 MySQL 表级引擎/字符集/排序规则/行格式选项及 STORED 关键字；测试给索引名加表名前缀以适应 H2 的 schema 级索引命名。固定测试审计填充器不代表生产登录态。真实 MySQL 生成列/排序规则/索引性能、生产审计、权限、菜单执行与协议联调尚未验收。迁移状态以 [`docs/migration/mapping.md`](../../docs/migration/mapping.md) 为准。
