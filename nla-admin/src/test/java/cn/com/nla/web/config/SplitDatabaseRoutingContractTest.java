package cn.com.nla.web.config;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.TypeFilter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分库路由配置契约测试：只读取 nla-admin 交付的 yml 与类路径上的 Mapper 字节码，不连接任何数据库。
 * <p>
 * 覆盖三类只在运行期才暴露的错误：业务 Mapper 上的 {@code @DS} 键名与
 * {@code spring.datasource.dynamic.datasource} 不一致（{@code strict: true} 下直接报错），
 * 新增分库 Mapper 忘记标注解（静默落到主库 master），以及交付建库脚本与路由库清单漂移
 * （部署时少建一个库，首条跨库 SQL 才报库不存在）。同时校验工作流表白名单在
 * dev / prod 两个 profile 下形状与内容一致，避免只改一个 profile 造成的环境漂移。
 *
 * @author TZY
 */
class SplitDatabaseRoutingContractTest {

    /**
     * 需要同时校验的 profile，与 nla-admin/src/main/resources 下交付的 application-{profile}.yml 对应
     */
    private static final List<String> PROFILES = List.of("dev", "prod");

    /**
     * 主库数据源键名
     */
    private static final String MASTER = "master";

    /**
     * 业务分库数据源键名 -> 该库 Mapper 所在的包，一个库对应一个模块
     */
    private static final Map<String, String> BUSINESS_DATASOURCES = Map.of(
        "nla_message", "cn.com.nla.message.mapper",
        "nla_face", "cn.com.nla.face.mapper",
        "nla_video", "cn.com.nla.video.mapper",
        "nla_callcenter", "cn.com.nla.callcenter.mapper");

    /**
     * 工作流分库需要限定的表名（与 script/sql/nla_workflow.sql 建表一致；本工程不建 flow_form）
     */
    private static final Set<String> WORKFLOW_TABLES = Set.of(
        "flow_definition", "flow_node", "flow_skip", "flow_instance", "flow_task", "flow_his_task",
        "flow_user", "flow_category", "flow_spel", "flow_instance_biz_ext", "test_leave");

    /**
     * 交付的建库脚本，由 nla-admin/pom.xml 的 testResources 从 script/sql 复制进类路径
     */
    private static final String SCHEMA_SCRIPT = "nla_databases.sql";

    /**
     * 不由 nla-admin 直连、但同属交付库清单的库（各自的 server 进程自带数据源）
     */
    private static final Set<String> STANDALONE_DATABASES = Set.of("nla_job", "nla_ai");

    /**
     * MySQL JDBC URL 里的库名，形如 jdbc:mysql://host:3306/nla_system?useUnicode=true
     */
    private static final Pattern JDBC_DATABASE = Pattern.compile("jdbc:mysql://[^/]+/([A-Za-z0-9_]+)");

    /**
     * 建库脚本里的 CREATE DATABASE 语句，反引号与 IF NOT EXISTS 均可选
     */
    private static final Pattern CREATE_DATABASE =
        Pattern.compile("(?i)^\\s*create\\s+database\\s+(?:if\\s+not\\s+exists\\s+)?`?([A-Za-z0-9_]+)`?");

    /**
     * 每个 profile 的业务分库数据源都必须齐备，且严格模式与主库标定不能被改动。
     *
     * @throws java.io.IOException 读取交付 yml 失败
     */
    @Test
    void everyProfileDeclaresAllBusinessDataSourcesInStrictMode() throws Exception {
        for (String profile : PROFILES) {
            List<String> block = blockLines(profile, "dynamic:");
            assertFalse(block.isEmpty(), () -> profile + " 缺少 spring.datasource.dynamic 配置块");
            assertTrue(block.stream().anyMatch(line -> line.matches("^\\s*primary:\\s*" + MASTER + "\\s*$")),
                profile + " 的 dynamic-datasource 未把 master 标为 primary");
            assertTrue(block.stream().anyMatch(line -> line.matches("^\\s*strict:\\s*true\\s*$")),
                profile + " 的 dynamic-datasource 未开启 strict，写错的 @DS 键名会静默回落到主库");

            Set<String> keys = new TreeSet<>(dataSourceKeys(block));
            Set<String> expected = new TreeSet<>(BUSINESS_DATASOURCES.keySet());
            expected.add(MASTER);
            assertEquals(expected, keys, () -> profile + " 的 datasource 键名清单不符合预期");
        }
    }

