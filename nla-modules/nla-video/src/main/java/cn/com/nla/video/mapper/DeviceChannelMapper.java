package cn.com.nla.video.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.video.domain.DeviceChannel;
import cn.com.nla.video.domain.vo.DeviceChannelVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 通道信息 Mapper。
 * @author TZY
 */
public interface DeviceChannelMapper extends BaseMapperPlus<DeviceChannel, DeviceChannelVo> {

    /** 按上级平台国标编号与其共享的通道编号反查设备通道。 */
    DeviceChannel selectChannelByPlatformChannel(@Param("platformId") String platformId,
        @Param("channelId") String channelId);

    /** 查询平台共享的通道，坐标取 WGS84，父级编号取共享所在目录。 */
    List<DeviceChannel> selectChannelWithCatalog(@Param("serverGbId") String serverGbId);

    /** 重算指定通道下挂的子通道数量。 */
    int updateChannelSubCount(@Param("deviceId") String deviceId, @Param("channelId") String channelId);

    /** 查询平台共享的国标流并伪装为通道节点，状态取推流或拉流代理。 */
    List<cn.com.nla.common.video.basic.vo.video.DeviceChannelVo> selectGbStreamChannelListInPlatform(
        @Param("serverGbId") String serverGbId, @Param("gbId") String gbId,
        @Param("usePushingAsStatus") boolean usePushingAsStatus);
}
