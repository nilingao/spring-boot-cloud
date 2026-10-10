# nla-message

短信渠道配置、模板和发送记录管理，发送底座为 sms4j；数据库配置由 `DbSmsReadConfig` 读取，`SmsChannelManager` 负责渠道选择和最新模板查询。发送入口接入登录/注册流程仍按迁移台账待定。

## 阶段 4 数据层校正

`SmsConfig` 和 `MobileMessageTemplate` 的删除标志已统一为 `String delFlag`，使用 `@TableLogic(value="0", delval="1")`；对应 DDL 为 `del_flag char(1) not null default '0'`。普通 Mapper 查询、供应商配置读取、可用渠道选择、最新模板和删除前引用校验均过滤删除项。

删除渠道前仍检查有效模板引用。删除最新模板后，发送链路读取剩余的最新模板；有效模板全部删除后，不再选择该模板对应的渠道。已删除实体不会通过 `updateById` 或重复删除恢复。

`MobileMessage` 是追加型发送记录，没有 delFlag，保留显式物理删除。

## 建表脚本

[`script/sql/nla_message.sql`](../../script/sql/nla_message.sql) 的三张表显式使用 `utf8mb4` / `utf8mb4_cs_0900_ai_ci`。渠道配置和模板采用 char(1) 删除标志；发送记录无删除标志。菜单和角色授权种子沿用原脚本。

该文件为初始化脚本，包含 drop table 和菜单种子。已有数据库升级应单独检查 schema，再修改列，不能以重新初始化代替升级。本轮只更新仓库文件，未对外部数据库执行 SQL。

对已有旧版短信表，先确认删除标志仅含 0/1、无 null，并确认当前字符集/排序规则：

```sql
select id, del_flag from sms_sms_config
where del_flag is null or del_flag not in (0, 1);
select id, del_flag from sms_mobile_message_template
where del_flag is null or del_flag not in (0, 1);
show create table sms_sms_config;
show create table sms_mobile_message_template;
show create table sms_mobile_message;
```

两条异常值查询均为空时，可在数据库升级流程中采用下列删除标志列变更（未在真实 MySQL 执行验收）：

```sql
alter table sms_sms_config
    modify column del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）';
alter table sms_mobile_message_template
    modify column del_flag char(1) not null default '0' comment '删除标志（0代表存在 1代表删除）';
```

这些 ALTER 只修改删除标志列；已有表的字符集/排序规则须另按实际 schema 纳入数据库升级。新初始化脚本中的表级声明不会自动改变已有表。

## 验证

```powershell
mvn -o -B -pl nla-modules/nla-message -am '-Dtest=SmsDataContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

`SmsDataContractTest` 8 项测试实际加载交付 DDL 的三张表，并执行真实 Mapper 及服务查询：String/char(1) 默认值与列定义、供应商读取排除删除渠道、最新模板回退、可用渠道排除删除模板/渠道及停用渠道、删除前引用校验和定向注销、批量删除与分页计数、禁止更新恢复删除项、发送记录物理删除。

测试使用 H2 2.4.240 MySQL 模式，剥离表级引擎/字符集/排序规则选项；不执行脚本中的 drop 和菜单种子。不启动完整 Spring 应用、不连接 Redis、不创建供应商客户端或真实发送短信。渠道注销回调用 mock 验证，实际逻辑删除及业务 SQL 使用真实 Mapper。

真实 MySQL 列转换/排序规则、完整应用启动与短信供应商联调仍需对应环境验收。迁移状态以 [`docs/migration/mapping.md`](../../docs/migration/mapping.md) 为准。
