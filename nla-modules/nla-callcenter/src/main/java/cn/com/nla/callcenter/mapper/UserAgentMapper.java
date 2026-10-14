package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.UserAgent;
import cn.com.nla.callcenter.domain.vo.UserAgentVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 用户座席中间表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface UserAgentMapper extends BaseMapperPlus<UserAgent, UserAgentVo> {
}
