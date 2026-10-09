package cn.com.nla.common.freeswitch.client.fs.assign;

import cn.com.nla.common.freeswitch.common.fs.Constant;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.client.fs.AgentStrategy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Created by caoliang on 2021/8/3
 */
public class AgentCustomAssign implements AgentStrategy {

    private Logger logger = LoggerFactory.getLogger(AgentCustomAssign.class);

    /**
     * 自定义分配策略
     */
    private String customExpression;

    public AgentCustomAssign(String customExpression) {
        this.customExpression = customExpression;
    }

    @Override
    public Long calculateLevel(AgentVoInfo agentInfo) {
        EvaluationContext context = new StandardEvaluationContext();
        //技能相关
        agentInfo.getSkillAgents().forEach(skill -> {
            context.setVariable("skill_" + skill.getSkillId(), skill.getRankValue());
        });
        //时间变量
        context.setVariable(Constant.LONG_READY, -agentInfo.getStateTime());
        context.setVariable(Constant.READY_TIMES, agentInfo.getReadyTimes());
        context.setVariable(Constant.SEREVICE_TIMES, agentInfo.getServiceTime());
        return calculate(customExpression, context);
    }

    /**
     * @param expression
     * @param context
     * @return
     */
    private Long calculate(String expression, EvaluationContext context) {
        if (StringUtils.isBlank(expression)) {
            return 0L;
        }
        try {
            ExpressionParser parser = new SpelExpressionParser();
            Expression exp = parser.parseExpression(expression);
            return exp.getValue(context, Long.class);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return 0L;
    }

}
