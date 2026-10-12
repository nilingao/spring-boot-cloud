package cn.com.nla.video.controller;

import cn.com.nla.common.video.basic.common.ZLMediaKitConstant;
import cn.com.nla.common.video.basic.model.NotNullMap;
import cn.com.nla.common.video.basic.vo.media.HookVo;
import cn.com.nla.common.video.basic.vo.media.OnPlayHookVo;
import cn.com.nla.common.video.basic.vo.media.OnPublishHookVo;
import cn.com.nla.common.video.basic.vo.media.OnRtpServerTimeoutHookVo;
import cn.com.nla.common.video.basic.vo.media.OnSendRtpStoppedHookVo;
import cn.com.nla.common.video.basic.vo.media.OnStreamChangedHookVo;
import cn.com.nla.common.video.basic.vo.media.OnStreamNoneReaderHookVo;
import cn.com.nla.common.video.basic.vo.media.OnStreamNotFoundHookVo;
import cn.com.nla.common.video.core.media.hook.MediaHookServer;
import cn.hutool.json.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * ZLMediaKit 流媒体回调入口，路径与 {@code hook.url} 配置项保持一致。
 *
 * <p>调用方是流媒体服务器而非前端用户，没有登录态，因此 {@code /index/hook/**}
 * 需要在 {@code security.excludes} 中匿名放行；本控制器不挂任何权限注解。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，因为依赖的 {@link MediaHookServer}
 * 由 SIP 自动配置按需创建，协议未开启时该 Bean 不存在。</p>
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
@RestController
@RequestMapping(ZLMediaKitConstant.HOOK_URL_PREFIX)
public class MediaHookController {

    private final MediaHookServer mediaHookServer;

    /**
     * 流媒体心跳回调，刷新媒体服务器在线状态。
     *
     * @param param 回调参数
     * @return ZLMediaKit 约定的响应体
     */
    @PostMapping(ZLMediaKitConstant.MEDIA_HOOK_ON_SERVER_KEEPALIVE)
    public NotNullMap onServerKeepalive(@RequestBody HookVo param) {
        return mediaHookServer.onServerKeepalive(param);
    }

    /**
     * 播放器鉴权事件，rtsp/rtmp/http-flv/ws-flv/hls 播放时触发。
     *
     * @param param 回调参数
     * @return ZLMediaKit 约定的响应体
     */
    @PostMapping(ZLMediaKitConstant.MEDIA_HOOK_ON_PLAY)
    public NotNullMap onPlay(@RequestBody OnPlayHookVo param) {
        return mediaHookServer.onPlay(param);
    }

    /**
     * rtsp/rtmp/rtp 推流鉴权事件。
     *
     * @param param 回调参数
     * @return ZLMediaKit 约定的响应体
     */
    @PostMapping(ZLMediaKitConstant.MEDIA_HOOK_ON_PUBLISH)
    public NotNullMap onPublish(@RequestBody OnPublishHookVo param) {
        return mediaHookServer.onPublish(param);
    }

    /**
     * rtsp/rtmp 流注册或注销事件，对回复内容不敏感。
     *
     * @param param 回调参数
     * @return ZLMediaKit 约定的响应体
     */
    @PostMapping(ZLMediaKitConstant.MEDIA_HOOK_ON_STREAM_CHANGED)
    public NotNullMap onStreamChanged(@RequestBody OnStreamChangedHookVo param) {
        return mediaHookServer.onStreamChanged(param);
    }

    /**
     * 流无人观看事件，用于决定是否关闭无人观看的流。
     *
     * @param param 回调参数
     * @return 异步返回 ZLMediaKit 约定的响应体
     */
    @PostMapping(ZLMediaKitConstant.MEDIA_HOOK_ON_STREAM_NONE_READER)
    public CompletableFuture<NotNullMap> onStreamNoneReader(@RequestBody OnStreamNoneReaderHookVo param) {
        return mediaHookServer.onStreamNoneReader(param);
    }

    /**
     * 流未找到事件，可在此按需触发拉流，对回复内容不敏感。
     *
     * @param param 回调参数
     * @return 异步返回 ZLMediaKit 约定的响应体
     */
    @PostMapping(ZLMediaKitConstant.MEDIA_HOOK_ON_STREAM_NOT_FOUND)
    public CompletableFuture<NotNullMap> onStreamNotFound(@RequestBody OnStreamNotFoundHookVo param) {
        return mediaHookServer.onStreamNotFound(param);
    }

    /**
     * 服务器启动事件，用于感知流媒体重启。
     *
     * @param param   回调参数
     * @param request 当前请求，用于取回调来源 IP
     * @return ZLMediaKit 约定的响应体
     */
    @PostMapping(ZLMediaKitConstant.MEDIA_HOOK_ON_SERVER_STARTED)
    public NotNullMap onServerStarted(@RequestBody JSONObject param, HttpServletRequest request) {
        return mediaHookServer.onServerStarted(request.getRemoteAddr(), param);
    }

    /**
     * 发送 RTP（startSendRtp）被动关闭时的回调。
     *
     * @param param 回调参数
     * @return ZLMediaKit 约定的响应体
     */
    @PostMapping(ZLMediaKitConstant.MEDIA_HOOK_ON_SEND_RTP_STOPPED)
    public NotNullMap onSendRtpStopped(@RequestBody OnSendRtpStoppedHookVo param) {
        return mediaHookServer.onSendRtpStopped(param);
    }

    /**
     * rtpServer 收流超时回调。
     *
     * @param param 回调参数
     * @return ZLMediaKit 约定的响应体
     */
    @PostMapping(ZLMediaKitConstant.MEDIA_HOOK_ON_RTP_SERVER_TIMEOUT)
    public NotNullMap onRtpServerTimeout(@RequestBody OnRtpServerTimeoutHookVo param) {
        return mediaHookServer.onRtpServerTimeout(param);
    }

}
