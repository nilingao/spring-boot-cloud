package cn.com.nla.video.spi;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.enums.VideoStreamType;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.core.demo.InviteInfo;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.DeviceChannelVoService;
import cn.com.nla.video.domain.DeviceChannel;
import cn.com.nla.video.mapper.DeviceChannelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备通道协议侧实现：供 SIP 信令层读写通道、目录级联通道与播放状态。
 *
 * <p>仅在 {@code video.enabled=true} 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class DeviceChannelVoServiceImpl implements DeviceChannelVoService {

    /**
     * 语音对讲依赖 SSRC 事务与 WebRTC 推流地址，属于实时指令能力，随后续批次迁移。
     */
    private static final String AUDIO_UNSUPPORTED_MESSAGE = "语音对讲功能将在后续批次迁移";

    private final DeviceChannelMapper deviceChannelMapper;
    private final PlatformShareHelper platformShareHelper;

    /**
     * 查询设备最新登记的通道。
     *
     * @param deviceId 设备国标编号
     * @return 通道视图，未获取到返回 null
     */
    @Override
    public DeviceChannelVo findLastDevice(String deviceId) {
        return VideoSpiConvert.to(deviceChannelMapper.lambda()
            .eq(DeviceChannel::getDeviceId, deviceId)
            .orderByDesc(DeviceChannel::getCreateTime)
            .last("limit 1")
            .one(), DeviceChannelVo.class);
    }

    /**
     * 按通道国标编号查询通道，编号重复视为脏数据。
     *
     * @param channelId 通道国标编号
     * @return 通道视图，未获取到返回 null
     */
    @Override
    public DeviceChannelVo findChannelId(String channelId) {
        List<DeviceChannel> channels = deviceChannelMapper.lambda().eq(DeviceChannel::getChannelId, channelId).list();
        if (channels.isEmpty()) {
            return null;
        }
        if (channels.size() > 1) {
            throw new ServiceException("设备通道国标重复，请检查数据");
        }
        return VideoSpiConvert.to(channels.get(0), DeviceChannelVo.class);
    }

    /**
     * 按上级平台国标编号与其共享的通道编号查询通道。
     *
     * @param platformId 上级平台国标编号
     * @param channelId  通道国标编号
     * @return 通道视图，未获取到返回 null
     */
    @Override
    public DeviceChannelVo findPlatformIdChannelId(String platformId, String channelId) {
        return VideoSpiConvert.to(deviceChannelMapper.selectChannelByPlatformChannel(platformId, channelId),
            DeviceChannelVo.class);
    }

    /**
     * 按设备与通道国标编号查询通道。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 通道视图，未获取到返回 null
     */
    @Override
    public DeviceChannelVo findDeviceIdChannelId(String deviceId, String channelId) {
        return VideoSpiConvert.to(deviceChannelMapper.lambda()
            .eq(DeviceChannel::getDeviceId, deviceId)
            .eq(DeviceChannel::getChannelId, channelId)
            .one(), DeviceChannelVo.class);
    }

    /**
     * 更新通道在线状态，通道编号为空时更新设备全部通道。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param online    是否在线
     */
    @Override
    public void deviceChannelOnline(String deviceId, String channelId, boolean online) {
        deviceChannelMapper.lambda()
            .set(DeviceChannel::getStatus, online ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue())
            .eq(DeviceChannel::getDeviceId, deviceId)
            .eq(StringUtils.isNotEmpty(channelId), DeviceChannel::getChannelId, channelId)
            .update();
    }

    /**
     * 保存通道：设备必须存在，并按设备与通道编号判断新增或更新。
     *
     * @param deviceChannelVo 通道视图
     * @return 成功返回 1，失败返回 0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int save(DeviceChannelVo deviceChannelVo) {
        DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceChannelVo.getDeviceId());
        if (deviceVo == null) {
            log.error("[保存设备通道] 设备不存在 deviceId：{}", deviceChannelVo.getDeviceId());
            return ConstEnum.Flag.NO.getValue();
        }
        InviteInfo inviteInfo = RedisService.getInviteStreamManager()
            .getInviteInfoByDeviceAndChannel(VideoStreamType.play, deviceChannelVo.getDeviceId(),
                deviceChannelVo.getChannelId());
        if (inviteInfo != null) {
            deviceChannelVo.setStreamId(inviteInfo.getStream());
        }
        deviceChannelVo.initGps(deviceVo.getGeoCoordSys() == null ? 0 : deviceVo.getGeoCoordSys());
        DeviceChannel add = VideoSpiConvert.to(deviceChannelVo, DeviceChannel.class);
        DeviceChannel exist = deviceChannelMapper.lambda()
            .eq(DeviceChannel::getDeviceId, add.getDeviceId())
            .eq(DeviceChannel::getChannelId, add.getChannelId())
            .one();
        boolean saved;
        if (exist == null) {
            saved = deviceChannelMapper.insert(add) > 0;
        } else {
            add.setId(exist.getId());
            saved = deviceChannelMapper.updateById(add) > 0;
        }
        deviceChannelMapper.updateChannelSubCount(deviceChannelVo.getDeviceId(), deviceChannelVo.getChannelId());
        return saved ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 保存设备通道位置信息，设备编号与通道编号相同时视为设备级位置。
     *
     * @param deviceChannelVo 通道视图
     */
    @Override
    public void updateMobilePosition(DeviceChannelVo deviceChannelVo) {
        String channelId = deviceChannelVo.getChannelId();
        if (channelId != null && channelId.equals(deviceChannelVo.getDeviceId())) {
            deviceChannelVo.setChannelId(null);
        }
        if (deviceChannelVo.getGpsTime() == null) {
            deviceChannelVo.setGpsTime(LocalDateTime.now());
        }
        DeviceChannel update = VideoSpiConvert.to(deviceChannelVo, DeviceChannel.class);
        deviceChannelMapper.lambda()
            .eq(DeviceChannel::getDeviceId, update.getDeviceId())
            .eq(StringUtils.isNotEmpty(update.getChannelId()), DeviceChannel::getChannelId, update.getChannelId())
            .update(update);
    }

    /**
     * 删除设备全部通道，并清理级联共享记录。
     *
     * @param deviceId 设备国标编号
     * @return 成功返回 1，失败返回 0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delAll(String deviceId) {
        if (StringUtils.isEmpty(deviceId)) {
            return ConstEnum.Flag.NO.getValue();
        }
        platformShareHelper.removeChannelRel(deviceId, null);
        boolean removed = deviceChannelMapper.lambda().eq(DeviceChannel::getDeviceId, deviceId).deleteCount() > 0;
        return removed ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 删除单个通道，并清理级联共享记录。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 成功返回 1，失败返回 0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int del(String deviceId, String channelId) {
        if (StringUtils.isEmpty(deviceId) || StringUtils.isEmpty(channelId)) {
            return ConstEnum.Flag.NO.getValue();
        }
        platformShareHelper.removeChannelRel(deviceId, channelId);
        boolean removed = deviceChannelMapper.lambda()
            .eq(DeviceChannel::getDeviceId, deviceId)
            .eq(DeviceChannel::getChannelId, channelId)
            .deleteCount() > 0;
        return removed ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 查询设备全部通道。
     *
     * @param deviceId 设备国标编号
     * @return 通道列表
     */
    @Override
    public List<DeviceChannelVo> queryAllChannels(String deviceId) {
        return VideoSpiConvert.toList(
            deviceChannelMapper.lambda().eq(DeviceChannel::getDeviceId, deviceId).list(), DeviceChannelVo.class);
    }

    /**
     * 以信令侧目录查询结果覆盖设备通道：新增缺失、更新已有、删除对方没有的通道。
     *
     * @param deviceId    设备国标编号
     * @param channelList 目录查询返回的通道列表
     * @return 覆盖是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetChannels(String deviceId, List<DeviceChannelVo> channelList) {
        Map<String, DeviceChannel> existMap = new HashMap<>();
        for (DeviceChannel channel : deviceChannelMapper.lambda().eq(DeviceChannel::getDeviceId, deviceId).list()) {
            existMap.put(channel.getChannelId(), channel);
        }
        Map<String, List<DeviceChannelVo>> subMap = channelList.stream()
            .filter(o -> StringUtils.isNotEmpty(o.getDeviceId()))
            .collect(Collectors.groupingBy(DeviceChannelVo::getDeviceId));
        Map<String, DeviceChannel> incomingMap = new HashMap<>();
        for (DeviceChannelVo channelVo : channelList) {
            if (StringUtils.isEmpty(channelVo.getChannelId())) {
                continue;
            }
            channelVo.setSubCount(subMap.getOrDefault(channelVo.getChannelId(), List.of()).size());
            incomingMap.put(channelVo.getChannelId(), VideoSpiConvert.to(channelVo, DeviceChannel.class));
        }
        if (incomingMap.isEmpty()) {
            log.error("[更新设备全部通道信息] 未获取到更新设备");
            return false;
        }
        List<String> deleteList = existMap.keySet().stream().filter(key -> !incomingMap.containsKey(key)).toList();
        List<String> addList = incomingMap.keySet().stream().filter(key -> !existMap.containsKey(key)).toList();
        if (!deleteList.isEmpty()) {
            deviceChannelMapper.lambda()
                .eq(DeviceChannel::getDeviceId, deviceId)
                .in(DeviceChannel::getChannelId, deleteList)
                .deleteCount();
        }
        List<DeviceChannel> addChannels = new ArrayList<>();
        for (String channelId : addList) {
            addChannels.add(incomingMap.get(channelId));
        }
        if (!addChannels.isEmpty()) {
            deviceChannelMapper.insertBatch(addChannels, 300);
            addList.forEach(incomingMap::remove);
        }
        if (!incomingMap.isEmpty()) {
            List<DeviceChannel> updateChannels = new ArrayList<>();
            for (DeviceChannel channel : incomingMap.values()) {
                channel.setId(existMap.get(channel.getChannelId()).getId());
                updateChannels.add(channel);
            }
            deviceChannelMapper.updateBatchById(updateChannels, 300);
        }
        return true;
    }

    /**
     * 查询上级平台已共享的通道，父级编号取所在目录。
     *
     * @param serverGbId 上级平台国标编号
     * @return 通道列表
     */
    @Override
    public List<DeviceChannelVo> queryChannelWithCatalog(String serverGbId) {
        return VideoSpiConvert.toList(deviceChannelMapper.selectChannelWithCatalog(serverGbId), DeviceChannelVo.class);
    }

    /**
     * 记录通道正在推送的流编号。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param stream    流编号
     */
    @Override
    public void startPlay(String deviceId, String channelId, String stream) {
        deviceChannelMapper.lambda()
            .set(DeviceChannel::getStreamId, stream)
            .eq(DeviceChannel::getDeviceId, deviceId)
            .eq(DeviceChannel::getChannelId, channelId)
            .update();
    }

    /**
     * 清除通道正在推送的流编号。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     */
    @Override
    public void stopPlay(String deviceId, String channelId) {
        deviceChannelMapper.lambda()
            .set(DeviceChannel::getStreamId, "")
            .eq(DeviceChannel::getDeviceId, deviceId)
            .eq(DeviceChannel::getChannelId, channelId)
            .update();
    }

    /**
     * 获取语音对讲推流地址。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 暂不支持的业务提示
     */
    @Override
    public ProtocolResult<?> findAudioPushPath(String deviceId, String channelId) {
        return ProtocolResult.result(RespCode.CODE_2.getValue(), AUDIO_UNSUPPORTED_MESSAGE);
    }

    /**
     * 获取语音对讲推流状态。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 暂不支持的业务提示
     */
    @Override
    public ProtocolResult<?> findAudioPushStatus(String deviceId, String channelId) {
        return ProtocolResult.result(RespCode.CODE_2.getValue(), AUDIO_UNSUPPORTED_MESSAGE);
    }

    /**
     * 关闭语音对讲推流。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 暂不支持的业务提示
     */
    @Override
    public ProtocolResult<?> stopAudioPushStatus(String deviceId, String channelId) {
        return ProtocolResult.result(RespCode.CODE_2.getValue(), AUDIO_UNSUPPORTED_MESSAGE);
    }
}
