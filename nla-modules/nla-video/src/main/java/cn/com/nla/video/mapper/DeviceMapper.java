package cn.com.nla.video.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.video.domain.Device;
import cn.com.nla.video.domain.vo.DeviceVo;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Param;

/** 设备信息 Mapper。
 * @author TZY
 */
@DS("nla_video")
public interface DeviceMapper extends BaseMapperPlus<Device, DeviceVo> {

    /** 按上级平台国标编号与其共享的通道编号反查设备。 */
    Device selectDeviceByPlatformChannel(@Param("platformId") String platformId, @Param("channelId") String channelId);

    /** 按上级平台国标编号与其共享的通道编号反查设备，并以通道编号、通道名称覆盖设备编号与名称。 */
    Device selectDeviceChannelInfoByPlatformChannel(@Param("platformId") String platformId,
        @Param("channelId") String channelId);
}
