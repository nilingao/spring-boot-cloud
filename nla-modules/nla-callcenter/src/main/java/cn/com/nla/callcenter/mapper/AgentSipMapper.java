package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.AgentSip;
import cn.com.nla.callcenter.domain.vo.AgentSipVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** sip表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface AgentSipMapper extends BaseMapperPlus<AgentSip, AgentSipVo> {
}
