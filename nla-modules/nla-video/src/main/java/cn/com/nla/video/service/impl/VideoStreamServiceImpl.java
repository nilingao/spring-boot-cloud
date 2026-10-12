package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.vo.media.OnStreamChangedResult;
import cn.com.nla.common.video.basic.vo.media.ZLMServerConfig;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.basic.vo.video.StreamProxyVo;
import cn.com.nla.common.video.core.demo.InviteInfo;
import cn.com.nla.common.video.core.demo.StreamInfo;
import cn.com.nla.common.video.core.media.client.MediaClient;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.video.domain.StreamProxy;
import cn.com.nla.video.domain.StreamPush;
import cn.com.nla.video.mapper.StreamProxyMapper;
import cn.com.nla.video.mapper.StreamPushMapper;
import cn.com.nla.video.service.IVideoStreamService;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 流媒体运行态服务实现。
 *
 * <p>播放地址、流信息与 FFmpeg 模板均来自流媒体节点的实时状态，不落在业务库；
 * 拉流代理的启停复用协议侧的 {@code StreamProxyVoService}，业务库只维护配置。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，避免依赖协议侧的节点上下文。</p>
 *
 * @author TZY
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class VideoStreamServiceImpl implements IVideoStreamService {

    private final StreamProxyMapper streamProxyMapper;
    private final StreamPushMapper streamPushMapper;

    @Override
    public StreamInfo queryInviteStreamUrl(String deviceId, String channelId) {
        InviteInfo inviteInfo = RedisService.getInviteStreamManager().getInviteInfo(null, deviceId, channelId, null, null);
        if (inviteInfo == null || inviteInfo.getStreamInfo() == null) {
            throw new ServiceException("当前流未播放");
        }
        return inviteInfo.getStreamInfo();
    }

    @Override
    public OnStreamChangedResult queryMediaInfo(String app, String stream, String mediaServerId) {
        MediaServerVo mediaServerVo = VideoService.getMediaServerService().findOnLineMediaServerId(mediaServerId);
        if (mediaServerVo == null) {
            throw new ServiceException("未获取流媒体信息");
        }
        OnStreamChangedResult mediaInfo = MediaClient.getMediaInfo(mediaServerVo, null, null, app, stream);
        if (mediaInfo == null || mediaInfo.getCode() != RespCode.CODE_0.getValue()) {
            throw new ServiceException("未获取当前流信息");
        }
        return mediaInfo;
    }

    @Override
    public String queryFfmpegCmd(String mediaServerId) {
        MediaServerVo mediaServerVo = VideoService.getMediaServerService().findOnLineMediaServerId(mediaServerId);
        if (mediaServerVo == null) {
            throw new ServiceException("未获取到当前流媒体信息");
        }
        ZLMServerConfig zlmServerConfig = MediaClient.getZLMServerConfig(mediaServerVo);
        if (zlmServerConfig == null) {
            throw new ServiceException("未获取到当前流媒体信息");
        }
        return zlmServerConfig.getFfmpegCmd();
    }

    @Override
    public void startProxy(Long id) {
        StreamProxy streamProxy = streamProxyMapper.selectById(id);
        if (streamProxy == null) {
            throw new ServiceException("未获取拉流代理");
        }
        StreamProxyVo streamProxyVo = BeanUtil.copyProperties(streamProxy, StreamProxyVo.class);
        if (!VideoService.getStreamProxyService().start(streamProxyVo)) {
            throw new ServiceException("启用失败");
        }
    }

    @Override
    public void stopProxy(Long id) {
        StreamProxy streamProxy = streamProxyMapper.selectById(id);
        if (streamProxy == null) {
            throw new ServiceException("未获取拉流代理");
        }
        if (!VideoService.getStreamProxyService().stop(streamProxy.getApp(), streamProxy.getStream())) {
            throw new ServiceException("停用失败");
        }
    }

    @Override
    public StreamInfo queryProxyStreamUrl(Long id) {
        StreamProxy streamProxy = streamProxyMapper.selectById(id);
        if (streamProxy == null) {
            throw new ServiceException("未获取拉流代理");
        }
        return requireStreamInfo(streamProxy.getStatus(), streamProxy.getMediaServerId(),
            streamProxy.getApp(), streamProxy.getStream());
    }

    @Override
    public StreamInfo queryPushStreamUrl(Long id) {
        StreamPush streamPush = streamPushMapper.selectById(id);
        if (streamPush == null) {
            throw new ServiceException("未获取推流");
        }
        return requireStreamInfo(streamPush.getStatus(), streamPush.getMediaServerId(),
            streamPush.getApp(), streamPush.getStream());
    }

    /**
     * 校验流任务已上线，并回查其在流媒体节点上的各协议播放地址。
     *
     * @param status        流任务在线状态
     * @param mediaServerId 流媒体服务编号
     * @param app           流应用名
     * @param stream        流编号
     * @return 播放地址信息
     */
    private StreamInfo requireStreamInfo(Integer status, String mediaServerId, String app, String stream) {
        if (status == null || status != ConstEnum.Flag.YES.getValue()) {
            throw new ServiceException("当前流未上线");
        }
        if (StringUtils.isEmpty(mediaServerId)) {
            throw new ServiceException("未获取当前流媒体信息");
        }
        MediaServerVo mediaServerVo = VideoService.getMediaServerService().findOnLineMediaServerId(mediaServerId);
        StreamInfo streamInfo = MediaClient.getStreamInfo(mediaServerVo, app, stream, null, null);
        if (streamInfo == null) {
            throw new ServiceException("未获取当前流信息");
        }
        return streamInfo;
    }
}
