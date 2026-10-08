package cn.com.nla.workflow.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link WarmFlowConfig} 工作流分库的表名重写契约测试。
 * <p>
 * 分库的正确性完全取决于 SQL 里的表名有没有被准确限定：漏一张表会直接报
 * “表不存在”，误伤一个列名则会生成非法 SQL。这两类错误在编译期都看不出来，
 * 启动整个应用去撞的成本又太高，因此在这里对 {@code changeTable} 做纯函数级断言。
 *
 * @author TZY
 */
@DisplayName("WarmFlowConfig 工作流分库单元测试")
class WarmFlowConfigTest {

    /**
     * 工作流独立库名，与 application-dev.yml 的 {@code nla.workflow.schema} 保持一致
     */
    private static final String SCHEMA = "nla_workflow";

    /**
     * {@code nla_workflow} 库里实际存在的 11 张表，取自 information_schema。
     * {@code flow_form} 由 WarmFlow 实体声明但本工程未建表，单独断言。
     */
    private static final List<String> WORKFLOW_TABLES = List.of(
        "flow_category", "flow_definition", "flow_his_task", "flow_instance",
        "flow_instance_biz_ext", "flow_node", "flow_skip", "flow_spel",
        "flow_task", "flow_user", "test_leave");

    /**
     * 按 MybatisPlusConfig 的真实顺序组装拦截器链。数据权限拦截器需要 Spring 上下文，
     * 此处省略——本测试只关心动态表名与分页插件的相对位置。
     *
     * @return 含分页与乐观锁两个插件的拦截器链
     */
    private MybatisPlusInterceptor newChain() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    /**
     * 取出已注册到链上的动态表名拦截器。
     *
     * @return 拦截器实例，未注册时返回 {@code null}
     */
    private DynamicTableNameInnerInterceptor registered() {
        MybatisPlusInterceptor chain = newChain();
        new WarmFlowConfig(chain, props(SCHEMA)).afterPropertiesSet();
        DynamicTableNameInnerInterceptor dynamic = chain.getInterceptors().stream()
            .filter(DynamicTableNameInnerInterceptor.class::isInstance)
            .map(DynamicTableNameInnerInterceptor.class::cast)
            .findFirst()
            .orElse(null);
        assertNotNull(dynamic, "动态表名拦截器未注册");
        return dynamic;
    }

