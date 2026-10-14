package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.RouteGroup;
import cn.com.nla.callcenter.domain.vo.RouteGroupVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 网关组 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface RouteGroupMapper extends BaseMapperPlus<RouteGroup, RouteGroupVo> {
}
