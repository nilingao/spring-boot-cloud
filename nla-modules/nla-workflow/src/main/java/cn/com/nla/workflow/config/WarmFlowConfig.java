package cn.com.nla.workflow.config;

import cn.com.nla.workflow.common.ConditionalOnEnable;
import org.springframework.context.annotation.Configuration;

/**
 * WarmFlow 工作流配置入口，在工作流开关开启时注册相关组件。
 *
 * @author TZY
 */
@ConditionalOnEnable
@Configuration
public class WarmFlowConfig {

}

