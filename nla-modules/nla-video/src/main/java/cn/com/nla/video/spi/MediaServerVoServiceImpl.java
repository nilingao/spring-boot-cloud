package cn.com.nla.video.spi;

import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.core.properties.VideoProperties;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.MediaServerVoService;
import cn.com.nla.video.domain.VideoMediaServer;
import cn.com.nla.video.mapper.VideoMediaServerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流媒体服务协议侧实现：供信令与回调层挑选、维护可用的 ZLM 节点。
 *
 * <p>仅在 {@code video.enabled=true} 时注册，避免协议组件缺失导致启动失败。
 * 负载最低节点取自数据库中「启用且在线」的节点，不再依赖 Redis 实时连接数统计。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class MediaServerVoServiceImpl implements MediaServerVoService {

    private final VideoMediaServerMapper videoMediaServerMapper;
    private final VideoProperties videoProperties;

    /**
     * 按编号查询流媒体服务。
     *
     * @param mediaServerId 流媒体服务编号
     * @return 流媒体视图，未获取到返回 null
     */
    @Override
    public MediaServerVo findMediaServerId(String mediaServerId) {
        if (StringUtils.isEmpty(mediaServerId)) {
            log.info("[流媒体信息]：未获取到流媒体编号信息");
            return null;
        }
        VideoMediaServer mediaServer = videoMediaServerMapper.lambda().eq(VideoMediaServer::getId, mediaServerId).one();
        if (mediaServer == null) {
            log.info("[流媒体信息]：未获取到流媒体信息");
            return null;
        }
        return VideoSpiConvert.to(mediaServer, MediaServerVo.class);
    }

    /**
     * 按编号查询启用且在线的流媒体服务。
     *
     * @param mediaServerId 流媒体服务编号
     * @return 流媒体视图，未获取到返回 null
     */
    @Override
    public MediaServerVo findOnLineMediaServerId(String mediaServerId) {
        if (StringUtils.isEmpty(mediaServerId)) {
            log.info("[流媒体信息]：未获取到流媒体编号信息");
            return null;
        }
        VideoMediaServer mediaServer = videoMediaServerMapper.lambda()
            .eq(VideoMediaServer::getEnable, ConstEnum.Flag.YES.getValue())
            .eq(VideoMediaServer::getId, mediaServerId)
            .eq(VideoMediaServer::getStatus, ConstEnum.Flag.YES.getValue())
            .one();
        if (mediaServer == null) {
            log.info("[流媒体信息]：未获取到流媒体信息");
            return null;
        }
        return VideoSpiConvert.to(mediaServer, MediaServerVo.class);
    }

    /**
     * 查询心跳已超时的流媒体服务，用于触发节点重连。
     *
     * @return 失联的流媒体服务列表
     */
    @Override
    public List<MediaServerVo> findConnectZlmList() {
        List<VideoMediaServer> list = videoMediaServerMapper.lambda()
            .eq(VideoMediaServer::getEnable, ConstEnum.Flag.YES.getValue())
            .list();
        return VideoSpiConvert.toList(list.stream().filter(this::isDisconnected).toList(), MediaServerVo.class);
    }

    /**
     * 获取设备绑定的流媒体服务，绑定节点不可用时回退到可用节点并重新绑定。
     *
     * @param deviceVo 设备视图
     * @return 流媒体视图，未获取到返回 null
     */
    @Override
    public MediaServerVo findMediaServerForMinimumLoad(DeviceVo deviceVo) {
        if (deviceVo == null) {
            log.error("未获取设备信息");
            return null;
        }
        MediaServerVo mediaServerItem = null;
        if (StringUtils.isNotEmpty(deviceVo.getMediaServerId())) {
            mediaServerItem = findOnLineMediaServerId(deviceVo.getMediaServerId());
            if (Boolean.TRUE.equals(videoProperties.getUseClientOnLineZlm()) && mediaServerItem == null) {
                mediaServerItem = findMediaServerForMinimumLoad();
                if (mediaServerItem != null) {
                    VideoService.getDeviceService().updateZlm(deviceVo.getDeviceId(), mediaServerItem.getId());
                }
            }
        } else {
            mediaServerItem = findMediaServerForMinimumLoad();
        }
        if (mediaServerItem == null) {
            log.warn("[获取可用的ZLM节点] 未找到可使用的ZLM...");
        }
        return mediaServerItem;
    }

    /**
     * 获取可用的流媒体服务。
     *
     * @return 流媒体视图，无在线节点时返回 null
     */
    @Override
    public MediaServerVo findMediaServerForMinimumLoad() {
        List<VideoMediaServer> list = videoMediaServerMapper.lambda()
            .eq(VideoMediaServer::getEnable, ConstEnum.Flag.YES.getValue())
            .eq(VideoMediaServer::getStatus, ConstEnum.Flag.YES.getValue())
            .orderByAsc(VideoMediaServer::getId)
            .list();
        if (list.isEmpty()) {
            log.info("[流媒体信息] 未获取到在线的流媒体服务");
            return null;
        }
        return VideoSpiConvert.to(list.get(0), MediaServerVo.class);
    }

    /**
     * 按编号更新流媒体服务信息。
     *
     * @param mediaServerVo 流媒体视图
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int updateById(MediaServerVo mediaServerVo) {
        boolean updated = videoMediaServerMapper.updateById(VideoSpiConvert.to(mediaServerVo, VideoMediaServer.class)) > 0;
        return updated ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 更新流媒体服务在线状态，上线时刷新保活时间。
     *
     * @param mediaServerId 流媒体服务编号
     * @param status        在线状态
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int updateStatus(String mediaServerId, Integer status) {
        if (StringUtils.isBlank(mediaServerId) || status == null) {
            log.info("[流媒体信息] 获取信息错误 mediaServerId：{}，status：{}", mediaServerId, status);
            return ConstEnum.Flag.NO.getValue();
        }
        VideoMediaServer update = new VideoMediaServer();
        update.setId(mediaServerId);
        update.setStatus(status);
        if (ConstEnum.Flag.YES.getValue() == status) {
            update.setKeepaliveTime(LocalDateTime.now());
        }
        boolean updated = videoMediaServerMapper.updateById(update) > 0;
        return updated ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 判断节点心跳是否已超时。
     *
     * @param mediaServer 流媒体实体
     * @return 心跳为空或已早于超时阈值返回 true
     */
    private boolean isDisconnected(VideoMediaServer mediaServer) {
        if (mediaServer.getKeepaliveTime() == null) {
            return true;
        }
        int interval = mediaServer.getHookAliveInterval() == null ? 0 : mediaServer.getHookAliveInterval();
        return mediaServer.getKeepaliveTime()
            .isBefore(LocalDateTime.now().minusSeconds(interval + VideoConstant.DELAY_TIME));
    }
}
