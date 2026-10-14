package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.Platform;
import cn.com.nla.callcenter.domain.vo.PlatformVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 平台信息 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface PlatformMapper extends BaseMapperPlus<Platform, PlatformVo> {
}