    /**
     * 返回指定类型拦截器在链上的下标，不存在时为 -1。
     *
     * @param interceptors 拦截器链
     * @param type         目标类型
     * @return 下标
     */
    private int indexOf(List<InnerInterceptor> interceptors, Class<?> type) {
        for (int i = 0; i < interceptors.size(); i++) {
            if (type.isInstance(interceptors.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 构造仅设置了库名的分库配置，表名白名单留空以走内置默认清单。
     *
     * @param schema 工作流独立库名
     * @return 分库配置
     */
    private WorkflowProperties props(String schema) {
        WorkflowProperties properties = new WorkflowProperties();
        properties.setSchema(schema);
        return properties;
    }

    /**
     * 构造同时指定库名与表名白名单的分库配置，用于验证 yml 覆盖默认清单。
     *
     * @param schema 工作流独立库名
     * @param tables 表名白名单
     * @return 分库配置
     */
    private WorkflowProperties props(String schema, List<String> tables) {
        WorkflowProperties properties = new WorkflowProperties();
        properties.setSchema(schema);
        properties.setTables(tables);
        return properties;
    }

    /**
     * 验证留空库名时回到上游的单库部署，不注册任何拦截器。
     */
    @Test
    @DisplayName("库名留空时不注册任何拦截器")
    void shouldSkipWhenSchemaBlank() {
        MybatisPlusInterceptor chain = newChain();

        new WarmFlowConfig(chain, props("  ")).afterPropertiesSet();

        assertEquals(2, chain.getInterceptors().size());
        assertEquals(-1, indexOf(chain.getInterceptors(), DynamicTableNameInnerInterceptor.class));
    }

    /**
     * 验证动态表名插件插在分页插件之前，这是 MyBatis-Plus 官方要求的顺序。
     */
    @Test
    @DisplayName("动态表名拦截器插在分页插件之前")
    void shouldInsertBeforePagination() {
        MybatisPlusInterceptor chain = newChain();

        new WarmFlowConfig(chain, props(SCHEMA)).afterPropertiesSet();

        List<InnerInterceptor> interceptors = chain.getInterceptors();
        assertEquals(3, interceptors.size());
        int dynamic = indexOf(interceptors, DynamicTableNameInnerInterceptor.class);
        int pagination = indexOf(interceptors, PaginationInnerInterceptor.class);
        assertTrue(dynamic >= 0 && dynamic < pagination,
            "动态表名必须排在分页之前，否则分页生成的 SQL 不会被改写");
    }

    /**
     * 验证 11 张工作流表全部被限定到独立库，漏一张就会在运行期报表不存在。
     */
    @Test
    @DisplayName("11 张工作流表全部被限定到独立库")
    void shouldQualifyEveryWorkflowTable() {
        DynamicTableNameInnerInterceptor dynamic = registered();

        for (String table : WORKFLOW_TABLES) {
            // changeTable 返回整条改写后的 SQL（不是单个表名），故与整条期望 SQL 比较
            assertEquals("SELECT id FROM " + SCHEMA + "." + table,
                dynamic.changeTable("SELECT id FROM " + table),
                "表 " + table + " 未被限定到 " + SCHEMA);
        }
        // flow_form 本工程未建表，但同样要改写，避免将来建表后漏配
        assertEquals("SELECT id FROM " + SCHEMA + ".flow_form",
            dynamic.changeTable("SELECT id FROM flow_form"));
    }

    /**
     * 验证主库的表不被改写，否则系统管理的查询会全部打到工作流库上。
     */
    @Test
    @DisplayName("主库的表保持原样")
    void shouldLeaveMasterTablesAlone() {
        DynamicTableNameInnerInterceptor dynamic = registered();

        assertEquals("SELECT id FROM sys_user", dynamic.changeTable("SELECT id FROM sys_user"));
        assertEquals("SELECT id FROM sys_dept", dynamic.changeTable("SELECT id FROM sys_dept"));
        assertEquals("SELECT id FROM gen_table", dynamic.changeTable("SELECT id FROM gen_table"));
    }

    /**
     * 验证增删改语句与查询语句一样被改写，工作流办理会大量写这几张表。
     */
    @Test
    @DisplayName("增删改语句同样被改写")
    void shouldQualifyWriteStatements() {
        DynamicTableNameInnerInterceptor dynamic = registered();

        assertEquals("INSERT INTO " + SCHEMA + ".flow_task (id) VALUES (?)",
            dynamic.changeTable("INSERT INTO flow_task (id) VALUES (?)"));
        assertEquals("UPDATE " + SCHEMA + ".flow_instance SET update_time = ?",
            dynamic.changeTable("UPDATE flow_instance SET update_time = ?"));
        assertEquals("DELETE FROM " + SCHEMA + ".flow_his_task WHERE id = ?",
            dynamic.changeTable("DELETE FROM flow_his_task WHERE id = ?"));
    }

    /**
     * 验证关联与子查询里的工作流表各自被限定，而主库的表留在原库。
     * 这正是单连接跨库方案成立的前提：同一条 SQL 可以同时命中两个库。
     */
    @Test
    @DisplayName("跨库关联与子查询各走各的库")
    void shouldQualifyJoinAndSubquery() {
        DynamicTableNameInnerInterceptor dynamic = registered();

        assertEquals("SELECT t.id FROM " + SCHEMA + ".flow_task t LEFT JOIN "
                + SCHEMA + ".flow_his_task h ON t.id = h.task_id",
            dynamic.changeTable("SELECT t.id FROM flow_task t LEFT JOIN flow_his_task h ON t.id = h.task_id"));

        assertEquals("SELECT u.user_name FROM sys_user u WHERE u.dept_id IN "
                + "(SELECT dept_id FROM " + SCHEMA + ".flow_category)",
            dynamic.changeTable("SELECT u.user_name FROM sys_user u WHERE u.dept_id IN "
                + "(SELECT dept_id FROM flow_category)"));
    }

    /**
     * 验证与表名同形的列名不会被误伤。误伤会生成非法 SQL，且只在运行期暴露。
     */
    @Test
    @DisplayName("不误伤与表名同形的列名")
    void shouldNotTouchLookalikeColumns() {
        DynamicTableNameInnerInterceptor dynamic = registered();

        // flow_task_id 是列名而非表名，delimiter 是 sai_rag 的列名
        assertEquals("SELECT t.flow_task_id, t.delimiter FROM " + SCHEMA
                + ".flow_task t WHERE t.flow_task_id = ?",
            dynamic.changeTable("SELECT t.flow_task_id, t.delimiter FROM flow_task t "
                + "WHERE t.flow_task_id = ?"));
    }

    /**
     * 验证拦截器可以被安全地重复初始化，避免上下文刷新时叠加注册。
     */
    @Test
    @DisplayName("重复初始化不会把库名叠加成两段")
    void shouldStayIdempotentOnSameInterceptor() {
        DynamicTableNameInnerInterceptor dynamic = registered();

        String once = dynamic.changeTable("SELECT id FROM flow_task");
        String twice = dynamic.changeTable(once);

        assertEquals("SELECT id FROM " + SCHEMA + ".flow_task", once);
        assertEquals(once, twice,
            "已限定的表名被二次改写，说明处理器不是幂等的");
    }

    /**
     * 验证 yml 显式配置 {@code nla.workflow.tables} 时覆盖内置默认清单：
     * 只有配置里的表被限定，默认清单里未列入的表保持原样。
     */
    @Test
    @DisplayName("yml 配置表清单时覆盖内置默认")
    void shouldOverrideDefaultTablesWhenConfigured() {
        MybatisPlusInterceptor chain = newChain();
        new WarmFlowConfig(chain, props(SCHEMA, List.of("flow_task", "flow_custom_ext")))
            .afterPropertiesSet();
        DynamicTableNameInnerInterceptor dynamic = chain.getInterceptors().stream()
            .filter(DynamicTableNameInnerInterceptor.class::isInstance)
            .map(DynamicTableNameInnerInterceptor.class::cast)
            .findFirst()
            .orElse(null);
        assertNotNull(dynamic, "动态表名拦截器未注册");

        // 配置内的表被限定，包括内置默认没有的自定义表
        assertEquals("SELECT id FROM " + SCHEMA + ".flow_task",
            dynamic.changeTable("SELECT id FROM flow_task"));
        assertEquals("SELECT id FROM " + SCHEMA + ".flow_custom_ext",
            dynamic.changeTable("SELECT id FROM flow_custom_ext"));
        // flow_definition 属于内置默认，但被配置覆盖后不在白名单，应保持原样
        assertEquals("SELECT id FROM flow_definition",
            dynamic.changeTable("SELECT id FROM flow_definition"));
    }

}
