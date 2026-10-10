package cn.com.nla.common.video.core.media.client;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.NotNullMap;
import cn.com.nla.common.video.basic.common.ZLMediaKitConstant;
import cn.com.nla.common.video.basic.enums.InviteStreamType;
import cn.com.nla.common.video.basic.enums.ProxyTypeEnum;
import cn.com.nla.common.video.basic.vo.media.*;
import cn.com.nla.common.video.basic.vo.sip.SSRCInfo;
import cn.com.nla.common.video.basic.vo.sip.SendRtp;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.basic.vo.video.StreamProxyVo;
import cn.com.nla.common.video.core.demo.StreamInfo;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.SsrcConfigManager;
import cn.com.nla.common.video.core.redis.subscribe.media.HookKeyFactory;
import cn.com.nla.common.video.core.redis.subscribe.media.MediaHookSubscribe;
import cn.com.nla.common.video.core.service.MediaService;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 获取流媒体相关接口操作
 */
@Slf4j
public class MediaClient {



    public static MediaRestResult getRtpInfo(MediaServerVo mediaServerVo, String streamId){
        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.GET_RTP_INFO,
                new NotNullMap(){{
                    putString("stream_id",streamId);
                }}
        );
    }

    /**
     *
     * @param mediaServerVo 流媒体信息
     * @param ip 推流ip
     * @param port 推流端口
     * @param ssrc 推流唯一标识
     * @param platformId 平台id
     * @param deviceId 设备编号
     * @param channelId  通道id
     * @param app appId
     * @param streamId streamId
     * @param tcp 是否为tcp
     * @param tcpActive 是否为tcp主动模式
     * @param serverId 服务id
     * @param callId 推流ip
     * @param rtcp 是否为RTCP流保活
     * @param type 发送rtp类型
     * @return
     */
    public static SendRtp createSendRtp(
            MediaServerVo mediaServerVo,
            String sessionName,
            long startTime,
            long stopTime,
            String ip,
            int port,
            String ssrc,
            String sipGbId,
            String platformId,//设备国标,上级平台国标,本平台国标
            String deviceId,
            String channelId,
            String app,
            String streamId,
            Boolean tcp,
            Boolean tcpActive,
            String serverId,
            String callId,
            Boolean rtcp,
            InviteStreamType type
    ){
        int localPort = keepPort(mediaServerVo,0,tcp && tcpActive?2:tcp?1:0, streamId);
        if (localPort == 0) {
            return null;
        }
        return SendRtp.builder()
                .sessionName(sessionName)
                .startTime(startTime)
                .stopTime(stopTime)
                .ip(ip)
                .port(port)
                .ssrc(ssrc)
                .sipGbId(sipGbId)
                .platformId(platformId)
                .deviceId(deviceId)
                .channelId(channelId)
                .app(app)
                .streamId(streamId)
                .status(0)
                .tcp(tcp)
                .tcpActive(tcpActive)
                .localPort(localPort)
                .mediaServerId(mediaServerVo.getId())
                .serverId(serverId)
                .callId(callId)
                .fromTag(null)
                .toTag(null)
                .pt(96)
                .usePs(true)
                .onlyAudio(false)
                .rtcp(rtcp)
                .playType(type)
                .build();
    }

    /**
     * 打开RTP服务
     * @param mediaServerVo 流服务信息
     * @param streamId 流编号
     * @param ssrc ssrc
     * @param ssrcCheck 是否检查ssrc
     * @param isPlayback 是否回放流
     * @param port 流端口
     * @return
     */
    public static SSRCInfo openRTPServer(MediaServerVo mediaServerVo, String streamId, String ssrc, boolean ssrcCheck, boolean isPlayback, Integer port, Boolean reUsePort, Integer tcpMode){
        if(mediaServerVo == null || mediaServerVo.getId() == null){
            log.error("[openRTPServer] 失败, mediaServer == null || mediaServer.getId() == null");
            return  null;
        }
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        if(ssrc == null){
            if(isPlayback){
                ssrc = ssrcConfigManager.getPlayBackSsrc(mediaServerVo.getId());
            }else {
                ssrc = ssrcConfigManager.getPlaySsrc(mediaServerVo.getId());
            }
        }
        if(streamId == null){
            streamId = String.format("%08x", Integer.parseInt(ssrc)).toUpperCase();
        }
        int rtpPort;
        if(mediaServerVo.getRtpEnable() == ConstEnum.Flag.YES.getValue()){
            rtpPort = createRTPServer(mediaServerVo,streamId,ssrcCheck?Integer.parseInt(ssrc):0,port,reUsePort,tcpMode);
        }else {
           rtpPort = mediaServerVo.getRtpProxyPort();
        }

        return new SSRCInfo(rtpPort,ssrc,streamId);
    }

    public static StreamInfo getStreamInfo(MediaServerVo mediaServerVo, String app, String stream, String addr,String callId){
        if (mediaServerVo == null) {
            return null;
        }
        OnStreamChangedResult result = getMediaInfo(mediaServerVo, "__defaultVhost__", "rtsp", app, stream);
        if(result == null || result.getCode() !=RespCode.CODE_0.getValue()){
            return null;
        }
        return new StreamInfo(mediaServerVo,app,stream,result.getTracks(),addr,callId,null,null);
    }


    /**
     * 创建RTP流服务
     * @param mediaServerVo 流媒体信息
     * @param streamId 流编号
     * @param ssrc ssrc
     * @param port 端口
     * @return
     */
    public static int createRTPServer(MediaServerVo mediaServerVo, String streamId, int ssrc, Integer port, Boolean reUsePort, Integer tcpMode){
        int result = -1;
        //1.检测此streamId 是否已开启
        MediaRestResult mediaRestResult = MediaUtils.request(mediaServerVo, ZLMediaKitConstant.GET_RTP_INFO,
                new NotNullMap() {{
                    putString("stream_id", streamId);
                }}
        );
        if(mediaRestResult.getCode() == RespCode.CODE_0.getValue()){
            //关闭此流重新开启
            closeRtpServer(mediaServerVo,streamId);
        }
        if (tcpMode == null) {
            tcpMode = 0;
        }
        Integer finalTcpMode = tcpMode;
        NotNullMap map = new NotNullMap() {{
            putInteger("tcp_mode", finalTcpMode);
            putString("stream_id", streamId);
            putInteger("port", port == null ? 0 : port);
        }};
        if (ssrc != 0) {
            map.putInteger("ssrc", ssrc);
        }
        if (reUsePort != null) {
            map.putString("re_use_port", reUsePort?"1":"0");
        }
        MediaRestResult request = MediaUtils.request(mediaServerVo, ZLMediaKitConstant.OPEN_RTP_SERVER, map);
        if(request == null){
            log.error("创建RTP Server 失败 {}: 请检查ZLM服务", port);
        }else if(request.getCode() != 0){
            log.error("创建RTP Server 失败 {}: ", request.getMsg());
        }else {
            result = request.getPort();
        }
        return result;
    }


    /**
     * 开始发送rtp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     * @param ssrc rtp推流的ssrc，ssrc不同时，可以推流到多个上级服务器
     * @param dstUrl 目标ip或域名
     * @param dstPort 目标端口
     * @param isUdp 是否为udp模式,否则为tcp模式
     * @param srcPort 使用的本机端口，为0或不传时默认为随机端口
     * @param pt 发送时，rtp的pt（uint8_t）,不传时默认为96
     * @param usePs 发送时，rtp的负载类型。为1时，负载为ps；为0时，为es；不传时默认为1
     * @param onlyAudio 当use_ps 为0时，有效。为1时，发送音频；为0时，发送视频；不传时默认为0
     */
    public static MediaRestResult startSendRtp(MediaServerVo mediaServerVo, String vhost, String app, String stream, String ssrc, String dstUrl, Integer dstPort, String isUdp, Integer srcPort, Integer pt, Integer usePs, Integer onlyAudio,Integer isRtcp){
        NotNullMap map = new NotNullMap() {{
            putString("vhost", vhost);
            putString("app", app);
            putString("stream", stream);
            putString("ssrc", ssrc);
            putString("dst_url", dstUrl);
            putInteger("dst_port", dstPort);
            putString("is_udp", isUdp);
            putInteger("src_port", srcPort);
            putInteger("pt", pt);
            putInteger("use_ps", usePs);
            putInteger("only_audio", onlyAudio);
        }};
        if(isRtcp != null){
            map.putString("udp_rtcp_timeout", isRtcp>1?"500":"0");
        }

        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.START_SEND_RTP, map);
    }


    /**
     * 暂停RTP超时检查
     * @param streamId 该端口绑定的流id
     */
    public static MediaRestResult pauseRtpCheck(MediaServerVo mediaServerVo, String streamId){
        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.PAUSE_RTP_CHECK,
                new NotNullMap(){{
                    putString("stream_id",streamId);
                }}
        );
    }

    /**
     * 恢复RTP超时检查
     * @param streamId 该端口绑定的流id
     */
    public static MediaRestResult resumeRtpCheck(MediaServerVo mediaServerVo, String streamId){
        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.RESUME_RTP_CHECK,
                new NotNullMap(){{
                    putString("stream_id",streamId);
                }}
        );
    }

    /**
     * 主动链接RTP服务器
     */
    public static MediaRestResult connectRtpServer(MediaServerVo mediaServerVo, String dstUrl, int dstPort, String streamId){
        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.CONNECT_RTP_SERVER,
                new NotNullMap(){{
                    putString("dst_url",dstUrl);
                    putInteger("dst_port",dstPort);
                    putString("stream_id",streamId);
                }}
        );
    }

    /**
     * 修改RTP SSRC信息
     */
    public static MediaRestResult updateRtpServerSsrc(MediaServerVo mediaServerVo ,String streamId, String ssrc){
        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.UPDATE_RTP_SERVER_SSRC,
                new NotNullMap(){{
                    putString("stream_id",streamId);
                    putString("ssrc",ssrc);
                }}
        );
    }
    /**
     * 获取流列表
     * @param schema 协议，例如 rtsp或rtmp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     */
    public static MediaRestResult getMediaList(MediaServerVo mediaServerVo, String vhost, String schema, String app, String stream){
        NotNullMap map = new NotNullMap() {{
            putString("app", app);
            putString("stream", stream);
        }};
        if(StringUtils.isNotEmpty(vhost)){
            map.putString("vhost",vhost);
        }
        if(StringUtils.isNotEmpty(schema)){
            map.putString("schema", schema);
        }
        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.GET_MEDIA_LIST, map);
    }

    /**
     * 获取流信息
     * @param schema 协议，例如 rtsp或rtmp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     */
    public static OnStreamChangedResult getMediaInfo(MediaServerVo mediaServerVo, String vhost, String schema, String app, String stream){
        NotNullMap map = new NotNullMap() {{
            putString("app", app);
            putString("stream", stream);
            putString("vhost",StringUtils.isNotEmpty(vhost)?vhost:"__defaultVhost__");
            putString("schema", StringUtils.isNotEmpty(vhost)?schema:"rtsp");
        }};
        return MediaUtils.requestStreamChanged(mediaServerVo,ZLMediaKitConstant.GET_MEDIA_INFO, map);
    }

    /**
     * 删除录像文件夹
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     * @param period 流的录像日期，格式为2020-02-01,如果不是完整的日期，那么是搜索录像文件夹列表，否则搜索对应日期下的mp4文件列表
     */
    public static MediaRestResult deleteRecordDirectory(MediaServerVo mediaServerVo, String vhost,String app, String stream,  String period){
        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.DELETE_RECORD_DIRECTORY,
                new NotNullMap(){{
                    putString("vhost",vhost);
                    putString("app",app);
                    putString("stream",stream);
                    putString("period",period);
                }}
        );
    }
    /**
     * 停止 发送rtp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     * @param ssrc 根据ssrc关停某路rtp推流，置空时关闭所有流
     */
    public static MediaRestResult stopSendRtp(MediaServerVo mediaServerVo, String vhost, String app, String stream, String ssrc){
        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.STOP_SEND_RTP,
                new NotNullMap(){{
                    putString("vhost",vhost);
                    putString("app",app);
                    putString("stream",stream);
                    putString("ssrc",ssrc);
                }}
        );
    }

    /**
     * 关闭流
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     */
    public static MediaRestResult closeStreams(MediaServerVo mediaServerVo, String vhost, String app, String stream){
        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.CLOSE_STREAMS,
                new NotNullMap(){{
                    putString("vhost",vhost);
                    putString("app",app);
                    putString("stream",stream);
                    putInteger("force",1);
                }}
        );
    }

    /**
     * 关闭RTP服务器
     * @param streamId 该端口绑定的流id
     */
    public static MediaRestResult closeRtpServer(MediaServerVo mediaServerVo, String streamId){
        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.CLOSE_RTP_SERVER,
                new NotNullMap(){{
                    putString("stream_id",streamId);
                }}
        );
    }

    /**
     * 流媒体开启服务时如长时间未接收到数据端口会自动关闭
     * 此方法为轮训检测如端口关闭则重新打开
     * @param port 绑定的端口，0时为随机端口
     * @param tcpMode tcp模式，0时为不启用tcp监听，1时为启用tcp监听，2时为tcp主动连接模式
     * @param streamId 该端口绑定的流id
     */
    private static int keepPort(MediaServerVo mediaServerVo, int port, int tcpMode, String streamId){
        MediaHookSubscribe mediaHookSubscribe = MediaService.getMediaHookSubscribe();
        int localPort = port;

        MediaRestResult request = MediaUtils.request(mediaServerVo, ZLMediaKitConstant.OPEN_RTP_SERVER,
                new NotNullMap(){{
                    putInteger("port",port);
                    putInteger("tcp_mode",tcpMode);
                    putString("stream_id",streamId);
                }}
        );
        if(request.getCode() == 0){
            HookKey hookKey = HookKeyFactory.onRtpServerTimeout(streamId, mediaServerVo.getId());
            localPort = request.getPort();
            int finalLocalPort = localPort;
            mediaHookSubscribe.addSubscribe(hookKey,(MediaServerVo server, HookVo response)->{
                if (!(response instanceof OnRtpServerTimeoutHookVo)){
                    log.error("收流超时,消息类型错误 response is not OnRtpServerTimeoutHookVo");
                }else {
                    OnRtpServerTimeoutHookVo vo = (OnRtpServerTimeoutHookVo) response;
                    if(streamId.equals(vo.getStream_id())){
                        log.warn("收流超时事件触发，重新创建RTP服务器 {}-->端口：{}",streamId, finalLocalPort);
                        int pt = keepPort(mediaServerVo, finalLocalPort, tcpMode, streamId);
                        if (port == 0) {
                            log.info("[上级点播] {}->监听端口失败，移除监听", streamId);
                            mediaHookSubscribe.removeSubscribe(hookKey);
                        }
                    }
                }
            });
            log.info("[上级点播] {}->监听端口: {}", streamId, localPort);
        }else {
            log.info("[上级点播] 监听端口失败: {}", streamId);
        }
        return localPort;
    }


    /**
     * 创建拉流
     * @param mediaServerVo
     * @param streamProxyVo
     * @return
     */
    public static MediaRestResult addStreamProxyToZlm(MediaServerVo mediaServerVo, StreamProxyVo streamProxyVo){
        if(streamProxyVo == null){
            return null;
        }

        if(streamProxyVo.getType()== ProxyTypeEnum.DEFAULT.getValue()){
            return  MediaUtils.request(mediaServerVo,ZLMediaKitConstant.ADD_STREAM_PROXY,new NotNullMap(){{
                putString("vhost","__defaultVhost__");
                putString("app", streamProxyVo.getApp());
                putString("stream", streamProxyVo.getStream());
                putString("url", streamProxyVo.getUrl());
                put("enable_mp4", streamProxyVo.getEnableMp4() == ConstEnum.Flag.YES.getValue());
                put("enable_audio", streamProxyVo.getEnableAudio() == ConstEnum.Flag.YES.getValue());
                putInteger("rtp_type", streamProxyVo.getRtpType());
            }});
        }else {
          return  MediaUtils.request(mediaServerVo,ZLMediaKitConstant.ADD_FFMPEG_SOURCE,new NotNullMap(){{
                putString("src_url", streamProxyVo.getSrcUrl());
                putString("dst_url", streamProxyVo.getDstUrl());
                putInteger("timeout_ms", streamProxyVo.getTimeoutMs());
                put("enable_mp4", streamProxyVo.getEnableMp4() == ConstEnum.Flag.YES.getValue());
                putString("ffmpeg_cmd_key", StringUtils.isNotEmpty(streamProxyVo.getFfmpegCmdKey())?URLUtil.encode(streamProxyVo.getFfmpegCmdKey()):null);
            }});
        }
    }


    /**
     * 关闭FFmpeg拉流代理
     * @param key addFFmpegSource接口返回的key
     */
    public static MediaRestResult delFfmpegSource(MediaServerVo mediaServerVo, String key){
        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.DEL_FFMPEG_SOURCE,
                new NotNullMap(){{
                    putString("key",key);
                }}
        );
    }

    /**
     * 对zlm服务器进行基础配置
     * @param mediaServerVo 服务ID
     * @param restart 是否重启zlm
     */
    public static void zlmConfigAuto(MediaServerVo mediaServerVo, boolean restart){
        String hookUrl = mediaServerVo.getHookIp();
        if(!mediaServerVo.getHookIp().startsWith("http")){
            hookUrl = String.format("%s://%s", mediaServerVo.getSslStatus()== ConstEnum.Flag.YES.getValue()?"https":"http", mediaServerVo.getHookIp());
        }
        String finalHookUrl = hookUrl;
        NotNullMap map = new NotNullMap() {{
            putString("api.secret", mediaServerVo.getSecret());
            if (mediaServerVo.getRtspPort() != 0) {
                putString("ffmpeg.snap", URLUtil.encode("%s -rtsp_transport tcp -i %s -y -f mjpeg -frames:v 1 %s", CharsetUtil.CHARSET_UTF_8));
            }
            putString("rtc.externIP", mediaServerVo.getSdpIp());
            putInteger("hook.enable", 1);
            putInteger("hook.alive_interval", Math.max(mediaServerVo.getHookAliveInterval(),10));
            putString("hook.on_flow_report","");
            putString("hook.on_play", String.format("%s%s/%s", finalHookUrl,ZLMediaKitConstant.HOOK_URL_PREFIX,ZLMediaKitConstant.MEDIA_HOOK_ON_PLAY));
            putString("hook.on_http_access","");
            putString("hook.on_publish",String.format("%s%s/%s", finalHookUrl,ZLMediaKitConstant.HOOK_URL_PREFIX,ZLMediaKitConstant.MEDIA_HOOK_ON_PUBLISH));
            putString("hook.on_record_ts","");
            putString("hook.on_rtsp_auth","");
            putString("hook.on_rtsp_realm","");
            putString("hook.on_server_started",String.format("%s%s/%s", finalHookUrl,ZLMediaKitConstant.HOOK_URL_PREFIX,ZLMediaKitConstant.MEDIA_HOOK_ON_SERVER_STARTED));
            putString("hook.on_shell_login","");
            putString("hook.on_stream_changed",String.format("%s%s/%s", finalHookUrl,ZLMediaKitConstant.HOOK_URL_PREFIX,ZLMediaKitConstant.MEDIA_HOOK_ON_STREAM_CHANGED));
            putString("hook.on_stream_none_reader",String.format("%s%s/%s", finalHookUrl,ZLMediaKitConstant.HOOK_URL_PREFIX,ZLMediaKitConstant.MEDIA_HOOK_ON_STREAM_NONE_READER));
            putString("hook.on_stream_not_found",String.format("%s%s/%s", finalHookUrl,ZLMediaKitConstant.HOOK_URL_PREFIX,ZLMediaKitConstant.MEDIA_HOOK_ON_STREAM_NOT_FOUND));
            putString("hook.on_server_keepalive",String.format("%s%s/%s", finalHookUrl,ZLMediaKitConstant.HOOK_URL_PREFIX,ZLMediaKitConstant.MEDIA_HOOK_ON_SERVER_KEEPALIVE));
            putString("hook.on_send_rtp_stopped",String.format("%s%s/%s", finalHookUrl,ZLMediaKitConstant.HOOK_URL_PREFIX,ZLMediaKitConstant.MEDIA_HOOK_ON_SEND_RTP_STOPPED));
            putString("hook.on_rtp_server_timeout",String.format("%s%s/%s", finalHookUrl,ZLMediaKitConstant.HOOK_URL_PREFIX,ZLMediaKitConstant.MEDIA_HOOK_ON_RTP_SERVER_TIMEOUT));
            putString("hook.on_record_mp4", mediaServerVo.getRecordAssistPort() <= 0 ? "" : String.format("http://127.0.0.1:%s/%s/%s",mediaServerVo.getRecordAssistPort(),ZLMediaKitConstant.HOOK_URL_PREFIX,ZLMediaKitConstant.MEDIA_HOOK_ON_RECORD_MP4));
            //事件触发http post超时时间
            putString("hook.timeout_sec","20");
            //断连续推延时，单位毫秒，置空使用配置文件默认值
            putString("protocol.continue_push_ms", "3000" );
        }};
        if (mediaServerVo.getRtpEnable() == ConstEnum.Flag.YES.getValue() && !ObjectUtils.isEmpty(mediaServerVo.getRtpPortRange())) {
            map.putString("rtp_proxy.port_range", mediaServerVo.getRtpPortRange().replace(",", "-"));
        }

        MediaRestResult request = MediaUtils.request(mediaServerVo, ZLMediaKitConstant.SET_SERVER_CONFIG, map);
        if(request == null ||request.getCode() != 0){
            log.error("[ZLM] 设置zlm失败 {} -> {}", mediaServerVo.getId(), mediaServerVo.getIp());
        }else if(restart){
            log.info("[ZLM] 设置成功,开始重启以保证配置生效 {} -> {}", mediaServerVo.getId(), mediaServerVo.getIp());
            MediaUtils.request(mediaServerVo,ZLMediaKitConstant.RESTART_SERVER);
        }else {
            log.info("[ZLM] 设置成功 {} -> {}", mediaServerVo.getId(), mediaServerVo.getIp());
        }
    }

    public static ZLMServerConfig getZLMServerConfig(MediaServerVo mediaServerVo){
        MediaRestResult result = MediaClient.getZlmConfig(mediaServerVo);
        if(result == null || result.getCode() != 0){
            log.error("[ {} ]-[ {}:{} ]主动连接失败 ", mediaServerVo.getId(), mediaServerVo.getIp(), mediaServerVo.getHttpPort());
            return null;
        }
        List<ZLMServerConfig> zlmServerConfigList = JSONUtil.toList(JSONUtil.toJsonStr(result.getData()), ZLMServerConfig.class);
        if(zlmServerConfigList.isEmpty()){
            log.info("[ {} ]-[ {}:{} ]主动连接失败, 清理相关资源", mediaServerVo.getId(), mediaServerVo.getIp(), mediaServerVo.getHttpPort());
            return null;
        }
        return zlmServerConfigList.get(0);
    }


    public static MediaRestResult getZlmConfig(MediaServerVo mediaServerVo){
        return MediaUtils.request(mediaServerVo,ZLMediaKitConstant.GET_SERVER_CONFIG);
    }


    public static void startSendRtpStreamForPassive(MediaServerVo mediaServerVo,SendRtp sendRtp,int localPort){
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost","__defaultVhost__");
        param.put("app",sendRtp.getApp());
        param.put("stream",sendRtp.getStreamId());
        param.put("ssrc", sendRtp.getSsrc());
        if (!sendRtp.isTcpActive()) {
            param.put("dst_url",sendRtp.getIp());
            param.put("dst_port", sendRtp.getPort());
        }
        param.put("is_udp", sendRtp.isTcp() ? "0" : "1");
        param.put("src_port", localPort);
        param.put("pt", sendRtp.getPt());
        param.put("use_ps", sendRtp.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtp.isOnlyAudio() ? "1" : "0");
        if (!sendRtp.isTcp()) {
            // 开启rtcp保活
            param.put("udp_rtcp_timeout", sendRtp.isRtcp()? "500":"0");
        }
        MediaRestResult request = MediaUtils.request(mediaServerVo, ZLMediaKitConstant.START_SEND_RTP_PASSIVE, param);
        if(request == null){
            log.error("下级TCP被动启动监听失败: 请检查ZLM服务");
        }else if (request.getCode() == RespCode.CODE_0.getValue()) {
            log.info("启动监听TCP被动推流成功[ {}/{} ]，{}->{}:{}, 结果： {}, " ,param.get("app"), param.get("stream"), request.getLocal_port(), param.get("dst_url"), param.get("dst_port"),JSONUtil.toJsonStr(request));
        } else {
            log.error("启动监听TCP被动推流失败: {}, 参数：{}",request.getMsg(), JSONUtil.toJsonStr(param));
        }
    }


}
