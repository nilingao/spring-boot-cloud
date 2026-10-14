package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.GroupAgentStrategy;
import cn.com.nla.callcenter.domain.vo.GroupAgentStrategyVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 技能组中坐席分配策略 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface GroupAgentStrategyMapper extends BaseMapperPlus<GroupAgentStrategy, GroupAgentStrategyVo> {
}
