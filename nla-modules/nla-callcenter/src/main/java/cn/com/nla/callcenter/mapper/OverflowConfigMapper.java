package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.OverflowConfig;
import cn.com.nla.callcenter.domain.vo.OverflowConfigVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 溢出策略表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface OverflowConfigMapper extends BaseMapperPlus<OverflowConfig, OverflowConfigVo> {
}
