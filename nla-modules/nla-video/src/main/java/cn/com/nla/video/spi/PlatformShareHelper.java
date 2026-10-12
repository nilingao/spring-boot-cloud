package cn.com.nla.video.spi;

import cn.com.nla.video.domain.DeviceChannel;
import cn.com.nla.video.domain.GbStream;
import cn.com.nla.video.domain.PlatformGbChannel;
import cn.com.nla.video.domain.PlatformGbStream;
import cn.com.nla.video.mapper.DeviceChannelMapper;
import cn.com.nla.video.mapper.GbStreamMapper;
import cn.com.nla.video.mapper.PlatformGbChannelMapper;
import cn.com.nla.video.mapper.PlatformGbStreamMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 上级平台共享关系清理。
 *
 * <p>设备通道或国标流被删除时，需要同步移除级联共享记录，否则上级平台目录仍会下发已消失的节点。
 * 关联表以国标编号（通道编号 / 流国标编号）作为业务外键，因此先取编号集合再逻辑删除。</p>
 *
 * @author TZY
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class PlatformShareHelper {

    private final DeviceChannelMapper deviceChannelMapper;
    private final GbStreamMapper gbStreamMapper;
    private final PlatformGbChannelMapper platformGbChannelMapper;
    private final PlatformGbStreamMapper platformGbStreamMapper;

    /**
     * 移除设备通道的级联共享记录。
     *
     * @param deviceId  设备国标编号，为空时不按设备过滤
     * @param channelId 通道国标编号，为空时不按通道过滤
     */
    public void removeChannelRel(String deviceId, String channelId) {
        if (StringUtils.isEmpty(deviceId) && StringUtils.isEmpty(channelId)) {
            return;
        }
        List<String> channelIds = deviceChannelMapper.lambda()
            .select(DeviceChannel::getChannelId)
            .eq(StringUtils.isNotEmpty(deviceId), DeviceChannel::getDeviceId, deviceId)
            .eq(StringUtils.isNotEmpty(channelId), DeviceChannel::getChannelId, channelId)
            .list()
            .stream()
            .map(DeviceChannel::getChannelId)
            .toList();
        if (channelIds.isEmpty()) {
            return;
        }
        platformGbChannelMapper.lambda().in(PlatformGbChannel::getDeviceChannelId, channelIds).deleteCount();
    }

    /**
     * 移除国标流的级联共享记录。
     *
     * @param app    流应用名
     * @param stream 流编号
     */
    public void removeStreamRel(String app, String stream) {
        if (StringUtils.isEmpty(app) || StringUtils.isEmpty(stream)) {
            return;
        }
        List<String> gbIds = gbStreamMapper.lambda()
            .select(GbStream::getGbId)
            .eq(GbStream::getApp, app)
            .eq(GbStream::getStream, stream)
            .list()
            .stream()
            .map(GbStream::getGbId)
            .toList();
        if (gbIds.isEmpty()) {
            return;
        }
        platformGbStreamMapper.lambda().in(PlatformGbStream::getGbStreamId, gbIds).deleteCount();
    }
}
