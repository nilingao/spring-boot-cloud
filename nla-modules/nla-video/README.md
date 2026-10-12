# nla-video

交付视频监控 12 张表的管理 CRUD、国标协议 SPI 实现与 ZLMediaKit 回调入口，算法底座为 [`nla-common-video`](../../nla-common/nla-common-video/README.md)（阶段 3.2 交付的 GB28181/SIP + ZLMediaKit 封装）。模块已登记 reactor 与根 POM 坐标管理，并已接入 `nla-admin`。阶段 4 的数据层（12 套 Entity/Bo/Vo/Mapper）说明见下文；SIP 点播/回放/云台/报警应答等实时指令端点留后续批次。

## 新库初始化

[`script/sql/nla_video.sql`](../../script/sql/nla_video.sql) 用于 MySQL 8 新库初始化，表结构部分只包含 `create table if not exists`，显式采用 utf8mb4 / utf8mb4_cs_0900_ai_ci，不包含旧测试数据与媒体节点凭据；文件末尾附「视频监控」菜单与角色授权种子。已有同名表不会自动升级；此次未执行外部数据库脚本。

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
- 旧分页/树/目录事件/流状态聚合和原生删除 SQL 未直接复制，阶段 6.5 第一轮已按新的 Bo/Vo、分页、权限和审计契约重写为 Service + 自定义 XML（见下方「业务层」）。
- Bo 使用 AddGroup/EditGroup 和 MapStruct-Plus，不接受审计、删除或生成列；Long 主键新增不接受自定义 ID、修改必填。Vo 不包含删除标志、password、secret 和原始 url/srcUrl/dstUrl。原始流地址可能携带账号或 token，后续展示可由业务层生成脱敏地址。
- 实体/Bo 的凭据和原始流地址可写入数据库，JSON 仅写、toString 排除；Mapper 实体读取保留凭据供内部协议适配使用。Controller 必须返回 Vo。

## 业务层（批次 6.5 第一轮）

- 管理 CRUD：12 张表各有 `I*Service`/`*ServiceImpl`，提供 list/分页、getInfo、add、edit、remove。写路径统一做业务键查重（编辑排除自身）与删除前引用校验：设备←通道、通道←级联共享关系、目录←子目录/级联通道与流/上级平台共享根目录、上级平台←目录与级联关系、流媒体节点←设备/直播流/拉流代理/推流、直播流←级联共享。报警与移动位置是协议侧写入的记录表，只开放查询与删除端点（`insertByBo` 供 SPI 使用）。
- 协议 SPI：`spi/` 下 10 个 `*VoServiceImpl` 实现 `nla-common-video` 的设备/通道/报警/移动位置/流媒体/上级平台/目录/国标流/拉流代理/推流视图服务，另有 `TokenServiceImpl`（播放与推流鉴权改用单体 Sa-Token 登录态）、`VideoLoginUserProvider`（把登录用户交给点播/下载流程记账）、`PlatformShareHelper`（级联共享清理）和 `VideoSpiConvert`。SPI 只读写本模块 12 张表，不再依赖旧微服务的 Feign 与静态 ServiceLocator。
- 自定义 XML：6 份新增 Mapper XML 承担通道级联目录查询、子通道计数重算、平台反查通道、国标流伪装通道列表、设备在线统计与推流状态聚合，全部语句显式过滤 `del_flag = '0'`。
- 回调入口：`MediaHookController` 暴露 9 个 ZLMediaKit hook（keepalive/play/publish/stream_changed/none_reader/not_found/server_started/send_rtp_stopped/rtp_server_timeout），路径取自 `ZLMediaKitConstant`，直接委派 `MediaHookServer`；调用方是流媒体服务器、没有登录态，因此 `/index/hook/**` 已在 `nla-admin` 的 `security.excludes` 匿名放行。

## 门控

`video.enabled` 缺省不配置即关闭：`nla-common-video` 的 `SipConfig` 自动配置、本模块全部 SPI 实现与 `MediaHookController` 都带 `@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")`，协议组件与 SIP 监听端口在关闭时完全不创建。管理 CRUD 的 Service/Controller 不受门控，始终可用。

## 接口与权限

12 个管理控制器统一路由 `/video/{device,deviceChannel,deviceAlarm,deviceMobilePosition,mediaServer,gbStream,streamProxy,streamPush,parentPlatform,platformCatalog,platformGbChannel,platformGbStream}`，权限 `video:{business}:{list,query,add,edit,remove}`（报警与移动位置无 add/edit）；写操作带 `@Log`、`@RepeatSubmit` 与 `AddGroup`/`EditGroup` 校验。菜单种子见 [`script/sql/nla_video.sql`](../../script/sql/nla_video.sql) 的 `1761400000000004000` 号段（1 目录 + 12 菜单 + 44 按钮 + 57 条角色授权），前端页面由独立前端任务线补齐。

## 尚未交付

SIP 实时指令（点播/回放/云台/报警应答/目录订阅）、Excel 导出与语音对讲未实现，语音对讲的 3 个 SPI 方法返回 `RespCode.CODE_2` 占位提示。`queryOnlineList`/`queryEnableList`/`queryByGbId`/`queryByServerGbId` 等协议侧查询已在 Service 暴露但未开放 HTTP 端点，随实时指令批次一并铺设。

## 验证

```powershell
.migration\test-video.ps1
```

共 57 项测试：`VideoDataContractTest`(47) 加载交付 DDL，解析真实 Mapper XML，对全部 12 个真实 Mapper 执行全字段 CRUD、审计填充和逻辑删除；覆盖生成 Bo→实体→Vo 映射、分组/长度/非负数/端口校验、凭据不回显、字符串主键、特殊 gb_stream_id 主键、两类国标关联、每张参与查询表的删除过滤、参数绑定、空集合、目录归属、全部唯一键和多次删除/重建。`VideoServiceContractTest`(10) 在同一个 H2 库上真实执行管理 Service 与 SPI 契约：业务键查重、编辑排除自身、删除前引用校验、级联共享清理、自定义 XML 查询、子通道计数重算与语音对讲占位；两者均不启动 Spring Boot、不连 SIP/ZLM/Redis，`VideoSpringSupport` 只预装最小容器以满足 MapStruct-Plus 与 JsonUtils 的静态初始化。全量回归入口 `.migration\test-video-all.ps1`。

H2 2.4.240 的 MySQL 模式保留列定义和生成列表达式，剥离 MySQL 表级引擎/字符集/排序规则/行格式选项及 STORED 关键字；测试只回放 `create table` 段（菜单与角色授权属于 MySQL 交付脚本），并给索引名加表名前缀以适应 H2 的 schema 级索引命名。固定测试审计填充器不代表生产登录态。真实 MySQL 生成列/排序规则/索引性能、生产审计、权限、菜单执行与协议联调尚未验收。迁移状态以 [`docs/migration/mapping.md`](../../docs/migration/mapping.md) 为准。
