package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.SkillAgent;
import cn.com.nla.callcenter.domain.vo.SkillAgentVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 坐席技能表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface SkillAgentMapper extends BaseMapperPlus<SkillAgent, SkillAgentVo> {
}
