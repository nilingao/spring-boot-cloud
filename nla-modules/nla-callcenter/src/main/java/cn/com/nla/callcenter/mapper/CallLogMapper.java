package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.CallLog;
import cn.com.nla.callcenter.domain.vo.CallLogVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 话单表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface CallLogMapper extends BaseMapperPlus<CallLog, CallLogVo> {
}
