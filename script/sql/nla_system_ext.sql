-- ----------------------------
-- nla-system 扩展表（迁移批次 6.1：旧 spring-boot-bean → nla-modules/nla-system）
--
-- 决策依据（详见 docs/migration/mapping.md 6.1）：
--   1) bean_* 核心表（user/dept/role/menu/post/dict + 关联表）已由基线 sys_* 完整覆盖，
--      老数据（测试数据）一律丢弃、不迁移；
--   2) bean_mini / bean_mini_user（微信小程序 + 绑定）由基线 sys_social（openId/userId/nickName/avatar/source）
--      + sys_user 覆盖，不新建；
--   3) bean_user_set（is_admin/is_enabled）由基线 sys_user.status + 超级管理员角色覆盖，不新建；
--   4) 仅 sys_area（行政区划）为基线缺口，迁移保留为「只读业务参考表」。
--
-- 数据：sys_area 为国标行政区划参考数据（旧库 67 万余行），批量导入延后处理，不随本脚本入库；
--       Easy-ES 全文检索（旧 bean_sys_area 索引）延后至阶段4（nla-common-elasticsearch）再评估。
-- 依赖：菜单 / 角色授权部分需先执行 nla_system.sql（建 sys_menu / sys_role_menu 并初始化角色）。
-- ----------------------------

-- ----------------------------
-- 1、行政区划表
-- ----------------------------
drop table if exists sys_area;
create table sys_area (
    area_id    bigint(20)  not null              comment '地区Id',
    parent_id  bigint(20)  default 0             comment '父级地区Id(0为顶级)',
    area_code  varchar(50) not null              comment '地区编码',
    area_name  varchar(50) not null              comment '地区名称',
    level      tinyint(4)  default 1             comment '地区级别(1省 2市 3区县 4街道)',
    city_code  varchar(50) default null          comment '城市编码',
    center     varchar(50) default null          comment '城市中心点(经纬度坐标)',
    primary key (area_id),
    key idx_area_code (area_code),
    key idx_parent_id (parent_id),
    key idx_level (level),
    key idx_area_name (area_name)
) engine=innodb comment = '行政区划表';

-- ----------------------------
-- 2、菜单权限（行政区划，只读：列表 + 查询）
-- 挂在「系统管理」(1761400000000000001) 下，order_num=12（现有子菜单最大为 11）
-- menu_id 采用 1761400000000002100 号段（短信批次 6.3 用 2000 号段，见 nla_message.sql，无冲突）
-- 授权：超级管理员(1761300000000000001)自动可见全部菜单；下列面向普通角色 test1(1761300000000000003)，与 nla_system.sql 种子约定一致
-- 前端：component 路径(system/area/index)为前端页面预留，Vue 页面由独立前端任务线补齐
-- ----------------------------
insert into sys_menu values(1761400000000002100, '行政区划', 1761400000000000001, 12, 'area', 'system/area/index', '', 'N', 'Y', 'C', '0', '0', 'system:area:list', 'international', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '行政区划菜单');
insert into sys_menu values(1761400000000002101, '行政区划查询', 1761400000000002100, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:area:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 角色菜单授权
insert into sys_role_menu values (1761300000000000003, 1761400000000002100);
insert into sys_role_menu values (1761300000000000003, 1761400000000002101);
