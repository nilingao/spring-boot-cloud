package cn.com.nla.video.spi;

import cn.com.nla.common.video.basic.vo.video.DeviceMobilePositionVo;
import cn.com.nla.common.video.core.service.video.DeviceMobilePositionVoService;
import cn.com.nla.video.domain.DeviceMobilePosition;
import cn.com.nla.video.mapper.DeviceMobilePositionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 设备移动位置协议侧实现：登记并回查通道最新位置上报。
 *
 * <p>仅在 {@code video.enabled=true} 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class DeviceMobilePositionVoServiceImpl extends DeviceMobilePositionVoService {

    private final DeviceMobilePositionMapper deviceMobilePositionMapper;

    /**
     * 保存位置信息：按设备与通道编号覆盖历史位置。
     *
     * @param deviceMobilePositionVo 位置视图
     */
    @Override
    public void save(DeviceMobilePositionVo deviceMobilePositionVo) {
        DeviceMobilePosition add = VideoSpiConvert.to(deviceMobilePositionVo, DeviceMobilePosition.class);
        DeviceMobilePosition exist = deviceMobilePositionMapper.lambda()
            .eq(DeviceMobilePosition::getDeviceId, add.getDeviceId())
            .eq(DeviceMobilePosition::getChannelId, add.getChannelId())
            .one();
        if (exist == null) {
            deviceMobilePositionMapper.insert(add);
        } else {
            add.setId(exist.getId());
            deviceMobilePositionMapper.updateById(add);
        }
    }

    /**
     * 查询通道最近一次位置上报。
     *
     * @param channelId 通道国标编号
     * @return 位置视图，未获取到返回 null
     */
    @Override
    public DeviceMobilePositionVo findLastChannelId(String channelId) {
        DeviceMobilePosition last = deviceMobilePositionMapper.lambda()
            .eq(DeviceMobilePosition::getChannelId, channelId)
            .orderByDesc(DeviceMobilePosition::getCreateTime)
            .last("limit 1")
            .one();
        return VideoSpiConvert.to(last, DeviceMobilePositionVo.class);
    }
}
