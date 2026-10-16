-- video 数据层：新库初始化，无旧测试数据、无 DROP、无协议开关。
-- active_marker：有效行为1，删除行为NULL；唯一索引只约束有效记录，支持多次删除/重建。
-- 字符串主键仍不可复用；平台/通道/流关联列存国标编号。

create table if not exists video_device (
    `id`                                  bigint not null COMMENT '主键id',
    `device_id`                           varchar(50)  NOT NULL COMMENT '设备国标编号',
    `name`                                varchar(255)  DEFAULT NULL COMMENT '设备名',
    `manufacturer`                        varchar(255)  DEFAULT NULL COMMENT '生产厂商',
    `model`                               varchar(255)  DEFAULT NULL COMMENT '型号',
    `custom_name`                         varchar(255)  DEFAULT NULL COMMENT '风格名词',
    `firmware`                            varchar(255)  DEFAULT NULL COMMENT '固件版本',
    `transport`                           tinyint  DEFAULT NULL COMMENT '传输协议 1.UDP 2.TCP',
    `ip`                                  varchar(50)  DEFAULT NULL COMMENT 'wan地址_ip',
    `port`                                int DEFAULT NULL COMMENT 'wan地址_port',
    `host_address`                        varchar(50)  DEFAULT NULL COMMENT 'wan地址',
    `password`                            varchar(255)  DEFAULT NULL COMMENT '设备密码',
    `sdp_ip`                              varchar(50)  DEFAULT NULL COMMENT '收流IP',
    `local_ip`                            varchar(50)  DEFAULT NULL COMMENT 'SIP交互IP（设备访问平台的IP）',
    `charset`                             tinyint  default 1 COMMENT '字符集, 1.UTF-8 2.GB2312',
    `tree_type`                           int  default 215 COMMENT '树类型 国标规定了两种树的展现方式 216.行政区划：CivilCode 215.业务分组:BusinessGroup',
    `geo_coord_sys`                       tinyint  default 1 COMMENT '地理坐标系， 目前支持 1.WGS84,2.GCJ02',
    `online`                              tinyint  default 2 COMMENT '是否在线，1.为在线，0.为离线',
    `register_time`                       datetime  DEFAULT NULL COMMENT '注册时间',
    `keepalive_time`                      datetime  DEFAULT NULL COMMENT '心跳时间',
    `heart_beat_interval`                 int default 30 COMMENT '心跳间隔 (最低25秒)',
    `heart_beat_count`                    int default 2 COMMENT '心跳间隔 (最低2次)',
    `position_capability`                 int default 0 COMMENT '定位功能支持情况。取值:0-不支持;1-支持 GPS定位;2-支持北斗定位(可选,默认取值为0',
    `stream_mode`                         tinyint  DEFAULT 0 COMMENT '数据流传输模式 0.UDP:udp传输 1.TCP-PASSIVE：tcp被动模式 2.TCP-ACTIVE：tcp主动模式',
    `expires`                             int DEFAULT 86400 COMMENT '注册有效期（单位：秒 默认1天）',
    `mobile_position_submission_interval` int DEFAULT '5' COMMENT '移动设备位置信息上报时间间隔,单位:秒,默认值5 (秒)',
    `subscribe_cycle_for_catalog`         int DEFAULT '0' COMMENT '目录订阅周期，0为不订阅 (秒)',
    `subscribe_cycle_for_mobile_position` int DEFAULT '0' COMMENT '移动设备位置订阅周期，0为不订阅 (秒)',
    `subscribe_cycle_for_alarm`           int DEFAULT '0' COMMENT '报警心跳时间订阅周期，0为不订阅 (秒)',
    `ssrc_check`                          tinyint DEFAULT '0' COMMENT '是否开启ssrc校验，默认关闭，开启可以防止串流',
    `media_server_id`                     varchar(255)  DEFAULT NULL COMMENT '流媒体编号 默认为null',
    `as_message_channel`                  tinyint DEFAULT '0' COMMENT '是否作为消息通道 1.是 0.否',
    `has_administrator`                   tinyint DEFAULT '0' COMMENT '是否开启超管权限(只有超级管理员才可查看)',
    `switch_primary_sub_stream`           tinyint DEFAULT '0' COMMENT '是否开启主子码流开关 1.是 0.否',
    `create_dept` bigint default null comment '创建部门',
    `create_by` bigint default null comment '创建者',
    `create_time` datetime default null comment '创建时间',
    `update_by` bigint default null comment '更新者',
    `update_time` datetime default null comment '更新时间',
    `del_flag` char(1) not null default '0' comment '0存在1删除',
    `active_marker` tinyint generated always as (case when del_flag = '0' then 1 else null end) stored,
    PRIMARY KEY (id),
    UNIQUE KEY idx_device_id (device_id, active_marker)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci row_format=dynamic comment='设备信息';

create table if not exists video_device_channel (
    `id`                  bigint not null COMMENT '主键id',
    `parent_id`           varchar(50)  DEFAULT NULL COMMENT '父级id',
    `channel_id`          varchar(50)  NOT NULL COMMENT '通道国标编号',
    `device_id`           varchar(50)  NOT NULL COMMENT '设备国标编号',
    `name`                varchar(255)  DEFAULT NULL COMMENT '通道名',
    `manufacture`         varchar(50)  DEFAULT NULL COMMENT '生产厂商',
    `model`               varchar(50)  DEFAULT NULL COMMENT '型号',
    `owner`               varchar(50)  DEFAULT NULL COMMENT '设备归属',
    `civil_code`          varchar(50)  DEFAULT NULL COMMENT '行政区域',
    `block`               varchar(50)  DEFAULT NULL COMMENT '警区',
    `address`             varchar(50)  DEFAULT NULL COMMENT '安装地址',
    `parental`            tinyint  DEFAULT 0 COMMENT '是否有子设备 1有, 0没有',
    `safety_way`          tinyint DEFAULT 0 COMMENT '信令安全模式  缺省为0; 0:不采用; 2: S/MIME签名方式; 3: S/ MIME加密签名同时采用方式; 4:数字摘要方式',
    `register_way`        tinyint DEFAULT 1 COMMENT '注册方式 缺省为1;1:符合IETFRFC3261标准的认证注册模 式; 2:基于口令的双向认证注册模式; 3:基于数字证书的双向认证注册模式',
    `cert_num`            varchar(50)  DEFAULT NULL COMMENT '证书序列号',
    `certifiable`         tinyint DEFAULT NULL COMMENT '证书有效标识 缺省为0;证书有效标识:0:无效1: 有效',
    `err_code`            int DEFAULT NULL COMMENT '证书无效原因码',
    `end_time`            datetime  DEFAULT NULL COMMENT '证书终止有效期',
    `secrecy`             tinyint  DEFAULT 0 COMMENT '保密属性 缺省为0; 0:不涉密, 1:涉密',
    `ip_address`          varchar(50)  DEFAULT NULL COMMENT 'IP地址',
    `port`                int DEFAULT NULL COMMENT '端口号',
    `password`            varchar(255)  DEFAULT NULL COMMENT '密码',
    `ptz_type`            tinyint DEFAULT 0 COMMENT '云台类型 0.未知 1.球机 2.半球 3.固定枪机 4.遥控枪机',
    `ptz_type_text`       varchar(255) DEFAULT NULL COMMENT '云台类型描述字符串',
    `status`              tinyint DEFAULT 0 COMMENT '在线/离线， 1在线,0离线',
    `longitude`           double DEFAULT 0 COMMENT '经度',
    `latitude`            double DEFAULT 0 COMMENT '纬度',
    `longitude_gcj02`     double DEFAULT 0 COMMENT '经度 GCJ02',
    `latitude_gcj02`      double DEFAULT 0 COMMENT '纬度 GCJ02',
    `longitude_wgs84`     double DEFAULT 0 COMMENT '经度 WGS84',
    `latitude_wgs84`      double DEFAULT 0 COMMENT '纬度 WGS84',
    `sub_count`           int DEFAULT '0' COMMENT '子设备数',
    `stream_id`           varchar(255)  DEFAULT NULL COMMENT '流唯一编号，存在表示正在直播',
    `has_audio`           tinyint DEFAULT 0 COMMENT '是否开启音频',
    `has_record`          tinyint DEFAULT 0 COMMENT '是否开启录像',
    `channel_type`        tinyint DEFAULT 0 COMMENT '标记通道的类型，0->国标通道 1->直播流通道 2->业务分组/虚拟组织/行政区划',
    `business_group_id`   varchar(50)  DEFAULT NULL COMMENT '业务分组',
    `gps_time`            datetime  DEFAULT NULL COMMENT 'GPS的更新时间',
    `create_dept` bigint default null comment '创建部门',
    `create_by` bigint default null comment '创建者',
    `create_time` datetime default null comment '创建时间',
    `update_by` bigint default null comment '更新者',
    `update_time` datetime default null comment '更新时间',
    `del_flag` char(1) not null default '0' comment '0存在1删除',
    `active_marker` tinyint generated always as (case when del_flag = '0' then 1 else null end) stored,
    PRIMARY KEY (id),
    UNIQUE KEY idx_channel_id (channel_id, active_marker),
    KEY idx_device_parent (device_id, parent_id, del_flag)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci row_format=dynamic comment='通道信息';

create table if not exists video_device_alarm (
    `id`                  bigint not null COMMENT '主键id',
    `device_id`           varchar(50)  NOT NULL COMMENT '设备的国标编号',
    `channel_id`          varchar(50)  NOT NULL COMMENT '通道的国标编号',
    `alarm_priority`      tinyint  NOT NULL COMMENT '报警级别, 1为一级警情, 2为二级警情, 3为三级警情, 4为四级警情',
    `alarm_method`        tinyint  DEFAULT NULL COMMENT '报警方式 , 1为电话报警, 2为设备报警, 3为短信报警, 4为 GPS报警, 5为视频报警, 6为设备故障报警,7其他报警;可以为直接组合如12为电话报警或 设备报警-',
    `alarm_time`          datetime  NOT NULL COMMENT '报警时间',
    `alarm_description`   varchar(255)  DEFAULT NULL COMMENT '报警内容描述',
    `longitude`           double DEFAULT NULL COMMENT '经度',
    `latitude`            double DEFAULT NULL COMMENT '纬度',
    `alarm_type`          tinyint  DEFAULT NULL COMMENT '报警类型 报警方式为2时， 1-视频丢失报警 2-设备防拆报警 3-存储设备磁盘满报警 4-设备高温报警 5-设备低温报警， 报警方式为5时,取值如下，1-人工视频报警 2-运动目标检测报警 3-遗留物检测报警 4-物体移除检测报警 5-绊线检测报警  6-入侵检测报警 7-逆行检测报警 8-徘徊检测报警 9-流量统计报警 10-密度检测报警 11-视频异常检测报警 12-快速移动报警 报警方式为6时,取值下， 1-存储设备磁盘故障报警 2-存储设备风扇故障报警' ,
    `create_dept` bigint default null comment '创建部门',
    `create_by` bigint default null comment '创建者',
    `create_time` datetime default null comment '创建时间',
    `update_by` bigint default null comment '更新者',
    `update_time` datetime default null comment '更新时间',
    `del_flag` char(1) not null default '0' comment '0存在1删除',
    PRIMARY KEY (id),
    KEY idx_channel_time (device_id, channel_id, alarm_time, del_flag)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci row_format=dynamic comment='报警信息';

create table if not exists video_device_mobile_position (
    `id`                  bigint not null COMMENT '主键id',
    `device_id`           varchar(50)  NOT NULL COMMENT '设备Id',
    `channel_id`          varchar(50)  NOT NULL COMMENT '通道Id',
    `device_name`         varchar(255)  DEFAULT NULL COMMENT '设备名称',
    `time`                datetime  NOT NULL COMMENT '通知时间',
    `longitude`           double default 0.00 COMMENT '经度',
    `latitude`            double default 0.00 COMMENT '纬度',
    `altitude`            double  default 0.00 COMMENT '海拔高度',
    `speed`               double  default 0.00 COMMENT '速度',
    `direction`           double  default 0.00 COMMENT '方向',
    `report_source`       varchar(50)  DEFAULT NULL COMMENT '位置信息上报来源（Mobile Position、GPS Alarm）',
    `longitude_gcj02`     double default 0.00 COMMENT '国内坐标系：经度坐标',
    `latitude_gcj02`      double default 0.00 COMMENT '国内坐标系：纬度坐标',
    `longitude_wgs84`     double default 0.00 COMMENT '国内坐标系：经度坐标',
    `latitude_wgs84`      double default 0.00 COMMENT '国内坐标系：纬度坐标',
    `create_dept` bigint default null comment '创建部门',
    `create_by` bigint default null comment '创建者',
    `create_time` datetime default null comment '创建时间',
    `update_by` bigint default null comment '更新者',
    `update_time` datetime default null comment '更新时间',
    `del_flag` char(1) not null default '0' comment '0存在1删除',
    PRIMARY KEY (id),
    KEY idx_channel_time (device_id, channel_id, `time`, del_flag)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci row_format=dynamic comment='移动位置信息';

create table if not exists video_media_server (
    `id`                      varchar(255)  NOT NULL COMMENT '主键id',
    `ip`                      varchar(50)  NOT NULL COMMENT 'IP',
    `ssl_status`              tinyint  default 0 COMMENT '是否https',
    `hook_ip`                 varchar(50)  NOT NULL COMMENT 'hook使用的IP（zlm访问WVP使用的IP）',
    `sdp_ip`                  varchar(50)  NOT NULL COMMENT 'SDP IP',
    `stream_ip`               varchar(50)  default null COMMENT '流IP',
    `http_port`               int default 0 COMMENT 'HTTP端口',
    `http_ssl_port`           int default 0 COMMENT 'HTTPS端口',
    `rtmp_port`               int default 0 COMMENT 'RTMP端口',
    `rtmp_ssl_port`           int default 0 COMMENT 'RTMPS端口',
    `rtp_proxy_port`          int default 0 COMMENT 'RTP收流端口（单端口模式有用）',
    `rtsp_port`               int default 0 COMMENT 'RTSP端口',
    `rtsp_ssl_port`           int default 0 COMMENT 'RTSPS端口',
    `auto_config`             tinyint default 1 COMMENT '是否开启自动配置ZLM',
    `secret`                  varchar(50)  default null COMMENT 'ZLM鉴权参数',
    `rtp_enable`              tinyint default 0 COMMENT '是否使用多端口模式',
    `enable`                  tinyint default 0 COMMENT '启用状态',
    `keepalive_time`          datetime  COMMENT '心跳时间',
    `status`                  tinyint default 0 COMMENT '状态',
    `rtp_port_range`          varchar(50)  default null COMMENT '多端口RTP收流端口范围',
    `record_assist_port`      int default 0 COMMENT 'assist服务端口',
    `default_server`          tinyint default 0 COMMENT '是否是默认ZLM',
    `hook_alive_interval`     int default 30 COMMENT 'keepalive hook触发间隔,单位秒',
    `video_play_prefix`       varchar(50)  default null COMMENT '流媒体播放 代理前缀',
    `video_http_prefix`       varchar(50)  default null COMMENT 'video请求时前缀',
    `create_dept` bigint default null comment '创建部门',
    `create_by` bigint default null comment '创建者',
    `create_time` datetime default null comment '创建时间',
    `update_by` bigint default null comment '更新者',
    `update_time` datetime default null comment '更新时间',
    `del_flag` char(1) not null default '0' comment '0存在1删除',
    `active_marker` tinyint generated always as (case when del_flag = '0' then 1 else null end) stored,
    PRIMARY KEY (id),
    UNIQUE KEY idx_http_port (ip,http_port, active_marker)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci row_format=dynamic comment='流媒体服务信息';

create table if not exists video_parent_platform (
    `id`                          bigint not null COMMENT '主键id',
    `enable`                      tinyint default 1 COMMENT '是否启用',
    `name`                        varchar(255)  DEFAULT NULL COMMENT '名称',
    `server_gb_id`                varchar(50)  NOT NULL COMMENT 'SIP服务国标编码',
    `server_gb_domain`            varchar(50)  DEFAULT NULL COMMENT 'SIP服务国标域',
    `server_ip`                   varchar(50)  DEFAULT NULL COMMENT 'SIP服务IP',
    `server_port`                 int DEFAULT NULL COMMENT 'SIP服务端口',
    `device_gb_id`                varchar(50)  NOT NULL COMMENT '设备国标编号',
    `device_ip`                   varchar(50)  DEFAULT NULL COMMENT '设备ip',
    `device_port`                 int  DEFAULT NULL COMMENT '设备端口',
    `username`                    varchar(255)  DEFAULT NULL COMMENT 'SIP认证用户名(默认使用设备国标编号)',
    `password`                    varchar(50)  DEFAULT NULL COMMENT 'SIP认证密码',
    `expires`                     int  DEFAULT NULL COMMENT '注册周期 (秒)',
    `keep_timeout`                int  DEFAULT NULL COMMENT '心跳周期(秒)',
    `transport`                   tinyint  default 1 COMMENT '传输协议 1.UDP 2.TCP',
    `character_set`               tinyint  default 2 COMMENT '字符集, 1.UTF-8 2.GB2312',
    `catalog_id`                  varchar(50)  NOT NULL COMMENT '默认目录Id,自动添加的通道多放在这个目录下',
    `catalog_group`               int DEFAULT '1' COMMENT '目录分组-每次向上级发送通道信息时单个包携带的通道数量，取值1,2,4,8',
    `ptz`                         tinyint default 1 COMMENT '是否允许云台控制',
    `rtcp`                        int DEFAULT NULL COMMENT 'RTCP流保活',
    `status`                      tinyint default 0 COMMENT '在线状态',
    `start_offline_push`          tinyint DEFAULT '0' COMMENT '点播未推流的设备时是否使用redis通知拉起',
    `administrative_division`     varchar(50)  default null COMMENT '行政区划',
    `tree_type`                   int  default 215 COMMENT '树类型 国标规定了两种树的展现方式 216.行政区划：CivilCode 215.业务分组:BusinessGroup',
    `as_message_channel`          tinyint DEFAULT '0' COMMENT '是否作为消息通道 1.是 0.否',
    `create_dept` bigint default null comment '创建部门',
    `create_by` bigint default null comment '创建者',
    `create_time` datetime default null comment '创建时间',
    `update_by` bigint default null comment '更新者',
    `update_time` datetime default null comment '更新时间',
    `del_flag` char(1) not null default '0' comment '0存在1删除',
    `active_marker` tinyint generated always as (case when del_flag = '0' then 1 else null end) stored,
    PRIMARY KEY (id),
    UNIQUE KEY idx_server_gb_id (server_gb_id, active_marker)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci row_format=dynamic comment='平台信息';

create table if not exists video_platform_catalog (
    `id`                          varchar(50)  NOT NULL COMMENT '主键id',
    `parent_id`                   varchar(50)  DEFAULT NULL COMMENT '父级目录ID',
    `name`                        varchar(255) NOT NULL COMMENT '名称',
    `platform_id`                 varchar(50)  NOT NULL COMMENT '平台ID',
    `civil_code`                  varchar(50)  DEFAULT NULL COMMENT '行政区划',
    `business_group_id`           varchar(50)  DEFAULT NULL COMMENT '目录分组',
    `create_dept` bigint default null comment '创建部门',
    `create_by` bigint default null comment '创建者',
    `create_time` datetime default null comment '创建时间',
    `update_by` bigint default null comment '更新者',
    `update_time` datetime default null comment '更新时间',
    `del_flag` char(1) not null default '0' comment '0存在1删除',
    PRIMARY KEY (id),
    KEY idx_platform_parent (platform_id, parent_id, del_flag)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci row_format=dynamic comment='国标级联-目录';

create table if not exists video_platform_gb_stream (
    `id`                          bigint not null COMMENT '主键id',
    `platform_id`                 varchar(50)  NOT NULL COMMENT '平台国标ID',
    `catalog_id`                  varchar(50)  NOT NULL COMMENT '目录ID',
    `gb_stream_id`                varchar(50) NOT NULL COMMENT '直播流国标ID',
    `create_dept` bigint default null comment '创建部门',
    `create_by` bigint default null comment '创建者',
    `create_time` datetime default null comment '创建时间',
    `update_by` bigint default null comment '更新者',
    `update_time` datetime default null comment '更新时间',
    `del_flag` char(1) not null default '0' comment '0存在1删除',
    `active_marker` tinyint generated always as (case when del_flag = '0' then 1 else null end) stored,
    PRIMARY KEY (id),
    UNIQUE KEY idx_gb_stream_id (platform_id,catalog_id,gb_stream_id, active_marker)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci row_format=dynamic comment='国标级联关联直播流';

create table if not exists video_platform_gb_channel (
    `id`                          bigint not null COMMENT '主键id',
    `platform_id`                 varchar(50)  NOT NULL COMMENT '平台国标ID',
    `catalog_id`                  varchar(50)  NOT NULL COMMENT '目录ID',
    `device_channel_id`           varchar(50)  NOT NULL COMMENT '通道国标ID',
    `create_dept` bigint default null comment '创建部门',
    `create_by` bigint default null comment '创建者',
    `create_time` datetime default null comment '创建时间',
    `update_by` bigint default null comment '更新者',
    `update_time` datetime default null comment '更新时间',
    `del_flag` char(1) not null default '0' comment '0存在1删除',
    `active_marker` tinyint generated always as (case when del_flag = '0' then 1 else null end) stored,
    PRIMARY KEY (id),
    UNIQUE KEY idx_device_channel_id (platform_id,catalog_id,device_channel_id, active_marker)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci row_format=dynamic comment='国标级联关联通道信息';

create table if not exists video_gb_stream (
    `gb_stream_id`        bigint not null COMMENT '主键id',
    `app`                 varchar(255)  NOT NULL COMMENT '应用名',
    `stream`              varchar(255)  NOT NULL COMMENT '流ID',
    `gb_id`               varchar(50)  NOT NULL COMMENT '国标ID',
    `name`                varchar(255)  DEFAULT NULL COMMENT '名称',
    `longitude`           double default 0.0 COMMENT '经度',
    `latitude`            double default 0.0 COMMENT '纬度',
    `stream_type`         tinyint  DEFAULT NULL COMMENT '流类型（1.拉流/2.推流）',
    `media_server_id`     varchar(50)  DEFAULT NULL COMMENT '流媒体ID',
    `create_dept` bigint default null comment '创建部门',
    `create_by` bigint default null comment '创建者',
    `create_time` datetime default null comment '创建时间',
    `update_by` bigint default null comment '更新者',
    `update_time` datetime default null comment '更新时间',
    `del_flag` char(1) not null default '0' comment '0存在1删除',
    `active_marker` tinyint generated always as (case when del_flag = '0' then 1 else null end) stored,
    PRIMARY KEY (gb_stream_id),
    UNIQUE KEY idx_stream (app,stream, active_marker),
    UNIQUE KEY idx_gb_id (gb_id, active_marker)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci row_format=dynamic comment='直播流关联国标上级平台';

create table if not exists video_stream_proxy (
    `id`                          bigint not null COMMENT '主键id',
    `type`                        tinyint  NOT NULL COMMENT '类型',
    `app`                         varchar(255)  NOT NULL COMMENT '应用名',
    `stream`                      varchar(255)  NOT NULL COMMENT '流ID',
    `name`                        varchar(255)  DEFAULT NULL COMMENT '名称',
    `status`                      tinyint NOT NULL COMMENT '状态',
    `media_server_id`             varchar(50)  DEFAULT NULL COMMENT '流媒体服务ID',
    `url`                         varchar(255)  DEFAULT NULL COMMENT '拉流地址',
    `src_url`                     varchar(255)  DEFAULT NULL COMMENT '拉流地址',
    `dst_url`                     varchar(255)  DEFAULT NULL COMMENT '目标地址',
    `timeout_ms`                  int DEFAULT NULL COMMENT '超时时间',
    `ffmpeg_cmd_key`              varchar(255)  DEFAULT NULL COMMENT 'ffmpeg模板KEY',
    `rtp_type`                    tinyint  default 1 COMMENT 'rtsp拉流时，拉流方式，0：tcp，1：udp，2：组播',
    `enable`                      tinyint default 0 COMMENT '是否启用',
    `enable_audio`                tinyint default 0 COMMENT '是否启用音频',
    `enable_mp4`                  tinyint default 0 COMMENT '是否启用MP4',
    `enable_remove_none_reader`   tinyint default 0 COMMENT '是否 无人观看时删除',
    `enable_disable_none_reader`  tinyint default 1 COMMENT '是否 无人观看时自动停用',
    `create_dept` bigint default null comment '创建部门',
    `create_by` bigint default null comment '创建者',
    `create_time` datetime default null comment '创建时间',
    `update_by` bigint default null comment '更新者',
    `update_time` datetime default null comment '更新时间',
    `del_flag` char(1) not null default '0' comment '0存在1删除',
    `active_marker` tinyint generated always as (case when del_flag = '0' then 1 else null end) stored,
    PRIMARY KEY (id),
    UNIQUE KEY idx_stream (app,stream, active_marker)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci row_format=dynamic comment='拉流代理的信息';

create table if not exists video_stream_push (
    `id`                          bigint not null COMMENT '主键id',
    `app`                         varchar(255)  NOT NULL COMMENT '应用名',
    `stream`                      varchar(255)  NOT NULL COMMENT '流id',
    `total_reader_count`          varchar(50)  DEFAULT NULL COMMENT '观看总人数，包括hls/rtsp/rtmp/http-flv/ws-flv',
    `origin_type`                 tinyint DEFAULT NULL COMMENT '产生源类型 unknown = 0,rtmp_push=1,rtsp_push=2,rtp_push=3,pull=4,ffmpeg_pull=5,mp4_vod=6,device_chn=7',
    `origin_type_str`             varchar(50)  DEFAULT NULL COMMENT '产生源类型的字符串描述',
    `alive_second`                int DEFAULT NULL COMMENT '存活时间，单位秒',
    `media_server_id`             varchar(50)  DEFAULT NULL COMMENT '使用的流媒体ID',
    `server_id`                   varchar(50)  NOT NULL COMMENT '使用的服务ID',
    `push_time`                   datetime  DEFAULT NULL COMMENT '推流时间',
    `status`                      tinyint DEFAULT NULL COMMENT '状态',
    `push_ing`                    tinyint default 0 COMMENT '是否正在推流',
    `self`                        tinyint default 1 COMMENT '是否自己平台的推流',
    `create_dept` bigint default null comment '创建部门',
    `create_by` bigint default null comment '创建者',
    `create_time` datetime default null comment '创建时间',
    `update_by` bigint default null comment '更新者',
    `update_time` datetime default null comment '更新时间',
    `del_flag` char(1) not null default '0' comment '0存在1删除',
    `active_marker` tinyint generated always as (case when del_flag = '0' then 1 else null end) stored,
    PRIMARY KEY (id),
    UNIQUE KEY idx_stream (app,stream, active_marker)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci row_format=dynamic comment='推流信息';

-- ----------------------------
-- 2、菜单权限（阶段 6.5 第一轮）
-- 依赖：需先执行 nla_system.sql（建 sys_menu / sys_role_menu 表并初始化角色），本脚本在其后运行
-- 号段：menu_id 采用 1761400000000004000 独立号段（face 用 3000 段，sms 用 2000 段，无冲突）
-- 授权：超级管理员(role 1761300000000000001)自动可见全部菜单，无需授权；下列授权面向普通角色 1761300000000000003(test1)，与 nla_system.sql 种子约定一致
-- 前端：component 路径(video/device/index 等)为前端页面预留，Vue 页面由独立前端任务线补齐；后端 @SaCheckPermission 鉴权不依赖前端页面是否存在
-- 门控：菜单与 video.enabled 无关，管理端 CRUD 始终可用；ZLM 回调 /index/hook/** 不占菜单，已在 security.excludes 匿名放行
-- ----------------------------
-- 视频监控目录
insert into sys_menu values(1761400000000004000, '视频监控', 0, 9, 'video', null, '', 'N', 'Y', 'M', '0', '0', '', 'camera', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '视频监控目录');
-- 视频监控菜单
insert into sys_menu values(1761400000000004001, '设备管理', 1761400000000004000, 1, 'device', 'video/device/index', '', 'N', 'Y', 'C', '0', '0', 'video:device:list', 'monitor', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '国标设备管理菜单');
insert into sys_menu values(1761400000000004002, '通道管理', 1761400000000004000, 2, 'deviceChannel', 'video/deviceChannel/index', '', 'N', 'Y', 'C', '0', '0', 'video:deviceChannel:list', 'list', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '设备通道管理菜单');
insert into sys_menu values(1761400000000004003, '报警记录', 1761400000000004000, 3, 'deviceAlarm', 'video/deviceAlarm/index', '', 'N', 'Y', 'C', '0', '0', 'video:deviceAlarm:list', 'message', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '设备报警记录菜单');
insert into sys_menu values(1761400000000004004, '移动位置', 1761400000000004000, 4, 'deviceMobilePosition', 'video/deviceMobilePosition/index', '', 'N', 'Y', 'C', '0', '0', 'video:deviceMobilePosition:list', 'international', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '设备移动位置轨迹菜单');
insert into sys_menu values(1761400000000004005, '流媒体服务器', 1761400000000004000, 5, 'mediaServer', 'video/mediaServer/index', '', 'N', 'Y', 'C', '0', '0', 'video:mediaServer:list', 'server', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '流媒体服务器管理菜单');
insert into sys_menu values(1761400000000004006, '国标流', 1761400000000004000, 6, 'gbStream', 'video/gbStream/index', '', 'N', 'Y', 'C', '0', '0', 'video:gbStream:list', 'video', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '国标流信息管理菜单');
insert into sys_menu values(1761400000000004007, '拉流代理', 1761400000000004000, 7, 'streamProxy', 'video/streamProxy/index', '', 'N', 'Y', 'C', '0', '0', 'video:streamProxy:list', 'online', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '拉流代理管理菜单');
insert into sys_menu values(1761400000000004008, '推流管理', 1761400000000004000, 8, 'streamPush', 'video/streamPush/index', '', 'N', 'Y', 'C', '0', '0', 'video:streamPush:list', 'upload', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '推流信息管理菜单');
insert into sys_menu values(1761400000000004009, '上级平台', 1761400000000004000, 9, 'parentPlatform', 'video/parentPlatform/index', '', 'N', 'Y', 'C', '0', '0', 'video:parentPlatform:list', 'peoples', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '国标上级平台管理菜单');
insert into sys_menu values(1761400000000004010, '平台目录', 1761400000000004000, 10, 'platformCatalog', 'video/platformCatalog/index', '', 'N', 'Y', 'C', '0', '0', 'video:platformCatalog:list', 'tree', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '平台目录管理菜单');
insert into sys_menu values(1761400000000004011, '共享通道', 1761400000000004000, 11, 'platformGbChannel', 'video/platformGbChannel/index', '', 'N', 'Y', 'C', '0', '0', 'video:platformGbChannel:list', 'tree-table', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '平台共享通道管理菜单');
insert into sys_menu values(1761400000000004012, '共享流', 1761400000000004000, 12, 'platformGbStream', 'video/platformGbStream/index', '', 'N', 'Y', 'C', '0', '0', 'video:platformGbStream:list', 'code', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '平台共享流管理菜单');
-- 设备管理按钮
insert into sys_menu values(1761400000000004101, '设备查询', 1761400000000004001, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:device:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004102, '设备新增', 1761400000000004001, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:device:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004103, '设备修改', 1761400000000004001, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:device:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004104, '设备删除', 1761400000000004001, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:device:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 通道管理按钮
insert into sys_menu values(1761400000000004111, '通道查询', 1761400000000004002, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:deviceChannel:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004112, '通道新增', 1761400000000004002, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:deviceChannel:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004113, '通道修改', 1761400000000004002, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:deviceChannel:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004114, '通道删除', 1761400000000004002, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:deviceChannel:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 报警记录按钮（上报数据只读，不提供新增与修改）
insert into sys_menu values(1761400000000004121, '报警查询', 1761400000000004003, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:deviceAlarm:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004122, '报警删除', 1761400000000004003, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:deviceAlarm:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 移动位置按钮（轨迹数据只读，不提供新增与修改）
insert into sys_menu values(1761400000000004131, '轨迹查询', 1761400000000004004, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:deviceMobilePosition:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004132, '轨迹删除', 1761400000000004004, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:deviceMobilePosition:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 流媒体服务器按钮
insert into sys_menu values(1761400000000004141, '节点查询', 1761400000000004005, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:mediaServer:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004142, '节点新增', 1761400000000004005, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:mediaServer:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004143, '节点修改', 1761400000000004005, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:mediaServer:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004144, '节点删除', 1761400000000004005, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:mediaServer:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 国标流按钮
insert into sys_menu values(1761400000000004151, '国标流查询', 1761400000000004006, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:gbStream:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004152, '国标流新增', 1761400000000004006, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:gbStream:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004153, '国标流修改', 1761400000000004006, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:gbStream:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004154, '国标流删除', 1761400000000004006, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:gbStream:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 拉流代理按钮
insert into sys_menu values(1761400000000004161, '代理查询', 1761400000000004007, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:streamProxy:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004162, '代理新增', 1761400000000004007, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:streamProxy:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004163, '代理修改', 1761400000000004007, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:streamProxy:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004164, '代理删除', 1761400000000004007, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:streamProxy:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 推流管理按钮
insert into sys_menu values(1761400000000004171, '推流查询', 1761400000000004008, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:streamPush:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004172, '推流新增', 1761400000000004008, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:streamPush:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004173, '推流修改', 1761400000000004008, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:streamPush:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004174, '推流删除', 1761400000000004008, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:streamPush:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 上级平台按钮
insert into sys_menu values(1761400000000004181, '平台查询', 1761400000000004009, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:parentPlatform:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004182, '平台新增', 1761400000000004009, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:parentPlatform:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004183, '平台修改', 1761400000000004009, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:parentPlatform:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004184, '平台删除', 1761400000000004009, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:parentPlatform:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 平台目录按钮
insert into sys_menu values(1761400000000004191, '目录查询', 1761400000000004010, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:platformCatalog:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004192, '目录新增', 1761400000000004010, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:platformCatalog:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004193, '目录修改', 1761400000000004010, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:platformCatalog:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004194, '目录删除', 1761400000000004010, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:platformCatalog:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 共享通道按钮
insert into sys_menu values(1761400000000004201, '共享通道查询', 1761400000000004011, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:platformGbChannel:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004202, '共享通道新增', 1761400000000004011, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:platformGbChannel:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004203, '共享通道修改', 1761400000000004011, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:platformGbChannel:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004204, '共享通道删除', 1761400000000004011, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:platformGbChannel:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 共享流按钮
insert into sys_menu values(1761400000000004211, '共享流查询', 1761400000000004012, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:platformGbStream:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004212, '共享流新增', 1761400000000004012, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:platformGbStream:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004213, '共享流修改', 1761400000000004012, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:platformGbStream:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000004214, '共享流删除', 1761400000000004012, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'video:platformGbStream:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 角色菜单授权（普通角色 test1；超级管理员自动可见，无需授权）
insert into sys_role_menu values (1761300000000000003, 1761400000000004000);
insert into sys_role_menu values (1761300000000000003, 1761400000000004001);
insert into sys_role_menu values (1761300000000000003, 1761400000000004002);
insert into sys_role_menu values (1761300000000000003, 1761400000000004003);
insert into sys_role_menu values (1761300000000000003, 1761400000000004004);
insert into sys_role_menu values (1761300000000000003, 1761400000000004005);
insert into sys_role_menu values (1761300000000000003, 1761400000000004006);
insert into sys_role_menu values (1761300000000000003, 1761400000000004007);
insert into sys_role_menu values (1761300000000000003, 1761400000000004008);
insert into sys_role_menu values (1761300000000000003, 1761400000000004009);
insert into sys_role_menu values (1761300000000000003, 1761400000000004010);
insert into sys_role_menu values (1761300000000000003, 1761400000000004011);
insert into sys_role_menu values (1761300000000000003, 1761400000000004012);
insert into sys_role_menu values (1761300000000000003, 1761400000000004101);
insert into sys_role_menu values (1761300000000000003, 1761400000000004102);
insert into sys_role_menu values (1761300000000000003, 1761400000000004103);
insert into sys_role_menu values (1761300000000000003, 1761400000000004104);
insert into sys_role_menu values (1761300000000000003, 1761400000000004111);
insert into sys_role_menu values (1761300000000000003, 1761400000000004112);
insert into sys_role_menu values (1761300000000000003, 1761400000000004113);
insert into sys_role_menu values (1761300000000000003, 1761400000000004114);
insert into sys_role_menu values (1761300000000000003, 1761400000000004121);
insert into sys_role_menu values (1761300000000000003, 1761400000000004122);
insert into sys_role_menu values (1761300000000000003, 1761400000000004131);
insert into sys_role_menu values (1761300000000000003, 1761400000000004132);
insert into sys_role_menu values (1761300000000000003, 1761400000000004141);
insert into sys_role_menu values (1761300000000000003, 1761400000000004142);
insert into sys_role_menu values (1761300000000000003, 1761400000000004143);
insert into sys_role_menu values (1761300000000000003, 1761400000000004144);
insert into sys_role_menu values (1761300000000000003, 1761400000000004151);
insert into sys_role_menu values (1761300000000000003, 1761400000000004152);
insert into sys_role_menu values (1761300000000000003, 1761400000000004153);
insert into sys_role_menu values (1761300000000000003, 1761400000000004154);
insert into sys_role_menu values (1761300000000000003, 1761400000000004161);
insert into sys_role_menu values (1761300000000000003, 1761400000000004162);
insert into sys_role_menu values (1761300000000000003, 1761400000000004163);
insert into sys_role_menu values (1761300000000000003, 1761400000000004164);
insert into sys_role_menu values (1761300000000000003, 1761400000000004171);
insert into sys_role_menu values (1761300000000000003, 1761400000000004172);
insert into sys_role_menu values (1761300000000000003, 1761400000000004173);
insert into sys_role_menu values (1761300000000000003, 1761400000000004174);
insert into sys_role_menu values (1761300000000000003, 1761400000000004181);
insert into sys_role_menu values (1761300000000000003, 1761400000000004182);
insert into sys_role_menu values (1761300000000000003, 1761400000000004183);
insert into sys_role_menu values (1761300000000000003, 1761400000000004184);
insert into sys_role_menu values (1761300000000000003, 1761400000000004191);
insert into sys_role_menu values (1761300000000000003, 1761400000000004192);
insert into sys_role_menu values (1761300000000000003, 1761400000000004193);
insert into sys_role_menu values (1761300000000000003, 1761400000000004194);
insert into sys_role_menu values (1761300000000000003, 1761400000000004201);
insert into sys_role_menu values (1761300000000000003, 1761400000000004202);
insert into sys_role_menu values (1761300000000000003, 1761400000000004203);
insert into sys_role_menu values (1761300000000000003, 1761400000000004204);
insert into sys_role_menu values (1761300000000000003, 1761400000000004211);
insert into sys_role_menu values (1761300000000000003, 1761400000000004212);
insert into sys_role_menu values (1761300000000000003, 1761400000000004213);
insert into sys_role_menu values (1761300000000000003, 1761400000000004214);
