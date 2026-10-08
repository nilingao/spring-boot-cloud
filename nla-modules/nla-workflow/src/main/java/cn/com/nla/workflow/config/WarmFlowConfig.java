package cn.com.nla.workflow.config;

import cn.com.nla.workflow.common.ConditionalOnEnable;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * <b>工作流独立库（分库）支持。</b>WarmFlow 引擎的表由三方库
 * {@code org.dromara.warm.flow.orm.entity} 通过 {@code @TableName} 固定声明，
 * 既改不了实体也加不了 {@code @DS}；本模块自有的四张表虽然可以注解，
 * 但同一个业务方法内会同时访问工作流表与 {@code sys_*} 表（经 {@code nla-api} 的
 * {@code UserService} / {@code DeptService}），双数据源靠 ThreadLocal 路由会串库。
 * 因此改用 MyBatis-Plus 的动态表名插件，把工作流表名限定成 {@code <schema>.<table>}，
 * 全程复用主库那条连接 —— 同实例跨库是 MySQL 原生支持的，事务依然是原子的。
 * <p>
 * 可行性依据：WarmFlow 与本模块的 Mapper XML 全是空壳（只有 namespace），
 * 所有 SQL 都由 MyBatis-Plus 从 {@code @TableName} 生成；而
 * {@code DynamicTableNameInnerInterceptor} 内部用 {@code TableNameParser} 先定位出
 * 真正的表名 token 再逐个回调，不会误伤列名与别名。
 * <p>
 * 配置项 {@code nla.workflow.schema} 留空即回到上游的单库部署，本类什么都不做。
 *
 * @author TZY
 */
@ConditionalOnEnable
@Configuration
@EnableConfigurationProperties(WorkflowProperties.class)
@Slf4j
public class WarmFlowConfig implements InitializingBean {

    /**
     * WarmFlow 引擎表，表名取自 warm-flow-mybatis-plus-core 各实体的 {@code @TableName}。
     * <p>
     * {@code flow_form} 本工程未建表（不使用表单功能），登记在此不会有副作用。
     */
    private static final Set<String> ENGINE_TABLES = Set.of(
        "flow_definition", "flow_node", "flow_skip", "flow_instance",
        "flow_task", "flow_his_task", "flow_user", "flow_form");

    /**
     * 本模块自有表，与工作流引擎表同库。
     */
    private static final Set<String> MODULE_TABLES = Set.of(
        "flow_category", "flow_spel", "flow_instance_biz_ext", "test_leave");

    /**
     * yml 未显式配置 {@code nla.workflow.tables} 时使用的默认白名单（引擎表 + 自有表）。
     */
    private static final Set<String> DEFAULT_TABLES;

    static {
        Set<String> all = new HashSet<>(ENGINE_TABLES);
        all.addAll(MODULE_TABLES);
        DEFAULT_TABLES = Collections.unmodifiableSet(all);
    }

    private final MybatisPlusInterceptor mybatisPlusInterceptor;

    /**
     * 工作流分库配置（独立库名 + 可选的表名白名单）
     */
    private final WorkflowProperties properties;

    /**
     * 构造函数。
     *
     * @param mybatisPlusInterceptor MyBatis-Plus 核心拦截器链
     * @param properties             工作流分库配置，由 {@code nla.workflow.*} 绑定
     */
    public WarmFlowConfig(MybatisPlusInterceptor mybatisPlusInterceptor, WorkflowProperties properties) {
        this.mybatisPlusInterceptor = mybatisPlusInterceptor;
        this.properties = properties;
    }

    /**
     * 将动态表名拦截器插入现有拦截器链。
     * <p>
     * 位置按 MyBatis-Plus 官方建议：多租户 / 数据权限 → 动态表名 → 分页 → 乐观锁，
     * 即插在分页插件之前；链上没有分页插件时退而为追加到末尾。
     */
    @Override
    public void afterPropertiesSet() {
        String schema = properties.getSchema();
        if (!StringUtils.hasText(schema)) {
            log.debug("工作流未配置独立库(nla.workflow.schema 为空)，沿用主库");
            return;
        }
        Set<String> tables = resolveTables();

        // 无参构造已标 @Deprecated，表名处理器必须走构造函数传入
        DynamicTableNameInnerInterceptor dynamicTableName = new DynamicTableNameInnerInterceptor(
            (sql, tableName) ->
                tables.contains(tableName.toLowerCase(Locale.ROOT)) ? schema + "." + tableName : tableName);

        List<InnerInterceptor> chain = new ArrayList<>(mybatisPlusInterceptor.getInterceptors());
        int index = chain.size();
        for (int i = 0; i < chain.size(); i++) {
            if (chain.get(i) instanceof PaginationInnerInterceptor) {
                index = i;
                break;
            }
        }
        chain.add(index, dynamicTableName);
        mybatisPlusInterceptor.setInterceptors(chain);

        log.info("工作流分库已启用：{} 张表限定到库 [{}]，插入拦截器链位置 {}", tables.size(), schema, index);
    }

    /**
     * 解析生效的表白名单：yml 显式配置了 {@code nla.workflow.tables} 就用配置值（覆盖默认），
     * 否则回退到内置默认清单。配置值统一 {@code trim} 并转小写，以匹配
     * {@code TableNameParser} 解析出的表名 token。
     *
     * @return 生效的表名白名单（均为小写）
     */
    private Set<String> resolveTables() {
        List<String> configured = properties.getTables();
        if (configured == null || configured.isEmpty()) {
            return DEFAULT_TABLES;
        }
        Set<String> tables = new HashSet<>();
        for (String table : configured) {
            if (StringUtils.hasText(table)) {
                tables.add(table.trim().toLowerCase(Locale.ROOT));
            }
        }
        return tables.isEmpty() ? DEFAULT_TABLES : tables;
    }
}
