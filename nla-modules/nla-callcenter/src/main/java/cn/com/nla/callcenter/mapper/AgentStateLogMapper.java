package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.AgentStateLog;
import cn.com.nla.callcenter.domain.vo.AgentStateLogVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 坐席状态历史表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface AgentStateLogMapper extends BaseMapperPlus<AgentStateLog, AgentStateLogVo> {
}
