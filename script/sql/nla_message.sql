-- ----------------------------
-- nla-message 短信模块建表脚本
-- 表驱动短信：渠道配置(sms_sms_config) + 模板(sms_mobile_message_template) + 发送记录(sms_mobile_message)
-- 约定：主键雪花(ASSIGN_ID)、审计字段自动填充、逻辑删除 del_flag(0存在 1删除，发送记录为追加型不做逻辑删除)
-- 说明：渠道类型 sms_type=2(创蓝网)已废弃；新增 app_id 列承载腾讯云 sdkAppId / 容联云通讯 appId（替代旧硬编码常量）
-- 初始化：三表不预置数据，需在管理端「短信渠道配置」录入真实账号并配置模板后方可发送
-- ----------------------------

-- ----------------------------
-- 1、短信渠道配置表
-- ----------------------------
drop table if exists sms_sms_config;
create table sms_sms_config (
    id                bigint(20)      not null                   comment '主键',
    sms_type          int(2)          default null               comment '渠道类型（1短信网 3维纳多 4商务领航 5阿里云大于 6网易易盾 7云通讯 8腾讯云；2创蓝网已废弃）',
    config_name       varchar(100)    default ''                 comment '配置名称',
    account           varchar(255)    default ''                 comment '账号',
    password          varchar(255)    default ''                 comment '密码/密钥（只写不读，不返回前端）',
    app_id            varchar(255)    default ''                 comment '应用ID（腾讯云 sdkAppId / 容联云通讯 appId）',
    balance           varchar(50)     default ''                 comment '余额',
    is_active         int(1)          default 1                  comment '是否启用（0停用 1启用）',
    sign              varchar(100)    default ''                 comment '签名',
    sign_place        int(1)          default 1                  comment '签名位置（1左边 2右边）',
    del_flag          bigint(20)      default 0                  comment '删除标志（0代表存在 1代表删除）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (id),
    key idx_sms_config_type   (sms_type),
    key idx_sms_config_active (is_active)
) engine=innodb comment = '短信渠道配置表';


-- ----------------------------
-- 2、短信模板表
-- ----------------------------
drop table if exists sms_mobile_message_template;
create table sms_mobile_message_template (
    id                bigint(20)      not null                   comment '主键',
    config_id         bigint(20)      default null               comment '短信渠道配置id',
    code              varchar(255)    default ''                 comment '模板编号（多渠道模板号串，如 6:SMS_83975030,7:13824）',
    type              int(2)          default null               comment '模板类型（1登录 2注册 3重置）',
    title             varchar(100)    default ''                 comment '标题',
    content           varchar(500)    default ''                 comment '内容（含变量占位）',
    receiver          varchar(255)    default ''                 comment '接收人',
    variable          varchar(1000)   default null               comment '变量定义（JSON）',
    del_flag          bigint(20)      default 0                  comment '删除标志（0代表存在 1代表删除）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (id),
    key idx_sms_template_config (config_id),
    key idx_sms_template_type   (type)
) engine=innodb comment = '短信模板表';


-- ----------------------------
-- 3、短信发送记录表（追加型，物理删除，无 del_flag）
-- ----------------------------
drop table if exists sms_mobile_message;
create table sms_mobile_message (
    id                bigint(20)      not null                   comment '主键',
    sender_id         bigint(20)      default null               comment '发送人id（关联 sms_sms_config.id）',
    template_id       varchar(255)    default ''                 comment '模板号',
    type              int(2)          default null               comment '类型（1登录 2注册 3重置）',
    content           varchar(500)    default ''                 comment '内容',
    mobile            varchar(20)     default ''                 comment '手机号',
    handle_time       datetime                                   comment '操作时间',
    status            int(1)          default 0                  comment '状态（0失败 1成功）',
    msg_id            varchar(100)    default ''                 comment '响应编号',
    callback_status   varchar(50)     default ''                 comment '返回状态（回执）',
    resend_num        int(4)          default 0                  comment '重发次数',
    variable          varchar(1000)   default null               comment '变量（JSON）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (id),
    key idx_sms_message_mobile (mobile),
    key idx_sms_message_type   (type),
    key idx_sms_message_ct     (create_time)
) engine=innodb comment = '短信发送记录表';
