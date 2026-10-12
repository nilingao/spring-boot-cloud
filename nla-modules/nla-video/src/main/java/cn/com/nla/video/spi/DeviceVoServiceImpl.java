package cn.com.nla.video.spi;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.core.service.video.DeviceVoService;
import cn.com.nla.video.domain.Device;
import cn.com.nla.video.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备协议侧实现：供 SIP 信令层读写设备与设备状态。
 *
 * <p>仅在 {@code video.enabled=true} 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class DeviceVoServiceImpl extends DeviceVoService {

    private final DeviceMapper deviceMapper;

    /**
     * 查询全部在线设备。
     *
     * @return 在线设备列表
     */
    @Override
    public List<DeviceVo> getAllOnlineDevice() {
        return VideoSpiConvert.toList(
            deviceMapper.lambda().eq(Device::getOnline, ConstEnum.Flag.YES.getValue()).list(), DeviceVo.class);
    }

    /**
     * 按设备国标编号查询设备。
     *
     * @param deviceGbId 设备国标编号
     * @return 设备视图，未获取到返回 null
     */
    @Override
    public DeviceVo findDeviceGbId(String deviceGbId) {
        if (StringUtils.isEmpty(deviceGbId)) {
            log.error("[查询设备信息失败] deviceGbId is null");
            return null;
        }
        return VideoSpiConvert.to(deviceMapper.lambda().eq(Device::getDeviceId, deviceGbId).one(), DeviceVo.class);
    }

    /**
     * 按上级平台与其共享的通道编号查询设备。
     *
     * @param platformId 上级平台国标编号
     * @param channelId  通道国标编号
     * @return 设备视图，未获取到返回 null
     */
    @Override
    public DeviceVo findPlatformIdChannelId(String platformId, String channelId) {
        return VideoSpiConvert.to(deviceMapper.selectDeviceByPlatformChannel(platformId, channelId), DeviceVo.class);
    }

    /**
     * 按上级平台与其共享的通道编号查询设备，设备编号与名称替换为通道信息。
     *
     * @param platformId 上级平台国标编号
     * @param channelId  通道国标编号
     * @return 设备视图，未获取到返回 null
     */
    @Override
    public DeviceVo findDeviceInfoPlatformIdChannelId(String platformId, String channelId) {
        return VideoSpiConvert.to(deviceMapper.selectDeviceChannelInfoByPlatformChannel(platformId, channelId),
            DeviceVo.class);
    }

    /**
     * 查询可作为消息通道且在线的设备。
     *
     * @return 设备列表
     */
    @Override
    public List<DeviceVo> queryDeviceWithAsMessageChannel() {
        return VideoSpiConvert.toList(deviceMapper.lambda()
            .eq(Device::getOnline, ConstEnum.Flag.YES.getValue())
            .eq(Device::getAsMessageChannel, ConstEnum.Flag.YES.getValue())
            .list(), DeviceVo.class);
    }

    /**
     * 保存设备：按设备国标编号判断新增或更新。
     *
     * @param deviceVo 设备视图
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int save(DeviceVo deviceVo) {
        Device add = VideoSpiConvert.to(deviceVo, Device.class);
        Device device = deviceMapper.lambda().eq(Device::getDeviceId, add.getDeviceId()).one();
        if (device == null) {
            return deviceMapper.insert(add) > 0 ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
        }
        add.setId(device.getId());
        return deviceMapper.updateById(add) > 0 ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 更新设备在线状态。
     *
     * @param id     设备主键
     * @param status 是否在线
     */
    @Override
    public void updateStatus(Long id, boolean status) {
        if (id == null) {
            log.error("[设备状态更变] ：未获取设备编码");
            return;
        }
        Device update = new Device();
        update.setId(id);
        update.setOnline(status ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue());
        if (status) {
            update.setRegisterTime(LocalDateTime.now());
        }
        deviceMapper.updateById(update);
    }

    /**
     * 更新设备绑定的流媒体服务。
     *
     * @param deviceGbId    设备国标编号
     * @param mediaServerId 流媒体服务编号
     */
    @Override
    public void updateZlm(String deviceGbId, String mediaServerId) {
        deviceMapper.lambda()
            .set(Device::getMediaServerId, mediaServerId)
            .eq(Device::getDeviceId, deviceGbId)
            .update();
    }
}
