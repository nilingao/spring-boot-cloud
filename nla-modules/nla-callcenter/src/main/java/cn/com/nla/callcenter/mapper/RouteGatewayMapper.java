package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.RouteGateway;
import cn.com.nla.callcenter.domain.vo.RouteGatewayVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 媒体网关表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface RouteGatewayMapper extends BaseMapperPlus<RouteGateway, RouteGatewayVo> {
}
