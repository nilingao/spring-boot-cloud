package cn.com.nla.video.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.video.domain.DeviceMobilePosition;
import cn.com.nla.video.domain.vo.DeviceMobilePositionVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 移动位置信息 Mapper。
 * @author TZY
 */
@DS("nla_video")
public interface DeviceMobilePositionMapper extends BaseMapperPlus<DeviceMobilePosition, DeviceMobilePositionVo> {
}
