---
trigger: model_decision
description: 两部分：(1) 项目代码中确实不存在、禁止凭通用经验臆造的缺失清单——金额字段约定、防重提交注解、注解式分布式锁 Redisson、方法级权限注解、覆盖率门禁 JaCoCo、代码风格工具 Checkstyle / Spotless、数据库版本管理 Flyway、API 版本化 /v1/、国际化 i18n、行级数据权限等，需要时必须先问用户；(2) fs 呼叫中心模块 41 张表的页面归属映射（平台、媒体服务器、网关、路由、企业资源、技能组、坐席、IVR、VDN、通话记录等分组）与规范变更记录。加载场景：打算使用上述任一机制前先确认是否存在、开发或修改 fs 模块功能时确定表与页面的归属关系。
---

# 缺失清单（查不到就别编）与 fs 模块 41 张表页面归属附录

> 规范分册之一（原 `project-conventions.md` 按章拆分而来，结论均基于 `5.0.0-dev` 分支代码实证）。常驻铁律与全部分册索引见 `.qoder/rules/project-baseline.md`。**本册就是“查不到就别编”的权威清单**，需要下列机制时必须先问用户。

## 十一、缺失与待补充清单

以下项目**代码中不存在**。需要时必须先问用户，**禁止凭通用经验臆造**。

| # | 项 | 状态 | 说明 |
|---|---|---|---|
| 1 | 金额字段约定（精度 / 单位 元 vs 分） | 🔴 待补充 | `spring-boot-pay` README 标注"暂未开发"，全项目无 `BigDecimal` 金额实证 |
| 2 | 防重提交统一实现 | 🟡 部分缺失 | `RespCode.CODE_318(318,"访问重复请求")` 已定义，无 `@RepeatSubmit` 注解或 AOP |
| 3 | 注解式分布式锁 | 🟡 部分缺失 | 仅 `RedisUtils.getLock/releaseLock` 裸方法，无 Redisson，需手动 try-finally |
| 4 | 方法级权限注解 | 🟡 架构性缺失 | `@PreAuthorize`/`@Secured`/`@RolesAllowed` 全项目 0 处；权限仅在网关 URL-角色层。`fs:{resource}:*` 编码已规划未落地 |
| 5 | 测试覆盖率与 CI 门禁 | 🔴 待补充 | 根 POM 全局 `skipTests=true`，仅 fs 覆盖；无 JaCoCo / 阈值 / CI 门禁 |
| 6 | 代码风格强制工具 | 🔴 待补充 | 无 Checkstyle / Spotless / PMD / EditorConfig，规范靠约定与评审维持 |
| 7 | 数据库版本管理 | 🔴 待补充 | SQL 以 `sql/*.sql` 静态文件存放，无 Flyway / Liquibase |
| 8 | API 版本化策略 | 🔴 待补充 | 路径无 `/v1/` 版本段（fs 设计文档中 `/api/v1/users/*` 仅为鉴权示例） |
| 9 | 国际化 / 错误码多语言 | 🔴 待补充 | 提示文案硬编码中文，无 `MessageSource` / i18n 资源 |
| 10 | 行级数据权限 | 🔴 待补充 | 仅租户级隔离（`tenant_id`），无部门/个人级过滤实现 |
| 11 | `PlatformSaveParam.java` 中文注释编码 | ⚠️ 已知问题 | 部分工具下显示为 `???`，疑似编码问题，未修改 |

---

## 附录：fs 模块 41 张表页面归属

来源：`docs/superpowers/specs/2026-07-27-spring-boot-fs-management-api-design.md` §4。
新增 fs 管理端功能时，先在此定位表所属页面，**关联表不单独建菜单**。

| 页面分组 | 表 |
|---|---|
| 系统配置 | `fs_platform`、`fs_media_server`、`fs_gate_way`、`fs_route_gateway`、`fs_route_group`(+`fs_route_gateway_group`)、`fs_route_call` |
| 企业资源 | `fs_company`、`fs_company_conference`、`fs_company_display`、`fs_company_phone`(+`fs_company_phone_group`)、`fs_playback` |
| 呼叫中心 | `fs_group`(+`fs_group_agent_strategy`/`fs_group_strategy_exp`/`fs_group_memory_config`/`fs_group_overflow`/`fs_skill_group`)、`fs_skill`(+`fs_skill_agent`/`fs_skill_group`)、`fs_agent`(+`fs_agent_sip`/`fs_agent_group`/`fs_user_agent`/`fs_skill_agent`)、`fs_overflow_config`(+`fs_overflow_front`/`fs_overflow_exp`) |
| 呼入 IVR | `fs_ivr_workflow`、`fs_vdn_schedule`、`fs_vdn_code`(+`fs_vdn_phone`/`fs_vdn_config`/`fs_vdn_dtmf`) |
| 运行记录（**只读**） | `fs_call_log`(+`fs_call_detail`/`fs_call_device`/`fs_call_dtmf`)、`fs_agent_state_log`、`fs_group_memory`、`fs_push_log` |

**原则**：关联表只在所属页面的详情/抽屉/标签页中管理；日志表只读。

---

## 变更记录

| 日期 | 变更 |
|---|---|
| 2026-09-28 | 首次建立。基于 `5.0.0-dev` 分支全量代码与配置实证，含项目概况 + 9 类规范 + 注释文档规范 + 11 项缺失标注 + fs 表归属附录。原计划另建 `docs/superpowers/specs/` 报告体副本，已取消 —— 本文件为唯一权威副本，避免双份维护漂移。 |
| 2026-09-28 | 单文件 1280 行（66KB）一次性加载成本过高，按章拆为 9 个 `model_decision` 分册：`project-overview`、`naming-conventions`、`architecture-patterns`（二+七）、`coding-standards`（三+十）、`layered-templates`、`security-validation`、`exception-logging`（六+九）、`testing-database`、`gaps-and-fs-tables`（十一+附录+变更记录）。拆分后正文逐行校验与原文完全一致（980 行），**未改写任何结论**；仅修正 5 处跨章引用为跨分册引用。原 `project-conventions.md` 删除，分册索引落在 `project-baseline.md` 顶部。 |

