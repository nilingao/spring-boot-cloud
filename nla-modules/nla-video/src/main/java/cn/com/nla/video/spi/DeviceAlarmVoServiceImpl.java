package cn.com.nla.video.spi;

import cn.com.nla.common.video.basic.vo.video.DeviceAlarmVo;
import cn.com.nla.common.video.core.service.video.DeviceAlarmVoService;
import cn.com.nla.video.domain.DeviceAlarm;
import cn.com.nla.video.mapper.DeviceAlarmMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 设备报警协议侧实现：登记信令层收到的报警记录。
 *
 * <p>仅在 {@code video.enabled=true} 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class DeviceAlarmVoServiceImpl extends DeviceAlarmVoService {

    private final DeviceAlarmMapper deviceAlarmMapper;

    /**
     * 保存报警记录。
     *
     * @param deviceAlarmVo 报警视图
     */
    @Override
    public void insert(DeviceAlarmVo deviceAlarmVo) {
        deviceAlarmMapper.insert(VideoSpiConvert.to(deviceAlarmVo, DeviceAlarm.class));
    }
}
