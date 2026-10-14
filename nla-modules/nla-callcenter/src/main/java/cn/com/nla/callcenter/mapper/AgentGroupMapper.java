package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.AgentGroup;
import cn.com.nla.callcenter.domain.vo.AgentGroupVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 坐席技能组表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface AgentGroupMapper extends BaseMapperPlus<AgentGroup, AgentGroupVo> {
}
