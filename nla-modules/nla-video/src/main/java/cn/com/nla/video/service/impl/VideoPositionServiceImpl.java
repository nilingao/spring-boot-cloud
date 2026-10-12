package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.core.demo.DeviceNotifyVo;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.DeviceNotifySubscribeManager;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.video.command.VideoCommandDispatcher;
import cn.com.nla.video.command.VideoCommandKeys;
import cn.com.nla.video.domain.vo.DeviceMobilePositionVo;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.service.IDeviceMobilePositionService;
import cn.com.nla.video.service.IVideoPositionService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备移动位置服务实现。
 *
 * <p>历史与最新位置直接查库；实时位置需要设备应答，走 SSE 异步回执；订阅只维护设备周期字段与
 * 订阅管理器，坐标换算按设备坐标系重算通道经纬度后逐条落库。</p>
 *
 * @author TZY
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class VideoPositionServiceImpl implements IVideoPositionService {

    /**
     * 订阅来源标记，与旧实现保持一致
     */
    private static final String SUBSCRIBE_SOURCE = "web页面订阅";

    private final SipServer sipServer;
    private final SIPCommander sipCommander;
    private final IDeviceMobilePositionService deviceMobilePositionService;
    private final VideoCommandDispatcher videoCommandDispatcher;

    @Override
    public List<DeviceMobilePositionVo> queryHistory(String deviceId, String channelId, String start, String end) {
        LocalDateTime startTime = parseTime(start);
        LocalDateTime endTime = parseTime(end);
        return deviceMobilePositionService.queryHistory(deviceId, channelId, startTime, endTime);
    }

    @Override
    public DeviceMobilePositionVo queryLatest(String deviceId) {
        return deviceMobilePositionService.queryLatest(deviceId);
    }

    @Override
    public VideoCommandTicketVo queryRealtime(String deviceId, String channelId) {
        return videoCommandDispatcher.dispatch("position-realtime", VideoCommandKeys.mobilePosition(deviceId), handle -> {
            DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceId);
            if (deviceVo == null) {
                handle.fail(String.format("设备：%s 未找到", deviceId));
                return;
            }
            try {
                sipCommander.mobilePostitionQuery(sipServer, deviceVo, channelId, null,
                    error -> handle.fail(error.getStatusCode(), error.getMsg()));
            } catch (InvalidArgumentException | SipException | ParseException e) {
                log.error("[命令发送失败] 获取移动位置信息：", e);
                handle.fail("命令发送失败");
            }
        });
    }

    @Override
    public void subscribe(String deviceId, Integer type, Integer expires) {
        DeviceVo deviceVo = requireDevice(deviceId);
        int cycle = expires == null ? 0 : expires;
        int subscribeType = type == null ? -1 : type;
        DeviceNotifySubscribeManager manager = RedisService.getDeviceNotifySubscribeManager();
        if (subscribeType == DeviceNotifyVo.TypeEnum.CATALOG.getValue()) {
            if (cycle > 0) {
                deviceVo.setSubscribeCycleForCatalog(cycle);
                VideoService.getDeviceService().save(deviceVo);
                manager.addCatalogSubscribe(deviceVo, SUBSCRIBE_SOURCE);
            } else {
                manager.removeCatalogSubscribe(deviceVo);
            }
        } else if (subscribeType == DeviceNotifyVo.TypeEnum.MOBILE_POSITION.getValue()) {
            if (cycle > 0) {
                deviceVo.setSubscribeCycleForMobilePosition(cycle);
                VideoService.getDeviceService().save(deviceVo);
                manager.addMobilePositionSubscribe(deviceVo, SUBSCRIBE_SOURCE);
            } else {
                manager.removeMobilePositionSubscribe(deviceVo);
            }
        } else if (subscribeType == DeviceNotifyVo.TypeEnum.ALARM.getValue()) {
            if (cycle > 0) {
                deviceVo.setSubscribeCycleForAlarm(cycle);
                VideoService.getDeviceService().save(deviceVo);
                manager.addAlarmSubscribe(deviceVo, SUBSCRIBE_SOURCE);
            } else {
                manager.removeAlarmSubscribe(deviceVo);
            }
        } else {
            throw new ServiceException("订阅类型错误");
        }
    }

    @Override
    public void transform(String deviceId) {
        DeviceVo deviceVo = requireDevice(deviceId);
        List<DeviceChannelVo> channels = VideoService.getDeviceChannelService().queryAllChannels(deviceVo.getDeviceId());
        if (channels.isEmpty()) {
            return;
        }
        int geoCoordSys = deviceVo.getGeoCoordSys() == null ? 0 : deviceVo.getGeoCoordSys();
        for (DeviceChannelVo channel : channels) {
            VideoService.getDeviceChannelService().save(channel.initGps(geoCoordSys));
        }
    }

    /**
     * 查询设备，不存在时直接失败，避免后续空指针。
     *
     * @param deviceId 设备国标编号
     * @return 设备视图
     */
    private DeviceVo requireDevice(String deviceId) {
        DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceId);
        if (deviceVo == null) {
            throw new ServiceException(String.format("未获取设备信息：%s", deviceId));
        }
        return deviceVo;
    }

    /**
     * 解析时间串，空串返回 {@code null} 表示不做时间过滤。
     *
     * @param value 时间串
     * @return 时间，无法解析时抛出业务异常
     */
    private LocalDateTime parseTime(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return DateUtil.parse(value).toLocalDateTime();
        } catch (Exception e) {
            throw new ServiceException(String.format("时间格式错误：%s", value));
        }
    }
}
