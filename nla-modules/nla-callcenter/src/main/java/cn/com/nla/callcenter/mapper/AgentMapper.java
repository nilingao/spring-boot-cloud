package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.freeswitch.model.bean.UserModel;
import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.Agent;
import cn.com.nla.callcenter.domain.vo.AgentVo;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 座席工号表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface AgentMapper extends BaseMapperPlus<Agent, AgentVo> {
    /** 指定企业的用户绑定座席；仅有效企业/座席/绑定，按创建时间和ID稳定倒序。 */
    List<AgentVo> selectByUserId(@Param("companyId") Long companyId, @Param("userId") Long userId);

    /** 按SIP号码查询指定企业的有效座席，不返回密码。 */
    AgentVo selectBySip(@Param("companyId") Long companyId, @Param("sip") String sip);

    /** 按SIP号码全局查询有效座席(协议侧注册/鉴权查找，不分企业)。 */
    Agent getAgentBySip(@Param("sip") String sip);

    /** 用户绑定的最新座席(协议侧全局查找，不分企业，含密码)。 */
    Agent findUserId(@Param("userId") Long userId);

    /** 按SIP号码查询 FreeSWITCH XML directory 鉴权模型(协议侧鉴权，不分企业)。 */
    UserModel findUserModel(@Param("sip") String sip);
}