    /**
     * 四个业务包下的每个 Mapper 接口都必须带 {@code @DS}，且键名指向自己模块的分库；
     * 每个分库至少被一个 Mapper 使用，防止键名写了却没人路由。
     */
    @Test
    void everyBusinessMapperIsRoutedToItsOwnDataSource() {
        BUSINESS_DATASOURCES.forEach((dataSource, packageName) -> {
            List<Class<?>> mappers = mappersIn(packageName);
            assertFalse(mappers.isEmpty(), () -> "包 " + packageName + " 下没扫到 Mapper，检查模块是否接入 nla-admin");
            Set<String> used = new LinkedHashSet<>();
            for (Class<?> mapper : mappers) {
                DS ds = mapper.getAnnotation(DS.class);
                assertNotNull(ds, () -> mapper.getName() + " 缺少 @DS，会被路由到主库 " + MASTER);
                String value = ds.value().trim();
                assertEquals(dataSource, value, () -> mapper.getName() + " 的 @DS 键名应为 " + dataSource + "，实际 " + value);
                used.add(value);
            }
            assertEquals(Set.of(dataSource), used, () -> "分库 " + dataSource + " 的 Mapper 路由键名不统一");
        });
    }

    /**
     * 工作流表白名单在两个 profile 下必须是同形同内容，且不含本工程未建表的 flow_form。
     *
     * @throws java.io.IOException 读取交付 yml 失败
     */
    @Test
    void workflowTableWhitelistIsConsistentAcrossProfiles() throws Exception {
        Set<String> shared = null;
        for (String profile : PROFILES) {
            List<String> block = blockLines(profile, "workflow:");
            assertFalse(block.isEmpty(), () -> profile + " 缺少 nla.workflow 配置块");
            assertTrue(block.stream().anyMatch(line -> line.matches("^\\s*schema:\\s*nla_workflow\\s*$")),
                profile + " 的 nla.workflow.schema 未指向 nla_workflow");

            List<String> configured = sequenceItems(block, "tables:");
            assertFalse(configured.isEmpty(), () -> profile + " 的 nla.workflow.tables 为空或写成了 `- 名: 名` 映射项，"
                + "两者都会让工作流表退回主库解析");
            Set<String> tables = new TreeSet<>();
            for (String item : configured) {
                String table = item.toLowerCase(Locale.ROOT);
                assertFalse(table.contains(":"), () -> profile + " 的 nla.workflow.tables 存在映射项 [" + item
                    + "]，该配置项类型是 List<String>，必须写成 `- 表名`");
                tables.add(table);
            }
            assertEquals(WORKFLOW_TABLES, tables, () -> profile + " 的工作流表白名单与预期不一致");
            assertFalse(tables.contains("flow_form"), profile + " 的工作流表白名单包含未建表的 flow_form");
            shared = shared == null ? tables : shared;
            assertEquals(shared, tables, "dev 与 prod 的工作流表白名单不一致");
        }
    }

