package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.VdnConfig;
import cn.com.nla.callcenter.domain.vo.VdnConfigVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 呼入路由字码表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface VdnConfigMapper extends BaseMapperPlus<VdnConfig, VdnConfigVo> {
}
