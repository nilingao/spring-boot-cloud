package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.RouteGatewayGroup;
import cn.com.nla.callcenter.domain.vo.RouteGatewayGroupVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 路由与网关组关系表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface RouteGatewayGroupMapper extends BaseMapperPlus<RouteGatewayGroup, RouteGatewayGroupVo> {
}
