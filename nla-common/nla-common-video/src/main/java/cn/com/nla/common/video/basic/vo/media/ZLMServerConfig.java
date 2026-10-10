package cn.com.nla.common.video.basic.vo.media;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ZLMServerConfig extends HookVo{

    @Alias(value = "api.apiDebug")
    private String apiDebug;

    @Alias(value = "api.secret")
    private String apiSecret;

    @Alias(value = "api.snapRoot")
    private String apiSnapRoot;

    @Alias(value = "api.defaultSnap")
    private String apiDefaultSnap;

    @Alias(value = "ffmpeg.bin")
    private String ffmpegBin;

    @Alias(value = "ffmpeg.cmd")
    private String ffmpegCmd;

    @Alias(value = "ffmpeg.snap")
    private String ffmpegSnap;

    @Alias(value = "ffmpeg.log")
    private String ffmpegLog;

    @Alias(value = "ffmpeg.restart_sec")
    private String ffmpegRestartSec;

    @Alias(value = "protocol.modify_stamp")
    private String protocolModifyStamp;

    @Alias(value = "protocol.enable_audio")
    private String protocolEnableAudio;

    @Alias(value = "protocol.add_mute_audio")
    private String protocolAddMuteAudio;

    @Alias(value = "protocol.continue_push_ms")
    private String protocolContinuePushMs;

    @Alias(value = "protocol.enable_hls")
    private String protocolEnableHls;

    @Alias(value = "protocol.enable_mp4")
    private String protocolEnableMp4;

    @Alias(value = "protocol.enable_rtsp")
    private String protocolEnableRtsp;

    @Alias(value = "protocol.enable_rtmp")
    private String protocolEnableRtmp;

    @Alias(value = "protocol.enable_ts")
    private String protocolEnableTs;

    @Alias(value = "protocol.enable_fmp4")
    private String protocolEnableFmp4;

    @Alias(value = "protocol.mp4_as_player")
    private String protocolMp4AsPlayer;

    @Alias(value = "protocol.mp4_max_second")
    private String protocolMp4MaxSecond;

    @Alias(value = "protocol.mp4_save_path")
    private String protocolMp4SavePath;

    @Alias(value = "protocol.hls_save_path")
    private String protocolHlsSavePath;

    @Alias(value = "protocol.hls_demand")
    private String protocolHlsDemand;

    @Alias(value = "protocol.rtsp_demand")
    private String protocolRtspDemand;

    @Alias(value = "protocol.rtmp_demand")
    private String protocolRtmpDemand;

    @Alias(value = "protocol.ts_demand")
    private String protocolTsDemand;

    @Alias(value = "protocol.fmp4_demand")
    private String protocolFmp4Demand;

    @Alias(value = "general.enableVhost")
    private String generalEnableVhost;

    @Alias(value = "general.flowThreshold")
    private String generalFlowThreshold;

    @Alias(value = "general.maxStreamWaitMS")
    private String generalMaxStreamWaitMS;

    @Alias(value = "general.streamNoneReaderDelayMS")
    private int generalStreamNoneReaderDelayMS;

    @Alias(value = "general.resetWhenRePlay")
    private String generalResetWhenRePlay;

    @Alias(value = "general.mergeWriteMS")
    private String generalMergeWriteMS;

    @Alias(value = "general.mediaServerId")
    private String generalMediaServerId;

    @Alias(value = "general.wait_track_ready_ms")
    private String generalWaitTrackReadyMs;

    @Alias(value = "general.wait_add_track_ms")
    private String generalWaitAddTrackMs;

    @Alias(value = "general.unready_frame_cache")
    private String generalUnreadyFrameCache;

    @Alias(value = "ip")
    private String ip;
    @Alias(value = "sdpIp")
    private String sdpIp;
    @Alias(value = "streamIp")
    private String streamIp;
    @Alias(value = "hookIp")
    private String hookIp;
    @Alias(value = "updateTime")
    private String updateTime;
    @Alias(value = "createTime")
    private String createTime;

    @Alias(value = "hls.fileBufSize")
    private String hlsFileBufSize;

    @Alias(value = "hls.filePath")
    private String hlsFilePath;

    @Alias(value = "hls.segDur")
    private String hlsSegDur;

    @Alias(value = "hls.segNum")
    private String hlsSegNum;

    @Alias(value = "hls.segRetain")
    private String hlsSegRetain;

    @Alias(value = "hls.broadcastRecordTs")
    private String hlsBroadcastRecordTs;

    @Alias(value = "hls.deleteDelaySec")
    private String hlsDeleteDelaySec;

    @Alias(value = "hls.segKeep")
    private String hlsSegKeep;

    @Alias(value = "hook.access_file_except_hls")
    private String hookAccessFileExceptHLS;

    @Alias(value = "hook.admin_params")
    private String hookAdminParams;

    @Alias(value = "hook.alive_interval")
    private Float hookAliveInterval;

    @Alias(value = "hook.enable")
    private String hookEnable;

    @Alias(value = "hook.on_flow_report")
    private String hookOnFlowReport;

    @Alias(value = "hook.on_http_access")
    private String hookOnHttpAccess;

    @Alias(value = "hook.on_play")
    private String hookOnPlay;

    @Alias(value = "hook.on_publish")
    private String hookOnPublish;

    @Alias(value = "hook.on_record_mp4")
    private String hookOnRecordMp4;

    @Alias(value = "hook.on_rtsp_auth")
    private String hookOnRtspAuth;

    @Alias(value = "hook.on_rtsp_realm")
    private String hookOnRtspRealm;

    @Alias(value = "hook.on_shell_login")
    private String hookOnShellLogin;

    @Alias(value = "hook.on_stream_changed")
    private String hookOnStreamChanged;

    @Alias(value = "hook.on_stream_none_reader")
    private String hookOnStreamNoneReader;

    @Alias(value = "hook.on_stream_not_found")
    private String hookOnStreamNotFound;

    @Alias(value = "hook.on_server_started")
    private String hookOnServerStarted;

    @Alias(value = "hook.on_server_keepalive")
    private String hookOnServerKeepalive;

    @Alias(value = "hook.on_send_rtp_stopped")
    private String hookOnSendRtpStopped;

    @Alias(value = "hook.on_rtp_server_timeout")
    private String hookOnRtpServerTimeout;

    @Alias(value = "hook.timeoutSec")
    private String hookTimeoutSec;

    @Alias(value = "http.charSet")
    private String httpCharSet;

    @Alias(value = "http.keepAliveSecond")
    private String httpKeepAliveSecond;

    @Alias(value = "http.maxReqCount")
    private String httpMaxReqCount;

    @Alias(value = "http.maxReqSize")
    private String httpMaxReqSize;

    @Alias(value = "http.notFound")
    private String httpNotFound;

    @Alias(value = "http.port")
    private int httpPort;

    @Alias(value = "http.rootPath")
    private String httpRootPath;

    @Alias(value = "http.sendBufSize")
    private String httpSendBufSize;

    @Alias(value = "http.sslport")
    private int httpSSLport;

    @Alias(value = "multicast.addrMax")
    private String multicastAddrMax;

    @Alias(value = "multicast.addrMin")
    private String multicastAddrMin;

    @Alias(value = "multicast.udpTTL")
    private String multicastUdpTTL;

    @Alias(value = "record.appName")
    private String recordAppName;

    @Alias(value = "record.filePath")
    private String recordFilePath;

    @Alias(value = "record.fileSecond")
    private String recordFileSecond;

    @Alias(value = "record.sampleMS")
    private String recordFileSampleMS;

    @Alias(value = "rtmp.handshakeSecond")
    private String rtmpHandshakeSecond;

    @Alias(value = "rtmp.keepAliveSecond")
    private String rtmpKeepAliveSecond;

    @Alias(value = "rtmp.modifyStamp")
    private String rtmpModifyStamp;

    @Alias(value = "rtmp.port")
    private int rtmpPort;

    @Alias(value = "rtmp.sslport")
    private int rtmpSslPort;

    @Alias(value = "rtp.audioMtuSize")
    private String rtpAudioMtuSize;

    @Alias(value = "rtp.clearCount")
    private String rtpClearCount;

    @Alias(value = "rtp.cycleMS")
    private String rtpCycleMS;

    @Alias(value = "rtp.maxRtpCount")
    private String rtpMaxRtpCount;

    @Alias(value = "rtp.videoMtuSize")
    private String rtpVideoMtuSize;

    @Alias(value = "rtp_proxy.checkSource")
    private String rtpProxyCheckSource;

    @Alias(value = "rtp_proxy.dumpDir")
    private String rtpProxyDumpDir;

    @Alias(value = "rtp_proxy.port")
    private int rtpProxyPort;

    @Alias(value = "rtp_proxy.port_range")
    private String portRange;

    @Alias(value = "rtp_proxy.timeoutSec")
    private String rtpProxyTimeoutSec;

    @Alias(value = "rtsp.authBasic")
    private String rtspAuthBasic;

    @Alias(value = "rtsp.handshakeSecond")
    private String rtspHandshakeSecond;

    @Alias(value = "rtsp.keepAliveSecond")
    private String rtspKeepAliveSecond;

    @Alias(value = "rtsp.port")
    private int rtspPort;

    @Alias(value = "rtsp.sslport")
    private int rtspSSlport;

    @Alias(value = "shell.maxReqSize")
    private String shellMaxReqSize;

    @Alias(value = "shell.shell")
    private String shellPhell;
    /**
     * 是否需要重启
     */
    private int restart = ConstEnum.Flag.NO.getValue();

}