    /**
     * 取出交付 yml 中某个配置块的正文行（含更深缩进的子行，遇到同级或更浅的非空行即结束）。
     *
     * @param profile  profile 名，对应 application-{profile}.yml
     * @param lastKey  块的最后一个键名标记，例如 {@code dynamic:}
     * @return 块内正文行，保留原始缩进；未找到时为空列表
     * @throws java.io.IOException 读取资源失败
     */
    private List<String> blockLines(String profile, String lastKey) throws Exception {
        List<String> lines = readYaml(profile);
        int header = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals(lastKey) && !lines.get(i).trim().startsWith("#")) {
                header = i;
            }
        }
        if (header < 0) {
            return List.of();
        }
        int indent = indentOf(lines.get(header));
        List<String> block = new ArrayList<>();
        for (int i = header + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank() || line.trim().startsWith("#") || line.trim().startsWith("---")) {
                continue;
            }
            if (indentOf(line) <= indent) {
                break;
            }
            block.add(line);
        }
        return block;
    }

    /**
     * 取出 {@code datasource:} 节点下的一级子键，即各数据源键名。
     *
     * @param block {@code dynamic:} 块的正文行
     * @return 数据源键名，按出现顺序
     */
    private List<String> dataSourceKeys(List<String> block) {
        return childKeys(block, "datasource:");
    }

    /**
     * 取出某个映射节点下的一级子键。
     *
     * @param block   父配置块的正文行
     * @param mapKey  节点键名标记，例如 {@code datasource:}
     * @return 一级子键（去掉冒号）；节点不存在时为空列表
     */
    private List<String> childKeys(List<String> block, String mapKey) {
        int header = indexOfKey(block, mapKey);
        if (header < 0) {
            return List.of();
        }
        int childIndent = -1;
        List<String> keys = new ArrayList<>();
        for (int i = header + 1; i < block.size(); i++) {
            String line = block.get(i);
            if (line.isBlank() || line.trim().startsWith("#")) {
                continue;
            }
            int indent = indentOf(line);
            if (childIndent < 0) {
                childIndent = indent;
            }
            if (indent < childIndent) {
                break;
            }
            if (indent == childIndent && isNestedMapKey(line.trim())) {
                keys.add(line.trim().substring(0, line.trim().length() - 1));
            }
        }
        return keys;
    }

    /**
     * 判断一行是否是“只有键名、值在更深层”的嵌套映射键，例如 {@code master:}；
     * {@code url: jdbc:...} 这类带值的行不算。
     *
     * @param trimmed 去掉前后空白的行
     * @return 是嵌套映射键返回 {@code true}
     */
    private boolean isNestedMapKey(String trimmed) {
        return trimmed.endsWith(":") && trimmed.indexOf(':') == trimmed.length() - 1;
    }

    /**
     * 取出某个序列节点的原始项（冒号后的内容不参与，因此映射项会连同键值一起返回，便于断言报错）。
     *
     * @param block     父配置块的正文行
     * @param sequenceKey 序列节点键名标记，例如 {@code tables:}
     * @return 序列项原文；节点不存在时为空列表
     */
    private List<String> sequenceItems(List<String> block, String sequenceKey) {
        int header = indexOfKey(block, sequenceKey);
        if (header < 0) {
            return List.of();
        }
        int indent = indentOf(block.get(header));
        List<String> items = new ArrayList<>();
        for (int i = header + 1; i < block.size(); i++) {
            String line = block.get(i);
            if (line.isBlank() || line.trim().startsWith("#")) {
                continue;
            }
            if (indentOf(line) <= indent) {
                break;
            }
            if (line.trim().startsWith("-")) {
                items.add(line.trim().substring(1).trim());
            }
        }
        return items;
    }

    /**
     * 在块内定位某个键所在行。
     *
     * @param block 块正文行
     * @param key   键名标记，例如 {@code tables:}
     * @return 行下标，未找到返回 -1
     */
    private int indexOfKey(List<String> block, String key) {
        for (int i = 0; i < block.size(); i++) {
            String line = block.get(i);
            if (!line.trim().startsWith("#") && line.trim().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 计算一行的前导空格数（制表符按 1 列处理，交付 yml 统一使用空格）。
     *
     * @param line 原始行
     * @return 缩进列数
     */
    private int indentOf(String line) {
        int indent = 0;
        while (indent < line.length() && (line.charAt(indent) == ' ' || line.charAt(indent) == '\t')) {
            indent++;
        }
        return indent;
    }

    /**
     * 建库脚本必须建出路由要用的每一个库，且不得多出清单之外的库：
     * 数据源 URL 里的库名 + 工作流 schema + 独立进程使用的 nla_job / nla_ai。
     *
     * @throws java.io.IOException 读取交付 yml 或建库脚本失败
     */
    @Test
    void schemaScriptCreatesExactlyTheRoutedDatabases() throws Exception {
        Set<String> created = createdDatabases();
        for (String profile : PROFILES) {
            Set<String> routed = new TreeSet<>(urlDatabases(blockLines(profile, "dynamic:")));
            routed.add(workflowSchema(profile));
            routed.addAll(STANDALONE_DATABASES);
            assertTrue(created.containsAll(routed),
                () -> profile + " 需要路由的库 " + minus(routed, created) + " 不在 " + SCHEMA_SCRIPT + " 里");
            assertTrue(routed.containsAll(created),
                SCHEMA_SCRIPT + " 建出了路由之外的库 " + minus(created, routed));
        }
    }

    /**
     * 取出 {@code nla_databases.sql} 里建出的库名。
     *
     * @return 库名集合，按字典序
     * @throws java.io.IOException 读取失败
     */
    private Set<String> createdDatabases() throws Exception {
        Set<String> names = new TreeSet<>();
        for (String line : readClasspath(SCHEMA_SCRIPT)) {
            if (line.trim().startsWith("--")) {
                continue;
            }
            Matcher matcher = CREATE_DATABASE.matcher(line);
            if (matcher.find()) {
                names.add(matcher.group(1));
            }
        }
        assertFalse(names.isEmpty(), SCHEMA_SCRIPT + " 里没解析到任何 CREATE DATABASE 语句");
        return names;
    }

    /**
     * 取出数据源块内各 {@code url:} 行指向的库名。
     *
     * @param block {@code dynamic:} 块的正文行
     * @return 库名集合，按字典序
     */
    private Set<String> urlDatabases(List<String> block) {
        Set<String> databases = new TreeSet<>();
        for (String line : block) {
            if (!line.trim().startsWith("url:")) {
                continue;
            }
            Matcher matcher = JDBC_DATABASE.matcher(line);
            if (matcher.find()) {
                databases.add(matcher.group(1));
            }
        }
        assertFalse(databases.isEmpty(), "数据源块里没解析到任何 jdbc:mysql URL");
        return databases;
    }

    /**
     * 取出 {@code nla.workflow.schema} 配置的库名。
     *
     * @param profile profile 名
     * @return 工作流库名
     * @throws java.io.IOException 读取失败
     */
    private String workflowSchema(String profile) throws Exception {
        return blockLines(profile, "workflow:").stream()
            .map(String::trim)
            .filter(line -> line.startsWith("schema:"))
            .map(line -> line.substring("schema:".length()).trim().replace("\"", ""))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(profile + " 缺少 nla.workflow.schema"));
    }

    /**
     * 求两个集合的差。
     *
     * @param from 被减集合
     * @param minus 要减掉的集合
     * @return 差集，按字典序
     */
    private Set<String> minus(Set<String> from, Set<String> minus) {
        Set<String> rest = new TreeSet<>(from);
        rest.removeAll(minus);
        return rest;
    }

    /**
     * 按 UTF-8 读取交付的 profile 文件。
     *
     * @param profile  profile 名
     * @return 全部行
     * @throws java.io.IOException 读取失败
     */
    private List<String> readYaml(String profile) throws Exception {
        return readClasspath("application-" + profile + ".yml");
    }

    /**
     * 按 UTF-8 读取类路径上的交付文件。
     *
     * @param name 类路径资源名
     * @return 全部行
     * @throws java.io.IOException 读取失败
     */
    private List<String> readClasspath(String name) throws Exception {
        ClassPathResource resource = new ClassPathResource(name);
        assertTrue(resource.exists(), "类路径上缺少 " + name + "，检查 nla-admin/pom.xml 的 testResources 配置");
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        }
    }

    /**
     * 扫描指定包下所有 {@code *Mapper} 接口。
     *
     * @param packageName Mapper 所在包
     * @return 已加载的 Mapper 接口
     */
    private List<Class<?>> mappersIn(String packageName) {
        TypeFilter mapperFilter = (reader, factory) -> reader.getClassMetadata().isInterface()
            && reader.getClassMetadata().getClassName().endsWith("Mapper");
        // 默认实现只接受具体类，Mapper 都是接口，必须 override 候选判定才能扫到
        ClassPathScanningCandidateComponentProvider provider =
            new ClassPathScanningCandidateComponentProvider(false) {
                @Override
                protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                    return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
                }
            };
        provider.addIncludeFilter(mapperFilter);
        List<Class<?>> mappers = new ArrayList<>();
        provider.findCandidateComponents(packageName).forEach(candidate -> {
            String className = candidate.getBeanClassName();
            try {
                // initialize=false：只加载接口本身读取注解，不触发其静态初始化
                mappers.add(Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
            } catch (ClassNotFoundException | LinkageError e) {
                throw new IllegalStateException("无法加载 " + className + "，检查分库模块是否接入 nla-admin", e);
            }
        });
        return mappers;
    }
}
