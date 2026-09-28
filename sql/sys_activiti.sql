/*Activiti 工作流引擎表性能索引（库：sys_activiti）

  背景：act_* 表由 Activiti 引擎自动建表，官方 DDL 只建了主键与外键索引，
  项目代码中高频使用的过滤列（TASK_ID_ / TENANT_ID_ / ASSIGNEE_ / START_USER_ID_）
  均无索引，EXPLAIN 实测为 type=ALL 全表扫描。

  注意：
  1.引擎升级或重建表结构后，本脚本需要重新执行；
  2.MySQL 的 CREATE INDEX 不支持 IF NOT EXISTS，重复执行会报
    "Duplicate key name" 错误，属正常现象，可忽略或按需删除后重建；
  3.仅新增索引，不修改、不删除引擎自建的任何索引与列。*/

/*审批意见表：原表仅有主键索引。
  工作流列表接口每行都会按 TASK_ID_ 查节点意见、按 PROC_INST_ID_ 查实例意见，
  且查询列包含 LONGBLOB 类型的 FULL_MSG_，全表扫描代价随数据增长线性恶化。*/
CREATE INDEX idx_act_hi_comment_task ON act_hi_comment(TASK_ID_);
CREATE INDEX idx_act_hi_comment_proc ON act_hi_comment(PROC_INST_ID_);

/*历史任务表：已办事项列表按 ASSIGNEE_ + TENANT_ID_ 过滤已结束任务并计数*/
CREATE INDEX idx_act_hi_taskinst_assignee ON act_hi_taskinst(ASSIGNEE_, TENANT_ID_, END_TIME_);

/*历史流程实例表：
  (TENANT_ID_, START_TIME_) 供历史记录列表按租户过滤并按发起时间倒序分页，避免 filesort；
  (START_USER_ID_, TENANT_ID_) 供"我发起的事项"列表与个人工作流统计计数使用*/
CREATE INDEX idx_act_hi_procinst_tenant_start ON act_hi_procinst(TENANT_ID_, START_TIME_);
CREATE INDEX idx_act_hi_procinst_startuser ON act_hi_procinst(START_USER_ID_, TENANT_ID_);

/*运行时任务表：总待办事项列表与个人工作流统计按租户过滤计数*/
CREATE INDEX idx_act_ru_task_tenant ON act_ru_task(TENANT_ID_);

/*流程定义表：流程定义列表按租户过滤并取每个 KEY_ 的最新版本*/
CREATE INDEX idx_act_re_procdef_tenant ON act_re_procdef(TENANT_ID_, KEY_, VERSION_);
