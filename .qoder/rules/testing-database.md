---
trigger: model_decision
description: 测试与建表：JUnit 4 测试框架与三类写法（@RunWith(SpringRunner.class) 加 @SpringBootTest 集成测试、@RunWith(MockitoJUnitRunner.class) 单元测试、MockMvcBuilders.standaloneSetup 的 Controller 契约测试）及示例；根 POM 全局 skipTests=true 不可改、需在模块 POM 覆盖；mvn 执行测试的完整命令；测试类与方法命名；数据库表命名与建表约定（表名模块前缀、必备审计列、tenant_id、索引前缀 idx_ / un_ / uni_idx_ / fk_、无逻辑删除、普遍不建物理外键、sql 目录归属）。加载场景：写单元测试或集成测试、执行测试命令、编写建表 SQL、修改表结构或索引。
---

# 测试规范与数据库建表约定

> 规范分册之一（原 `project-conventions.md` 按章拆分而来，结论均基于 `5.0.0-dev` 分支代码实证）。常驻铁律与全部分册索引见 `.qoder/rules/project-baseline.md`；查不到的机制见 `gaps-and-fs-tables.md`，禁止编造。

## 八、测试规范与数据库表命名

### 8.1 测试框架与写法

JUnit **4**（`org.junit.Test`，非 JUnit5）+ Spring Test + Mockito + MockMvc。

```java
// 集成测试（旧模块）
@Log4j2
@RunWith(SpringRunner.class)
@SpringBootTest
public class MinioTest {
    @Resource private MinioUtils minioUtils;
    @Test public void imageToBean64() { ... }
}

// 单元测试（新规范，无 Spring 上下文）
@RunWith(MockitoJUnitRunner.class)
public class PlatformManageServiceTest {
    @Mock private PlatformService platformService;
    @InjectMocks private PlatformManageServiceImpl service;

    @Test public void saveRejectsDuplicateName() {
        RestResult<?> result = service.save(PlatformSaveParam.builder().name("fs-main").build());
        assertEquals(RespCode.CODE_2.getValue(), result.getCode());
        assertEquals("平台名称已存在", result.getMessage());
        verify(platformService, never()).save(any(Platform.class));
    }
}

// Controller 契约测试（不启动 Spring 上下文）
@RunWith(MockitoJUnitRunner.class)
public class PlatformManageControllerTest {
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private PlatformManageService platformManageService;
    @InjectMocks private PlatformManageController controller;

    @Before public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test public void detailUsesGet() throws Exception {
        mockMvc.perform(get("/api/fs/platform/detail").param("id", "1"))
               .andExpect(status().isOk());
    }

    @Test public void removeUsesDelete() throws Exception {
        mockMvc.perform(delete("/api/fs/platform/remove").param("id", "1"))
               .andExpect(status().isOk());
    }
}

// 参数校验测试（手动 Validator）
Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
Set<ConstraintViolation<PlatformSaveParam>> violations = validator.validate(param);

// @TransactionalEventListener AFTER_COMMIT 测试（需最小事务上下文）
@ContextConfiguration(classes = FsRuntimeConfigRefreshListenerTest.Config.class)
static class Config {
    @Bean TestPlatformTransactionManager transactionManager();
    @Bean TestTransactionService transactionService();
    @Bean FsRuntimeConfigRefreshListener listener(...);
}
// refreshRunsAfterCommit() / refreshDoesNotRunAfterRollback()
```

### 8.2 测试执行与覆盖

- 根 POM `maven-surefire-plugin` 全局 `<skipTests>true</skipTests>`
- **禁止改根 POM**；需跑测试的模块在自己 POM 覆盖（当前仅 `spring-boot-business/spring-boot-fs`）：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration><skipTests>false</skipTests></configuration>
</plugin>
```

- 运行命令（PowerShell）：

```
mvn -pl spring-boot-business/spring-boot-fs -am -Dtest=PlatformManageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 位置：`src/test/java/cn/com/tzy/springboot{module}/...`
- 命名：类 `{被测类}Test`；方法描述行为，不用 `test1`：
  `saveRejectsReversedRtpRange`、`saveRejectsDuplicateName`、`updatePreservesRuntimeStatus`、
  `removeRejectsEnabledPlatform`、`refreshRunsAfterCommit`、`refreshDoesNotRunAfterRollback`、
  `publisherDeduplicatesCompanyIds`
- 三层测试方案（fs 设计文档 §9）：Service 单元（Mockito）→ 参数校验（Validator）→
  Controller 契约（MockMvc standalone）；事务型行为加最小事务上下文验证 `AFTER_COMMIT`
- 提交前：`git status --short` + `git diff --check`；**不主动 `git commit`**

### 8.3 数据库表命名与建表约定

**表名**：`{module}_` 全小写下划线 —— `bean_*`、`fs_*`、`sys_*`、`oa_*`、`face_*`、`video_*`、`sms_*`；
第三方保留官方命名 `qrtz_*`（大写）、`act_*`。

**关联表**：`{主}_connect_{从}`（`bean_user_connect_role`）或 `{主}_{从}`（`fs_agent_group`、`fs_skill_agent`）。

**列与索引**：

| 项 | 约定 |
|---|---|
| 主键 | `id` BIGINT AUTO_INCREMENT（`LongIdEntity`）；特例 `fs_media_server` 字符串 ID |
| 审计字段 | 继承 `Base` 的表必带 `create_user_id`、`create_time`、`update_user_id`、`update_time` |
| 多租户 | `tenant_id` BIGINT，默认公共租户 `1L`（`Constant.TENANT_ID`） |
| 逻辑删除 | ❌ **无**（全项目 0 处 `@TableLogic`/`deleted`/`del_flag`/`is_deleted`）→ 物理删除，删前必须引用校验 |
| 状态 | `is_enable`/`enable`/`status` TINYINT/INT 取 0/1；`status` 为运行时状态，save 须置 null |
| 备注 | 统一 `memo` |
| 索引前缀 | 唯一 `uni_idx_*`/`un_*`，普通 `idx_*`，外键 `fk_*`（`ApiController` 正则强依赖） |
| 物理外键 | 普遍不建，引用完整性由业务层 + `@Transactional` 保证 |
| 关键字列名 | 反引号：`` `name` ``、`` `enable` ``、`` `status` `` |
| 唯一索引示例 | `uni_idx_phone(vdn_id, company_id)` —— 业务层预校验须与之对齐，唯一索引是最终防线 |

**SQL 文件位置**：根目录

- `sql/{module}.sql`：`freeswitch.sql`、`quartz.sql`、`sys_area.sql`、`sys_area_1.sql`、`sys_face.sql`、`sys_video.sql`
- `sql/sys_{module}-基类sql.sql`：`sys_bean-基类sql.sql`、`sys_oa-基类sql.sql`
- `sql/sys_{module}_初始化.sql`：`sys_bean_初始化.sql`

**数据库划分**（每模块独立库）：`sys_bean_dev`、`sys_oa`、`sys_video`、`sys_face`、`freeswitch`、
`nacos`、`seata`、`xxl_job`。建表语法 `CREATE TABLE [IF NOT EXISTS] {表名} ( ... )`。

**分页与查询约束**：分页大小设上限；日志类查询强制时间范围；`select` 下拉默认 `limit=20`；
企业级关联资源必须校验同属一个 `companyId`。

