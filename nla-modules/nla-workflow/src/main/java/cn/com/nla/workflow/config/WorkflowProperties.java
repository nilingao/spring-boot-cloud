package cn.com.nla.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流分库配置，对应 yml 的 {@code nla.workflow.*}。
 * <p>
 * 表名白名单之所以做成可配置，是为了在 WarmFlow 升级新增引擎表、或本模块新增自有
 * {@code flow_*} 扩展表时，无需改动代码即可调整限定范围；本类不提供内置默认清单，
 * 限定范围完全由配置决定，避免代码里藏着一份和实际建表脚本会漂移的表名副本。
 *
 * @author TZY
 */
@Data
@ConfigurationProperties(prefix = "nla.workflow")
public class WorkflowProperties {

    /**
     * 工作流表所在的独立库名，与主库同一个 MySQL 实例；留空表示与主库同库，
     * 此时不做任何表名改写。
     */
    private String schema;

    /**
     * 需要限定到 {@link #schema} 的表名白名单（精确匹配，大小写不敏感）。
     * 留空表示不改写任何表名，因此配了 {@link #schema} 就必须显式列出这些表，
     * 否则工作流表会在主库里找不到。
     */
    private List<String> tables = new ArrayList<>();
}
