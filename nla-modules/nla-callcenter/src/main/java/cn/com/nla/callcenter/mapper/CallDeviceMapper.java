package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.CallDevice;
import cn.com.nla.callcenter.domain.vo.CallDeviceVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 话单明细表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface CallDeviceMapper extends BaseMapperPlus<CallDevice, CallDeviceVo> {
}
