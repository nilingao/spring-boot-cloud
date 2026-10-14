package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.RouteCall;
import cn.com.nla.callcenter.domain.vo.RouteCallVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 字冠路由表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface RouteCallMapper extends BaseMapperPlus<RouteCall, RouteCallVo> {
}
