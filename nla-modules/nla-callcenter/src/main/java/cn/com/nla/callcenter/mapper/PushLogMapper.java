package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.PushLog;
import cn.com.nla.callcenter.domain.vo.PushLogVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 话单推送记录表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface PushLogMapper extends BaseMapperPlus<PushLog, PushLogVo> {
}
