# nla-video（阶段 4 数据层）

交付视频监控的 12 套实体、Bo、Vo、BaseMapperPlus 和两个平台关联查询。模块已登记 reactor 与根 POM 坐标管理；业务服务、Controller、权限、菜单和协议适配留阶段 6.5。本模块只依赖 nla-common-mybatis，不依赖 nla-common-video；尚未接入 nla-admin，不启动 SIP/ZLM/ONVIF。

## 新库初始化

[`script/sql/nla_video.sql`](../../script/sql/nla_video.sql) 用于 MySQL 8 新库初始化，只包含 `create table if not exists`。显式采用 utf8mb4 / utf8mb4_cs_0900_ai_ci，不包含旧测试数据、媒体节点凭据或菜单种子。已有同名表不会自动升级；此次未执行外部数据库脚本。

| 实体 | 表 / 主键 | 关键约束 |
|---|---|---|
| Device | video_device / Long id | device_id 有效记录唯一，保留旧 online 默认值 2 |
| DeviceChannel | video_device_channel / Long id | channel_id 在有效通道中全局唯一，保留 manufacture 列名 |
| DeviceAlarm | video_device_alarm / Long id | 设备/通道国标编号、报警时间与报警级别必填 |
| DeviceMobilePosition | video_device_mobile_position / Long id | 设备/通道国标编号、位置时间必填 |
| VideoMediaServer | video_media_server / String id | 调用方提供协议节点 ID；有效 (ip,http_port) 唯一 |
| ParentPlatform | video_parent_platform / Long id | 有效 server_gb_id 唯一 |
| PlatformCatalog | video_platform_catalog / String id | 调用方提供目录 ID |
| PlatformGbStream | video_platform_gb_stream / Long id | gb_stream_id 是 String 国标流编号，对应 GbStream.gbId |
| PlatformGbChannel | video_platform_gb_channel / Long id | device_channel_id 是 String 国标通道编号，对应 DeviceChannel.channelId |
| GbStream | video_gb_stream / Long gb_stream_id | 特殊主键列名保留，有效 (app,stream) 与 gb_id 分别唯一 |
| StreamProxy | video_stream_proxy / Long id | 有效 (app,stream) 唯一 |
| StreamPush | video_stream_push / Long id | 有效 (app,stream) 唯一；self 映射 onSelf，total_reader_count 保留 String |

VideoMediaServer 为旧 video.MediaServer 消歧，避免与 fs.MediaServer 混淆。关联表及目录的 platform_id 对应 ParentPlatform.serverGbId，不能传 ParentPlatform 的 Long 数据库主键。原非持久化字段 channelCount、platformId/catalogId、manufacturer/hostAddress、childrenCount/type 只保留在对应 Vo；普通 CRUD 不计算这些查询字段。DeviceChannel 的 ptzTypeText 独立存储，旧 setter 的派生文本逻辑留业务适配阶段。

全部实体继承 BaseEntity，采用 5 个审计列 create_dept/create_by/create_time/update_by/update_time，审计列允许无登录态初始化的空值；Date 改 LocalDateTime。Long 主键使用 ASSIGN_ID，String 主键使用 INPUT，Bo 新增必须传字符串主键。未新增租户列或物理外键。非空业务列、长度和默认值沿用旧 DDL；整数改 signed 与 Java Integer/Long 对齐，Bo 保留原 unsigned 字段非负校验及端口 0..65535 校验。

## 删除与唯一约束

12 张表采用 String delFlag / char(1)，`@TableLogic(value="0", delval="1")`，默认 0。报警和位置表当前也遵循阶段 4 的统一逻辑删除约定；大容量历史记录的清理与留存策略留业务阶段定义。

原唯一索引名称与业务字段保留，新增数据库生成列 active_marker：有效行是 1，删除行是 NULL。唯一键追加该生成列，只限制有效记录，支持多轮删除/重建；生成列不在实体或 Bo/Vo 中暴露。单纯 `(业务键,del_flag)` 会在第二次删除时撞上既有删除记录，因此未采用。字符串主键自身仍唯一，逻辑删除后不能用同一个 id 再 insert；恢复原节点/目录或换新身份的规则留业务阶段。

数据层不自动级联删除。后续 Service 仍须做引用校验，并事务性处理平台/目录/设备/通道/流关联。复用国标编号前须处理旧有效关联，防止旧引用指向新记录。删除后保留物理行不等于已完成业务删除流程。

## 查询与输入输出

- PlatformGbChannelMapper.selectSharedChannels(platformId,catalogIds) 返回 DeviceChannelVo；显式过滤关联、设备、通道、平台、目录五张表的 del_flag。
- PlatformGbStreamMapper.selectSharedStreams(platformId,catalogIds) 返回 GbStreamVo；按国标流编号关联，过滤关联、流、平台、目录四张表。
- 两个查询要求目录属于同一平台，使用绑定参数；null/空目录集合、未知平台或不匹配目录返回空列表。已删除或缺失的关联目标不返回，不以运行状态或权限条件替代删除过滤。
- 旧分页/树/目录事件/流状态聚合和原生删除 SQL 未直接复制；阶段 6.5 需按新的 Bo/Vo、分页、权限和审计契约重写。当前两个查询只提供数据读取，不构成公开业务接口或权限边界。
- Bo 使用 AddGroup/EditGroup 和 MapStruct-Plus，不接受审计、删除或生成列；Long 主键新增不接受自定义 ID、修改必填。Vo 不包含删除标志、password、secret 和原始 url/srcUrl/dstUrl。原始流地址可能携带账号或 token，后续展示可由业务层生成脱敏地址。
- 实体/Bo 的凭据和原始流地址可写入数据库，JSON 仅写、toString 排除；Mapper 实体读取保留凭据供内部协议适配使用。Controller 必须返回 Vo。

## 验证

```powershell
mvn -o -B -pl nla-modules/nla-video -am '-Dtest=VideoDataContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

测试加载交付 DDL，解析两个真实 Mapper XML，并对全部 12 个真实 Mapper 执行全字段 CRUD、审计填充和逻辑删除；覆盖生成 Bo→实体→Vo 映射、分组/长度/非负数/端口校验、凭据不回显、字符串主键、特殊 gb_stream_id 主键、两类国标关联、每张参与查询表的删除过滤、参数绑定、空集合、目录归属、全部唯一键和多次删除/重建。

H2 2.4.240 的 MySQL 模式保留列定义和生成列表达式，剥离 MySQL 表级引擎/字符集/排序规则/行格式选项及 STORED 关键字；仅在测试中给索引名加表名前缀，以适应 H2 的 schema 级索引命名。固定测试审计填充器不代表生产登录态。真实 MySQL 生成列/排序规则/索引性能、生产审计、权限、协议与管理接口尚未验收。
