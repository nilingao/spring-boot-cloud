package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.video.command.VideoCommandDispatcher;
import cn.com.nla.video.command.VideoCommandKeys;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.service.IVideoAudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * 语音对讲与语音广播服务实现。
 *
 * <p>推流地址、推流状态、关闭推流均由协议侧的语音事务管理，本层只负责把协议返回码翻译成
 * 业务异常；语音广播走异步回执，超时与失败时同步回收语音推流事务，避免占用 SSRC。</p>
 *
 * @author TZY
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class VideoAudioServiceImpl implements IVideoAudioService {

    /**
     * 语音广播回执超时，设备不支持语音广播时不会应答，与旧实现保持一致取 5 秒
     */
    private static final long BROADCAST_TIMEOUT_MILLIS = 5_000L;

    private final SipServer sipServer;
    private final SIPCommander sipCommander;
    private final VideoCommandDispatcher videoCommandDispatcher;

    @Override
    public String findPushPath(String deviceId, String channelId) {
        ProtocolResult<?> result = VideoService.getDeviceChannelService().findAudioPushPath(deviceId, channelId);
        if (!result.ok()) {
            throw new ServiceException(result.getMessage());
        }
        return (String) result.getData();
    }

    @Override
    public boolean isPushing(String deviceId, String channelId) {
        ProtocolResult<?> result = VideoService.getDeviceChannelService().findAudioPushStatus(deviceId, channelId);
        return Boolean.TRUE.equals(result.getData());
    }

    @Override
    public void stopPush(String deviceId, String channelId) {
        ProtocolResult<?> result = VideoService.getDeviceChannelService().stopAudioPushStatus(deviceId, channelId);
        if (!result.ok()) {
            throw new ServiceException(result.getMessage());
        }
    }

    @Override
    public VideoCommandTicketVo broadcast(String deviceId, String channelId) {
        return videoCommandDispatcher.dispatch("audio-broadcast", VideoCommandKeys.broadcast(deviceId, channelId),
            BROADCAST_TIMEOUT_MILLIS,
            () -> {
                // 设备不支持语音广播时不会应答，超时即回收本次登记的语音推流事务
                stopPushQuietly(deviceId, channelId);
                return ProtocolResult.result(RespCode.CODE_2.getValue(), "设备不支持");
            },
            handle -> {
                DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceId);
                if (deviceVo == null) {
                    handle.fail(String.format("设备：%s 未找到", deviceId));
                    return;
                }
                try {
                    sipCommander.audioBroadcastCmd(sipServer, deviceVo, channelId, null, error -> {
                        handle.fail(error.getStatusCode(), error.getMsg());
                        stopPushQuietly(deviceId, channelId);
                    });
                } catch (InvalidArgumentException | SipException | ParseException e) {
                    log.error("[命令发送失败] 语音广播：", e);
                    handle.fail("语音广播命令失败");
                    stopPushQuietly(deviceId, channelId);
                }
            });
    }

    /**
     * 静默回收语音推流事务，回收失败不影响回执内容本身。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     */
    private void stopPushQuietly(String deviceId, String channelId) {
        try {
            stopPush(deviceId, channelId);
        } catch (ServiceException e) {
            log.warn("[语音推流回收失败] 设备：{}，通道：{}，原因：{}", deviceId, channelId, e.getMessage());
        }
    }
}
