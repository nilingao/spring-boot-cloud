package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.enums.VideoStreamType;
import cn.com.nla.common.video.basic.exception.SsrcTransactionNotFoundException;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.vo.media.MediaRestResult;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
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
import cn.com.nla.video.service.IVideoPlaybackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * 历史视频回放服务实现。
 *
 * <p>暂停/恢复除了下发 INFO 指令，还必须先暂停/恢复流媒体侧的 RTP 超时检查，
 * 否则暂停期间收流超时会直接断流；该顺序与旧实现保持一致。</p>
 *
 * @author TZY
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class VideoPlaybackServiceImpl implements IVideoPlaybackService {

    /**
     * 国标协议允许的回放倍速
     */
    private static final List<Double> SUPPORTED_SPEED = List.of(0.25, 0.5, 1.0, 2.0, 4.0);

    private final SipServer sipServer;
    private final SIPCommander sipCommander;
    private final PlayService playService;
    private final VideoCommandDispatcher videoCommandDispatcher;

    @Override
    public VideoCommandTicketVo start(String deviceId, String channelId, String startTime, String endTime) {
        String key = VideoCommandKeys.playback(deviceId, channelId, startTime, endTime);
        return videoCommandDispatcher.dispatch("playback", key, handle -> {
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
            playService.playBack(sipServer, mediaServerVo, deviceVo, channelId, null, startTime, endTime,
                (code, msg, data) -> handle.complete(ProtocolResult.result(code, msg, data)));
        });
    }

    @Override
    public void stop(String deviceId, String channelId, String streamId) {
        DeviceVo deviceVo = requireDevice(deviceId);
        try {
            sipCommander.streamByeCmd(sipServer, deviceVo, channelId, streamId, null, null, null, null);
        } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
            log.error("[停止回放] 发送bye失败：", e);
            throw new ServiceException("停止视频回放失败");
        }
    }

    @Override
    public void suspend(String streamId) {
        InviteInfo inviteInfo = requirePlaybackInvite(streamId);
        StreamInfo streamInfo = inviteInfo.getStreamInfo();
        streamInfo.setPause(true);
        RedisService.getInviteStreamManager().updateInviteInfo(inviteInfo);
        MediaServerVo mediaServerVo = requireMediaServer(streamInfo.getMediaServerId());
        MediaRestResult result = MediaClient.pauseRtpCheck(mediaServerVo, streamId);
        if (result == null || result.getCode() != RespCode.CODE_0.getValue()) {
            throw new ServiceException("暂停RTP接收失败");
        }
        DeviceVo deviceVo = requireDevice(inviteInfo.getDeviceId());
        try {
            sipCommander.playPauseCmd(sipServer, deviceVo, streamInfo, null, null);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[暂停回放] 错误：", e);
            throw new ServiceException("暂停视频回放失败");
        }
    }

    @Override
    public void restore(String streamId) {
        InviteInfo inviteInfo = requirePlaybackInvite(streamId);
        StreamInfo streamInfo = inviteInfo.getStreamInfo();
        streamInfo.setPause(false);
        RedisService.getInviteStreamManager().updateInviteInfo(inviteInfo);
        MediaServerVo mediaServerVo = requireMediaServer(streamInfo.getMediaServerId());
        MediaRestResult result = MediaClient.resumeRtpCheck(mediaServerVo, streamId);
        if (result == null || result.getCode() != RespCode.CODE_0.getValue()) {
            throw new ServiceException("恢复RTP接收失败");
        }
        DeviceVo deviceVo = requireDevice(inviteInfo.getDeviceId());
        try {
            sipCommander.playResumeCmd(sipServer, deviceVo, streamInfo, null, null);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[回放恢复] 错误：", e);
            throw new ServiceException("暂停回放恢复失败");
        }
    }

    @Override
    public void seek(String streamId, Long seekTime) {
        InviteInfo inviteInfo = requirePlaybackInvite(streamId);
        DeviceVo deviceVo = requireDevice(inviteInfo.getDeviceId());
        try {
            sipCommander.playSeekCmd(sipServer, deviceVo, inviteInfo.getStreamInfo(), seekTime, null, null);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[回放拖动播放] 错误：", e);
            throw new ServiceException("回放拖动播放失败");
        }
    }

    @Override
    public void speed(String streamId, Double speed) {
        if (!SUPPORTED_SPEED.contains(speed)) {
            throw new ServiceException("不支持的speed，仅支持 0.25、0.5、1、2、4");
        }
        InviteInfo inviteInfo = requirePlaybackInvite(streamId);
        DeviceVo deviceVo = requireDevice(inviteInfo.getDeviceId());
        try {
            sipCommander.playSpeedCmd(sipServer, deviceVo, inviteInfo.getStreamInfo(), speed, null, null);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[回放倍速播放] 错误：", e);
            throw new ServiceException("回放倍速播放失败");
        }
    }

    /**
     * 取回放中的流信息，流已停止时抛出业务异常。
     *
     * @param streamId 流ID
     * @return 回放邀请信息
     */
    private InviteInfo requirePlaybackInvite(String streamId) {
        InviteInfo inviteInfo = RedisService.getInviteStreamManager()
            .getInviteInfoByStream(VideoStreamType.playback, streamId);
        if (inviteInfo == null || inviteInfo.getStreamInfo() == null) {
            throw new ServiceException("streamId不存在");
        }
        return inviteInfo;
    }

    /**
     * 取在线流媒体节点。
     *
     * @param mediaServerId 流媒体服务ID
     * @return 流媒体信息
     */
    private MediaServerVo requireMediaServer(String mediaServerId) {
        MediaServerVo mediaServerVo = VideoService.getMediaServerService().findOnLineMediaServerId(mediaServerId);
        if (mediaServerVo == null) {
            throw new ServiceException("mediaServer不存在");
        }
        return mediaServerVo;
    }

    /**
     * 取国标设备。
     *
     * @param deviceId 设备国标编号
     * @return 设备信息
     */
    private DeviceVo requireDevice(String deviceId) {
        DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceId);
        if (deviceVo == null) {
            throw new ServiceException("未获取设备信息");
        }
        return deviceVo;
    }
}
