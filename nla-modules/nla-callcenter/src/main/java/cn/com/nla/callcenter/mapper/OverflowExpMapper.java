package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.OverflowExp;
import cn.com.nla.callcenter.domain.vo.OverflowExpVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 自定义溢出策略优先级 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface OverflowExpMapper extends BaseMapperPlus<OverflowExp, OverflowExpVo> {
}
