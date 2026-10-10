# nla-callcenter（阶段 4 数据层）

呼叫中心 41 套 Entity、Bo、Vo、BaseMapperPlus，以及两个 Mapper XML 的三个只读查询。模块已登记 reactor 和根 POM 坐标管理，依赖 nla-common-mybatis / nla-common-sensitive。业务服务、Controller、菜单、权限和协议适配留阶段 6.6；尚未接入 nla-admin。

## 新库初始化

[`nla_callcenter.sql`](../../script/sql/nla_callcenter.sql) 用于 MySQL 8 新库初始化，只有 `create table if not exists`；41 张表显式采用 utf8mb4 / utf8mb4_cs_0900_ai_ci。没有旧测试数据、节点凭据、菜单种子或 FreeSWITCH 自身的 cdr/sip/registrations 等运行表。已有同名表不会自动升级；此次未执行外部数据库脚本。

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
- OverflowConfig.overflowValue 改为 Long / bigint，可承载技能组、IVR、VDN 的雪花目标 ID；协议 GroupOverFlowInfo 的字符串目标转换留业务适配。
- Agent.agentId/agentKey/agentCode 和 AgentSip.sip 保留字符串。VdnConfig.routeValue 是混合号码/路由目标，保留 String；VdnSchedule 的日期、时间仍是字符串规则。CallLog/CallDevice/CallDetail 等时间戳、计数、时长保留 Long，`fristQueueTime` / `frist_queue_time` 原拼写保留。
- GateWay.username 保留旧 Integer / int，routeId 保留 String；具体业务语义在后续迁移时核定。

## 删除与唯一约束

全部 41 张表采用 String delFlag / char(1)，`@TableLogic(value="0",delval="1")`，默认 0。历史通话/事件表也遵循此约定，后续业务阶段需定义留存与物理清理策略。

保留 16 张表的 19 个原业务唯一索引名称、字段及原全局/企业范围，各唯一键追加数据库生成列 active_marker：有效行是 1，删除行是 NULL，支持多轮删除/重建。生成列不暴露到 Java 模型。原 company_name/company_code/uniq_skill_name 等遗留名称保留；VdnPhone 的 uni_idx_phone 原字段为 (vdn_id,company_id)，没有改成 phone。

FsMediaServer 的字符串主键自身仍唯一，删除后不能用相同 id 再 insert；需恢复原记录或使用新身份。数据层不自动级联、校验关联存在性或清理引用，后续 Service 必须按企业范围校验引用并事务性处理有效关联。逻辑删除保留物理行不代表业务删除流程已完成。

## 查询与敏感数据

- AgentMapper.selectByUserId(companyId,userId) 返回 AgentVo 列表，过滤有效企业、座席和同企业用户绑定；exists 避免重复绑定产生重复座席，按 create_time / id 倒序稳定排序。选择首条的业务规则留后续。
- AgentMapper.selectBySip(companyId,sip) 返回有效同企业座席，过滤企业、座席、SIP 三张表的删除状态；SIP 关联的是 Agent.id。
- GroupOverflowMapper.selectConfigsByGroup(companyId,groupId) 返回 OverflowConfigVo 列表，过滤企业、技能组、配置和关联四张表，要求配置与组属于同企业，按关联 level_value / id 排序，返回配置主键而非关联主键。
- 全部参数使用 `#{}` 绑定；null、未知企业和错企业关联不匹配。查询没有 join sys_user，不承担系统用户存在性、账号状态或当前登录者权限校验；这些留业务层。三个查询不构成公开鉴权边界。
- passwd/password/sipPwd/secret/secretKey/notifyUrl/cdrNotifyUrl 在实体、Bo 中 JSON 仅写并排除 toString，Vo 无这些字段；内部实体读取仍保留协议所需凭据。Controller 必须返回 Vo。
- phone/caller/called/主被叫显示号码等字段在实体、Bo、Vo 的 toString 中排除，Vo 标注 `@Sensitive(PHONE)`。实际输出脱敏依赖既有 JsonValueEnhancer / SensitiveJsonFieldProcessor 和 SensitiveService 权限判定；没有 SensitiveService 时保留原值。裸 JSON 序列化、Vo getter 和数据库仍是原号码，生产响应链与权限服务必须在后续业务接入时验收。
- Agent.sipPhoneList 只放在 Vo，普通 CRUD 不计算。旧统计、缓存加载、策略引擎和协议 DTO 未直接复制。

## 验证

```powershell
mvn -o -B -pl nla-modules/nla-callcenter -am '-Dtest=CallcenterDataContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

测试加载交付 DDL、两个真实 XML 和全部 41 个 Mapper，覆盖全字段 CRUD、审计更新、逻辑删除、Bo→实体→Vo 全字段生成映射、凭据仅写、分组校验、精确金额、大整数关联、调度规则、数据库约束、企业隔离、每个查询参与表的删除过滤、重复绑定去重和稳定排序、19 个唯一键独立冲突及三轮删除重建、响应增强号码脱敏。

H2 2.4.240 MySQL 模式保留列定义和生成列表达式，剥离 MySQL 表级存储/字符集/排序规则/行格式选项及 STORED 关键字；测试中给索引名加表名前缀以适配 H2 的 schema 级命名。固定审计填充器不代表生产登录态。真实 MySQL 生成列、排序规则、索引性能、生产审计/鉴权及 ESL/SIP/媒体业务联调尚未验收。
