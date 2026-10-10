package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.Agent;
import cn.com.nla.callcenter.domain.vo.AgentVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 座席工号表 Mapper。
 * @author TZY
 */
public interface AgentMapper extends BaseMapperPlus<Agent, AgentVo> {
    /** 指定企业的用户绑定座席；仅有效企业/座席/绑定，按创建时间和ID稳定倒序。 */
    List<AgentVo> selectByUserId(@Param("companyId") Long companyId, @Param("userId") Long userId);

    /** 按SIP号码查询指定企业的有效座席，不返回密码。 */
    AgentVo selectBySip(@Param("companyId") Long companyId, @Param("sip") String sip);
}
