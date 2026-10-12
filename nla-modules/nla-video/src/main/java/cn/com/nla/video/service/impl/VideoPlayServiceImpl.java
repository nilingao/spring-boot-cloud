package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.enums.ProxyTypeEnum;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.enums.VideoStreamType;
import cn.com.nla.common.video.basic.exception.SsrcTransactionNotFoundException;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.vo.media.MediaRestResult;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.basic.vo.video.StreamProxyVo;
import cn.com.nla.common.video.core.demo.InviteInfo;
import cn.com.nla.common.video.core.demo.StreamInfo;
import cn.com.nla.common.video.core.media.client.MediaClient;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.service.PlayService;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.video.command.VideoCommandDispatcher;
import cn.com.nla.video.command.VideoCommandKeys;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.domain.vo.VideoConvertVo;
import cn.com.nla.video.service.IVideoPlayService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.Map;

/**
 * 实时视频点播服务实现。
 *
 * <p>点播/停止是 SIP 异步过程，HTTP 线程只下发指令并返回受理凭据，收流结果由
 * {@link VideoCommandDispatcher} 经 SSE 推送；转码是流媒体同步接口，直接返回结果。</p>
 *
 * @author TZY
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class VideoPlayServiceImpl implements IVideoPlayService {

    /**
     * 转码输出流的应用名，与流媒体约定保持一致
     */
    private static final String CONVERT_APP = "convert";

    /**
     * 转码任务超时，单位毫秒
     */
    private static final int CONVERT_TIMEOUT_MS = 20000;

    private final SipServer sipServer;
    private final SIPCommander sipCommander;
    private final PlayService playService;
    private final VideoCommandDispatcher videoCommandDispatcher;

    @Override
    public VideoCommandTicketVo play(String deviceId, String channelId) {
        return videoCommandDispatcher.dispatch("play", VideoCommandKeys.play(deviceId, channelId), handle -> {
            DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceId);
            if (deviceVo == null) {
                handle.fail(String.format("设备：%s 未找到", deviceId));
                return;
            }
            MediaServerVo mediaServerVo = VideoService.getMediaServerService().findMediaServerForMinimumLoad(deviceVo);
            if (mediaServerVo == null) {
                handle.fail("流媒体未找到");
                return;
            }
            playService.play(sipServer, mediaServerVo, deviceId, channelId, null,
                (code, msg, data) -> handle.complete(ProtocolResult.result(code, msg, data)));
        });
    }

    @Override
    public VideoCommandTicketVo stop(String deviceId, String channelId) {
        return videoCommandDispatcher.dispatch("play-stop", VideoCommandKeys.stop(deviceId, channelId), handle -> {
            DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceId);
            if (deviceVo == null) {
                handle.fail(String.format("设备：%s 未找到", deviceId));
                return;
            }
            InviteInfo inviteInfo = RedisService.getInviteStreamManager()
                .getInviteInfoByDeviceAndChannel(VideoStreamType.play, deviceId, channelId);
            if (inviteInfo == null || inviteInfo.getStreamInfo() == null) {
                handle.fail("点播未找到");
                return;
            }
            try {
                sipCommander.streamByeCmd(sipServer, deviceVo, channelId, inviteInfo.getStream(), null, null,
                    ok -> handle.success(null), error -> handle.fail(error.getStatusCode(), error.getMsg()));
            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                log.error("[停止点播] 发送bye失败：", e);
                handle.fail("停止点播失败");
            }
        });
    }

    @Override
    public VideoConvertVo convert(String streamId) {
        InviteInfo inviteInfo = RedisService.getInviteStreamManager().getInviteInfoByStream(null, streamId);
        if (inviteInfo == null || inviteInfo.getStreamInfo() == null) {
            throw new ServiceException("点播未找到，视频流可能已停止");
        }
        String mediaServerId = inviteInfo.getStreamInfo().getMediaServerId();
        MediaServerVo mediaServerVo = VideoService.getMediaServerService().findOnLineMediaServerId(mediaServerId);
        if (mediaServerVo == null) {
            throw new ServiceException("流媒体未找到");
        }
        MediaRestResult rtpInfo = MediaClient.getRtpInfo(mediaServerVo, streamId);
        if (rtpInfo == null || !Boolean.TRUE.equals(rtpInfo.getExist())) {
            throw new ServiceException("未找到视频流信息，视频流可能已停止推流");
        }
        StreamProxyVo proxy = StreamProxyVo.builder()
            .type(ProxyTypeEnum.FFMPEG.getValue())
            .srcUrl(String.format("rtsp://%s:%s/rtp/%s", "127.0.0.1", mediaServerVo.getRtspPort(), streamId))
            .dstUrl(String.format("rtmp://%s:%s/convert/%s", "127.0.0.1", mediaServerVo.getRtmpPort(), streamId))
            .timeoutMs(CONVERT_TIMEOUT_MS)
            .enableMp4(ConstEnum.Flag.NO.getValue())
            .ffmpegCmdKey(null)
            .build();
        MediaRestResult result = MediaClient.addStreamProxyToZlm(mediaServerVo, proxy);
        if (result == null || result.getCode() != RespCode.CODE_0.getValue()) {
            throw new ServiceException("转码失败");
        }
        Map<String, Object> data = BeanUtil.beanToMap(result.getData());
        StreamInfo streamInfo = MediaClient.getStreamInfo(mediaServerVo, CONVERT_APP, streamId, null, null);
        return new VideoConvertVo(MapUtil.getStr(data, "key"), mediaServerId, streamInfo);
    }

    @Override
    public void convertStop(String key, String mediaServerId) {
        MediaServerVo mediaServerVo = VideoService.getMediaServerService().findOnLineMediaServerId(mediaServerId);
        if (mediaServerVo == null) {
            throw new ServiceException("流媒体未找到");
        }
        MediaRestResult result = MediaClient.delFfmpegSource(mediaServerVo, key);
        if (result == null || result.getCode() != RespCode.CODE_0.getValue()) {
            throw new ServiceException("结束转码失败");
        }
        Map<String, Object> data = BeanUtil.beanToMap(result.getData());
        Boolean flag = MapUtil.getBool(data, "flag", Boolean.FALSE);
        if (!flag) {
            throw new ServiceException("转码已结束");
        }
    }
}
