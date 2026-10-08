package cn.com.nla.workflow.liteflow.start;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import cn.com.nla.workflow.common.ConditionalOnEnable;
import cn.com.nla.workflow.domain.context.StartProcessContext;

/**
 * 判断启动流程时是否走已有实例续提交分支。
 *
 * @author TZY
 */
@ConditionalOnEnable
@LiteflowComponent("startExists")
public class StartExistsComponent extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() {
        StartProcessContext context = getContextBean(StartProcessContext.class);
        return ObjectUtil.isNotNull(context.getExistingInstance());
    }

}
