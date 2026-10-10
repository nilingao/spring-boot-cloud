-- 阶段 4 数据层第一批：人员 face_person。
-- 新库初始化脚本，不导入旧测试数据，不执行删除或更新已有表结构。
-- 雪花 ID、BaseEntity 审计字段、String delFlag，去旧 create_user_id/update_user_id。
-- extract 保留字符串契约，扩为 longtext，特征由后续识别业务写入。
-- 没有菜单/角色种子，HTTP 接口与业务服务留阶段 6.4。
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
