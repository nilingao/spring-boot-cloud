package cn.com.nla.video.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.video.domain.DeviceAlarm;
import cn.com.nla.video.domain.vo.DeviceAlarmVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 报警信息 Mapper。
 * @author TZY
 */
@DS("nla_video")
public interface DeviceAlarmMapper extends BaseMapperPlus<DeviceAlarm, DeviceAlarmVo> {
}
