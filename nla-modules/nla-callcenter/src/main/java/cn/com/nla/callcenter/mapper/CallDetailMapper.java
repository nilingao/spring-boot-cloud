package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.CallDetail;
import cn.com.nla.callcenter.domain.vo.CallDetailVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 通话流程表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface CallDetailMapper extends BaseMapperPlus<CallDetail, CallDetailVo> {
}
