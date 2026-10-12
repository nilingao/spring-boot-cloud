-- 阶段 4 数据层第一批：人员 face_person。
-- 新库初始化脚本，不导入旧测试数据，不执行删除或更新已有表结构。
-- 雪花 ID、BaseEntity 审计字段、String delFlag，去旧 create_user_id/update_user_id。
-- extract 保留字符串契约，扩为 longtext，特征由识别业务写入（阶段 6.4 已落地 CRUD/检测服务）。
-- 菜单/角色种子随阶段 6.4 补充；通用识别端点（/face/detector）仅需登录，不占菜单。
create table if not exists face_person (
    id              bigint        not null              comment '雪花主键',
    img_id          varchar(40)   not null              comment '图片自定义编号',
    img_url         varchar(256)  not null              comment '图片地址',
    `extract`       longtext      not null              comment '内部人脸特征数组字符串',
    person_name     varchar(40)   not null              comment '人员姓名',
    person_age      int unsigned  default null          comment '年龄',
    gender          tinyint       not null default 0    comment '性别（0未知 1男 2女）',
    address         varchar(256)  not null default ''   comment '地址',
    del_flag        char(1)       not null default '0'  comment '删除标志（0存在 1删除）',
    create_dept     bigint        default null          comment '创建部门',
    create_by       bigint        default null          comment '创建者',
    create_time     datetime      default null          comment '创建时间',
    update_by       bigint        default null          comment '更新者',
    update_time     datetime      default null          comment '更新时间',
    primary key (id),
    key idx_face_person_img_id (img_id, del_flag)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_cs_0900_ai_ci
  row_format=dynamic comment='人员信息';

-- ----------------------------
-- 2、菜单权限（阶段 6.4）
-- 依赖：需先执行 nla_system.sql（建 sys_menu / sys_role_menu 表并初始化角色），本脚本在其后运行
-- 号段：menu_id 采用 1761400000000003000 独立号段（sms 用 1761400000000002000 段，无冲突）
-- 授权：超级管理员(role 1761300000000000001)自动可见全部菜单，无需授权；下列授权面向普通角色 1761300000000000003(test1)，与 nla_system.sql 种子约定一致
-- 前端：component 路径(face/person/index)为前端页面预留，Vue 页面由独立前端任务线补齐；后端 @SaCheckPermission 鉴权不依赖前端页面是否存在
-- ----------------------------
-- 人脸识别目录
insert into sys_menu values(1761400000000003000, '人脸识别', 0, 7, 'face', null, '', 'N', 'Y', 'M', '0', '0', '', 'peoples', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '人脸识别目录');
-- 人脸识别菜单
insert into sys_menu values(1761400000000003001, '人员管理', 1761400000000003000, 1, 'person', 'face/person/index', '', 'N', 'Y', 'C', '0', '0', 'face:person:list', 'user', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '人员信息管理菜单');
-- 人员管理按钮
insert into sys_menu values(1761400000000003010, '人员查询', 1761400000000003001, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'face:person:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000003011, '人员新增', 1761400000000003001, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'face:person:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000003012, '人员修改', 1761400000000003001, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'face:person:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000003013, '人员删除', 1761400000000003001, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'face:person:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000003014, '人员导出', 1761400000000003001, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'face:person:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 角色菜单授权（普通角色 test1；超级管理员自动可见，无需授权）
insert into sys_role_menu values (1761300000000000003, 1761400000000003000);
insert into sys_role_menu values (1761300000000000003, 1761400000000003001);
insert into sys_role_menu values (1761300000000000003, 1761400000000003010);
insert into sys_role_menu values (1761300000000000003, 1761400000000003011);
insert into sys_role_menu values (1761300000000000003, 1761400000000003012);
insert into sys_role_menu values (1761300000000000003, 1761400000000003013);
insert into sys_role_menu values (1761300000000000003, 1761400000000003014);
