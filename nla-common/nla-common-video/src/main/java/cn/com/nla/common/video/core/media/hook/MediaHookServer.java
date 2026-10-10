package cn.com.nla.common.video.core.media.hook;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.model.NotNullMap;
import cn.com.nla.common.video.basic.common.CatalogEventConstant;
import cn.com.nla.common.video.basic.enums.HookType;
import cn.com.nla.common.video.basic.enums.OriginType;
import cn.com.nla.common.video.basic.enums.VideoStreamType;
import cn.com.nla.common.video.basic.exception.SsrcTransactionNotFoundException;
import cn.com.nla.common.video.basic.vo.media.*;
import cn.com.nla.common.video.basic.vo.sip.SendRtp;
import cn.com.nla.common.video.basic.vo.video.*;
import cn.com.nla.common.video.core.demo.*;
import cn.com.nla.common.video.core.media.client.ZlmService;
import cn.com.nla.common.video.core.properties.VideoProperties;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.*;
import cn.com.nla.common.video.core.redis.subscribe.media.HookEvent;
import cn.com.nla.common.video.core.redis.subscribe.media.MediaHookSubscribe;
import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;
import cn.com.nla.common.video.core.service.MediaService;
import cn.com.nla.common.video.core.service.PlayService;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.authentication.TokenService;
import cn.com.nla.common.video.core.service.video.*;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.common.video.core.sip.cmd.SIPCommanderForPlatform;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.*;

/**
 * 流媒体服务回调事件
 */
@Slf4j
public class MediaHookServer {

    @Autowired
    protected SipServer sipServer;
    @Autowired
    private MediaHookSubscribe mediaHookSubscribe;
    @Autowired
    private VideoProperties videoProperties;
    @Autowired
    private SIPCommander sipCommander;
    @Autowired
    private SIPCommanderForPlatform sipCommanderForPlatform;

    /**
     * 流媒体心跳回调
     * @param hookVo
     */
    public NotNullMap onServerKeepalive(HookVo hookVo){
        log.info("[ZLM HOOK] 收到zlm心跳：" + hookVo.getMediaServerId());
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        String mediaServerId = hookVo.getMediaServerId();
        MediaServerVo mediaServerVo = mediaServerVoService.findMediaServerId(mediaServerId);
        ThreadUtil.execute(()->{
            if(mediaServerVo != null){
                mediaHookSubscribe.sendNotify(MediaHookVo.builder().type(HookType.on_server_keepalive).onAll(ConstEnum.Flag.NO.getValue()).mediaServerVo(mediaServerVo).hookVo(hookVo).build());
            }
        });
        return new NotNullMap(){{putInteger("code",0);putString("msg","success");}};
    }

    /**
     * 播放器鉴权事件，rtsp/rtmp/http-flv/ws-flv/hls的播放都将触发此鉴权事件。
     * @param hookVo
     */
    public NotNullMap onPlay(OnPlayHookVo hookVo){
        log.debug("[ZLM HOOK] 播放鉴权：{}->{}" + hookVo.getMediaServerId(), hookVo);
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        TokenService tokenService = MediaService.getTokenService();

        String mediaServerId = hookVo.getMediaServerId();
        MediaServerVo mediaServerVo = mediaServerVoService.findOnLineMediaServerId(mediaServerId);
        if (hookVo.getParams() == null) {
            log.info("播放鉴权失败： 缺少不要参数：sign=md5(user表的pushKey)");
            return  new NotNullMap(){{putInteger("code",401);putString("msg","Unauthorized");}};
        }
        Map<String, String> paramMap = HttpUtil.decodeParamMap(hookVo.getParams(), CharsetUtil.CHARSET_UTF_8);
        String token = paramMap.get("token");
        if (token == null) {
            log.info("播放鉴权失败： 缺少必要参数：token");
            return  new NotNullMap(){{putInteger("code",401);putString("msg","Unauthorized");}};
        }
        boolean authentication = tokenService.authentication(token);
        if(! authentication){
            log.info("播放鉴权失败： 用户 无权限:  token={}", token);
            return  new NotNullMap(){{putInteger("code",401);putString("msg","Unauthorized");}};
        }
        ThreadUtil.execute(()->{
            if(mediaServerVo != null){
                mediaHookSubscribe.sendNotify(MediaHookVo.builder().type(HookType.on_play).onAll(ConstEnum.Flag.NO.getValue()).mediaServerVo(mediaServerVo).hookVo(hookVo).build());
            }
        });
        return new NotNullMap(){{putInteger("code",0);putString("msg","success");}};
    }

    /**
     * rtsp/rtmp/rtp推流鉴权事件。
     * @param hookVo
     */
    public NotNullMap onPublish(OnPublishHookVo hookVo){
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        StreamProxyVoService streamProxyService = VideoService.getStreamProxyService();
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        TokenService tokenService = MediaService.getTokenService();
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();

        String mediaServerId = hookVo.getMediaServerId();
        MediaServerVo mediaServerVo = mediaServerVoService.findOnLineMediaServerId(mediaServerId);
        NotNullMap map = new NotNullMap();
        map.putInteger("code",0);
        map.putString("msg","success");
        map.putInteger("mp4_max_second",0);
        boolean enableAudio = false;
        boolean enableMp4 = false;
        if(!"rtp".equals(hookVo.getApp())){
            //是否开启鉴权
            if(videoProperties.getPushAuthority()){
                if (hookVo.getParams() == null) {
                    log.info("推流鉴权失败： 缺少不要参数：sign=md5(user表的pushKey)");
                    return  new NotNullMap(){{putInteger("code",401);putString("msg","Unauthorized");}};
                }
                Map<String, String> paramMap = HttpUtil.decodeParamMap(hookVo.getParams(), CharsetUtil.CHARSET_UTF_8);
                String token = paramMap.get("token");
                if (token == null) {
                    log.info("推流鉴权失败： 缺少必要参数：token");
                    return  new NotNullMap(){{putInteger("code",401);putString("msg","Unauthorized");}};
                }
                boolean authentication = tokenService.authentication(token);
                if(! authentication){
                    log.info("推流鉴权失败： 用户 无权限:  token={}", token);
                    return  new NotNullMap(){{putInteger("code",401);putString("msg","Unauthorized");}};
                }
            }
            StreamProxyVo appStream = streamProxyService.findAppStream(hookVo.getApp(), hookVo.getStream());
            if(appStream != null){
                enableAudio = Objects.equals(appStream.getEnableAudio(),ConstEnum.Flag.YES.getValue());
                enableMp4 = Objects.equals(appStream.getEnableMp4(),ConstEnum.Flag.YES.getValue());
            }else {
                enableAudio = true;
                enableMp4 = videoProperties.getRecordPushLive();
            }
        }else {
            enableMp4 = videoProperties.getRecordSip();
            // 替换流地址
            if(mediaServerVo.getRtpEnable() == ConstEnum.Flag.NO.getValue()){
                String ssrc = String.format("%010d", Long.parseLong(hookVo.getStream(), 16));;
                InviteInfo inviteInfo = inviteStreamManager.getInviteInfoBySSRC(ssrc);
                if (inviteInfo != null) {
                    log.info("[ZLM HOOK]推流鉴权 stream: {} 替换为 {}", hookVo.getStream(), inviteInfo.getStream());
                    map.put("stream_replace",inviteInfo.getStream());
                    hookVo.setStream(inviteInfo.getStream());
                }
            }
        }
        SsrcTransaction ssrcTransaction = ssrcTransactionManager.getParamOne(null, null, null,hookVo.getStream(),null);
        if(ssrcTransaction != null){
            DeviceChannelVo deviceChannelVo = deviceChannelVoService.findDeviceIdChannelId(ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId());
            if(deviceChannelVo !=null){
                enableAudio = deviceChannelVo.getHasAudio() == ConstEnum.Flag.YES.getValue();
                enableMp4 = deviceChannelVo.getHasRecord() == ConstEnum.Flag.YES.getValue();
            }
            // 如果是录像下载就设置视频间隔十秒
            if(ssrcTransaction.getType() == VideoStreamType.download){
                enableAudio = true;
                enableMp4 = true;
                map.putInteger("mp4_max_second",10);
            }
        }
        //有录像
        if(enableMp4){

        }
        //发送推流事件处理
        ThreadUtil.execute(()->{
            if(mediaServerVo != null){
                mediaHookSubscribe.sendNotify(MediaHookVo.builder().type(HookType.on_publish).onAll(ConstEnum.Flag.NO.getValue()).mediaServerVo(mediaServerVo).hookVo(hookVo).build());
            }
        });
        map.put("enable_audio",enableAudio);
        map.put("enable_mp4",enableMp4);
        log.info("[ZLM HOOK]推流鉴权 响应：{}->{}->>>>{}", hookVo.getMediaServerId(), JSONUtil.toJsonStr(hookVo), JSONUtil.toJsonStr(map));
        return map;
    }

    /**
     * rtsp/rtmp流注册或注销时触发此事件；此事件对回复不敏感。
     * @param hookVo
     */
    public NotNullMap onStreamChanged(OnStreamChangedHookVo hookVo){
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        StreamProxyVoService streamProxyVoService = VideoService.getStreamProxyService();
        GbStreamVoService gbStreamVoService = VideoService.getGbStreamService();
        StreamPushVoService streamPushVoService = VideoService.getStreamPushService();
        SendRtpManager sendRtpManager = RedisService.getSendRtpManager();
        MediaServerManager mediaServerManager = RedisService.getMediaServerManager();
        PlatformCatalogVoService platformCatalogVoService = VideoService.getPlatformCatalogService();
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();
        StreamChangedManager streamChangedManager = RedisService.getStreamChangedManager();
        DeferredResultHolder deferredResultHolder = SpringUtil.getBean(DeferredResultHolder.class);
        if(hookVo.isRegist()){
            log.info("[ZLM HOOK] 流注册, {}->{}->{}/{}", hookVo.getMediaServerId(), hookVo.getSchema(), hookVo.getApp(), hookVo.getStream());
        }else {
            log.info("[ZLM HOOK] 流注销, {}->{}->{}/{}", hookVo.getMediaServerId(), hookVo.getSchema(), hookVo.getApp(), hookVo.getStream());
        }
        MediaServerVo mediaServerVo = mediaServerVoService.findOnLineMediaServerId(hookVo.getMediaServerId());
        if (mediaServerVo == null) {
            log.info("[ZLM HOOK] 流变化未找到ZLM, {}", hookVo.getMediaServerId());
            return new NotNullMap(){{putInteger("code",1);putString("msg","zlm is null");}};
        }
        ThreadUtil.execute(()->{
            //维护数据
            if("rtsp".equals(hookVo.getSchema())){
                if(hookVo.isRegist()){
                    mediaServerManager.addCount(hookVo.getMediaServerId());
                    streamChangedManager.put(hookVo);
                }else {
                    mediaServerManager.removeCount(hookVo.getMediaServerId());
                    streamChangedManager.remove(hookVo);
                }
                mediaHookSubscribe.sendNotify(MediaHookVo.builder().type(HookType.on_stream_changed).onAll(ConstEnum.Flag.NO.getValue()).mediaServerVo(mediaServerVo).hookVo(hookVo).build());
                //设备给流媒体推流时，设置拉流代理上线/离线
                streamProxyVoService.updateStatus(hookVo.getApp(),hookVo.getStream(),null,hookVo.isRegist());
                String type = OriginType.values()[hookVo.getOriginType()].getType();
                if("rtp".equals(hookVo.getApp())){
                    InviteInfo inviteInfo = inviteStreamManager.getInviteInfoByStream(null, hookVo.getStream());
                    if(inviteInfo != null && (inviteInfo.getType() == VideoStreamType.play || inviteInfo.getType() == VideoStreamType.playback)){
                        if(hookVo.isRegist()){
                            deviceChannelVoService.startPlay(inviteInfo.getDeviceId(), inviteInfo.getChannelId(),inviteInfo.getStream());
                        }else {
                            //设备播放流
                            deviceChannelVoService.stopPlay(inviteInfo.getDeviceId(), inviteInfo.getChannelId());
                            inviteStreamManager.removeInviteInfo(inviteInfo);
                            String key = String.format("%s%s_%s",DeferredResultHolder.CALLBACK_CMD_STOP,inviteInfo.getDeviceId(), inviteInfo.getChannelId());
                            deferredResultHolder.invokeAllResult(key, ProtocolResult.result(RespCode.CODE_0.getValue(),"停止点播成功"));
                        }
                    }
                }else {
                    if(hookVo.isRegist()){
                        if("PUSH".equals(type)){
                            streamPushVoService.addPush(hookVo);
                        }
                    }else {
                        streamPushVoService.removePush(hookVo.getApp(),hookVo.getStream());
                    }
                    StreamPushVo streamPushVo = streamPushVoService.findAppStream(hookVo.getApp(), hookVo.getStream());
                    if(streamPushVo != null){
                        streamPushVo.setPushIng(hookVo.isRegist()? ConstEnum.Flag.YES.getValue() :ConstEnum.Flag.NO.getValue());
                        streamPushVo.setPushTime(java.time.LocalDateTime.now());
                        streamPushVoService.updateStreamPush(streamPushVo);
                    }
                    GbStreamVo gbStreamVo = gbStreamVoService.findAppStream(hookVo.getApp(), hookVo.getStream());
                    if(gbStreamVo != null && videoProperties.getUsePushingAsStatus()){
                        platformCatalogVoService.handleCatalogEvent(hookVo.isRegist()? CatalogEventConstant.ON:CatalogEventConstant.OFF,null,null, Collections.singletonList(gbStreamVo),null);
                    }
                }
                //流注销时触发
                if(!hookVo.isRegist()){
                    List<SendRtp> sendRtpList = sendRtpManager.querySendRTPServerByStream(hookVo.getStream());
                    for (SendRtp sendRtp : sendRtpList) {
                        // 设备编号 或 上级平台
                        ParentPlatformVo parentPlatformVo = parentPlatformVoService.getParentPlatformByServerGbId(sendRtp.getPlatform());
                        DeviceVo deviceVo = deviceVoService.findDeviceGbId(sendRtp.getPlatform());
                        try {
                            if(parentPlatformVo != null){
                                sipCommanderForPlatform.streamByeCmd(sipServer, parentPlatformVo,sendRtp,null,null);
                            }else {
                                sipCommander.streamByeCmd(sipServer, deviceVo,sendRtp.getChannelId(),sendRtp.getStreamId(),sendRtp.getCallId(),null,null,null);
                            }
                            sendRtpManager.deleteSendRTPServer(sendRtp.getPlatformId(),sendRtp.getChannelId(),sendRtp.getStreamId(),sendRtp.getCallId());
                        }catch (Exception e){
                            log.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                        }
                    }
                }
            }
        });
        return new NotNullMap(){{putInteger("code",0);putString("msg","success");}};
    }

    /**
     * 流无人观看时事件，用户可以通过此事件选择是否关闭无人看的流。
     *
     */
    public CompletableFuture<NotNullMap> onStreamNoneReader(OnStreamNoneReaderHookVo hookVo){
        log.info("[ZLM HOOK]流无人观看：{}->{}->{}/{}" ,hookVo.getMediaServerId(), hookVo.getSchema(), hookVo.getApp(), hookVo.getStream());
        SendRtpManager sendRtpManager = RedisService.getSendRtpManager();
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();
        DeferredResultHolder deferredResultHolder = SpringUtil.getBean(DeferredResultHolder.class);
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        StreamProxyVoService streamProxyVoService = VideoService.getStreamProxyService();
        VideoRestResult<NotNullMap> deferredResult = new VideoRestResult<>(videoProperties.getPlayTimeout()*1000L,()-> new NotNullMap(){{putInteger("code",0);putString("msg","success");}});
        String key = String.format("%s%s",DeferredResultHolder.CALLBACK_STREAM_NONE_READER,hookVo.getStream());
        String uuid = RandomUtil.randomString(32);
        deferredResultHolder.put(key,uuid,deferredResult);

        NotNullMap map = new NotNullMap();
        map.putInteger("code",0);
        map.putString("msg","success");
        if("rtp".equals(hookVo.getApp())){
            map.put("close",videoProperties.getStreamOnDemand());
            //获取国标流 ， 点播/录像回放/录像下载
            //点播
            InviteInfo inviteInfo = inviteStreamManager.getInviteInfoByStream(null, hookVo.getStream());
            log.info("无人观看流时 {}，{}",hookVo.getStream(), ObjectUtils.isEmpty(inviteInfo)?"未发现 inviteInfo":"发现 inviteInfo");
            if(inviteInfo != null){
                // 录像下载
                if (inviteInfo.getType() == VideoStreamType.download) {
                    map.put("close", false);
                    deferredResultHolder.invokeResult(key,uuid,map);
                    return deferredResult;
                }
                //收到无人观看说明流也没有在往上级推送
                if(sendRtpManager.isChannelSendingRTP(inviteInfo.getChannelId())){
                    List<SendRtp> sendRtpList = sendRtpManager.querySendRTPServerByChnnelId(inviteInfo.getChannelId());
                    for (SendRtp sendRtp : sendRtpList) {
                        ParentPlatformVo parentPlatformVo = parentPlatformVoService.getParentPlatformByServerGbId(sendRtp.getPlatform());
                        if(parentPlatformVo != null){
                            try {
                                sipCommanderForPlatform.streamByeCmd(sipServer, parentPlatformVo,sendRtp,null,null);
                            }catch (InvalidArgumentException | ParseException | SipException e){
                                log.error("[无人观看]点播， 发送 国标级联 BYE失败 {}", e.getMessage());
                            }
                        }
                    }
                }
                DeviceVo deviceVo = deviceVoService.findDeviceGbId(inviteInfo.getDeviceId());
                if(deviceVo != null){
                    try {
                        sipCommander.streamByeCmd(sipServer, deviceVo,inviteInfo.getChannelId(),inviteInfo.getStream(),null,null,(ok)->{
                            deferredResultHolder.invokeResult(key,uuid,map);
                        },(error)->{
                            log.error("[无人观看]点播， BYE异常 {}",error.getMsg());
                            deferredResultHolder.invokeResult(key,uuid,map);
                        });
                    }catch (InvalidArgumentException | ParseException | SipException |
                            SsrcTransactionNotFoundException e){
                        log.error("[无人观看]点播， 发送BYE失败 {}", e.getMessage());
                    }
                    return deferredResult;
                }else {
                    deferredResultHolder.invokeResult(key,uuid,map);
                    return deferredResult;
                }

            }else {
                deferredResultHolder.invokeResult(key,uuid,map);
                return deferredResult;
            }
        }else {
            // 非国标流 推流/拉流代理
            // 拉流代理
            StreamProxyVo streamProxyVo = streamProxyVoService.findAppStream(hookVo.getApp(), hookVo.getStream());
            if(streamProxyVo != null){
                if(streamProxyVo.getEnableRemoveNoneReader() == ConstEnum.Flag.YES.getValue()){
                    map.put("close", true);
                    streamProxyVoService.delete(hookVo.getApp(), hookVo.getStream());
                    String url = StringUtils.isNotEmpty(streamProxyVo.getUrl()) ? streamProxyVo.getUrl() : streamProxyVo.getSrcUrl();
                    log.info("[{}/{}]<-[{}] 拉流代理无人观看已经移除", hookVo.getApp(), hookVo.getStream(), url);
                } else if (streamProxyVo.getEnableDisableNoneReader() == ConstEnum.Flag.YES.getValue()) {
                    map.put("close", true);
                    streamProxyVoService.stop(hookVo.getApp(), hookVo.getStream());
                    String url = StringUtils.isNotEmpty(streamProxyVo.getUrl())? streamProxyVo.getUrl() : streamProxyVo.getSrcUrl();
                    log.info("[{}/{}]<-[{}] 拉流代理无人观看已经关闭", hookVo.getApp(), hookVo.getStream(), url);
                }else {
                    //拉流不做处理
                    map.put("close", false);
                }
            }
            deferredResultHolder.invokeResult(key,uuid,map);
            return deferredResult;
            //推流代理
            //推流暂不处理
        }
    }

    /**
     * 流未找到事件，用户可以在此事件触发时，立即去拉流，这样可以实现按需拉流；此事件对回复不敏感。
     */
    public CompletableFuture<NotNullMap> onStreamNotFound(OnStreamNotFoundHookVo hookVo){
        log.info("[ZLM HOOK] 流未找到：{}->{}->{}/{}", hookVo.getMediaServerId(), hookVo.getSchema(), hookVo.getApp(), hookVo.getStream());

        PlayService playService = SpringUtil.getBean(PlayService.class);
        DeferredResultHolder deferredResultHolder = SpringUtil.getBean(DeferredResultHolder.class);
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        StreamProxyVoService streamProxyVoService = VideoService.getStreamProxyService();
        MediaServerVo mediaServerVo = mediaServerVoService.findOnLineMediaServerId(hookVo.getMediaServerId());

        VideoRestResult<NotNullMap> deferredResult = new VideoRestResult<>(videoProperties.getPlayTimeout()*1000L,()->{return new NotNullMap(){{putInteger("code",404);putString("msg","资源未找到");}};});
        if(!videoProperties.getAutoApplyPlay() || mediaServerVo == null){
            deferredResult.complete(new NotNullMap(){{putInteger("code",404);putString("msg","资源未找到");}});
            return deferredResult;
        }
        if("rtp".equals(hookVo.getApp())){
            String[] split = hookVo.getStream().split("_");
            if(mediaServerVo.getRtpEnable()== ConstEnum.Flag.NO.getValue() || (split.length !=2 && split.length !=4)){
                deferredResult.complete(new NotNullMap(){{putInteger("code",0);putString("msg","success");}});
                return deferredResult;
            }
            String deviceId = split[0];
            String channelId = split[1];
            DeviceVo deviceVo = deviceVoService.findDeviceGbId(deviceId);
            if(deviceVo == null || deviceVo.getOnline()==ConstEnum.Flag.NO.getValue()){
                deferredResult.complete(new NotNullMap(){{putInteger("code",404);putString("msg","资源未找到");}});
                return deferredResult;
            }
            DeviceChannelVo deviceChannelVo = deviceChannelVoService.findChannelId(channelId);
            if(deviceChannelVo == null){
                deferredResult.complete(new NotNullMap(){{putInteger("code",404);putString("msg","资源未找到");}});
                return deferredResult;
            }
            //开始等待异步返回

            String uuid = RandomUtil.randomString(32);
            //将异步存储
            if(split.length ==2){
                log.info("[ZLM HOOK] 预览流未找到, 发起自动点播：{}->{}->{}/{}", hookVo.getMediaServerId(), hookVo.getSchema(), hookVo.getApp(), hookVo.getStream());
                String key = String.format("%s%s_%s",DeferredResultHolder.CALLBACK_CMD_PLAY,deviceId,channelId);
                deferredResultHolder.put(key,uuid,deferredResult);
                playService.play(sipServer,mediaServerVo,deviceVo.getDeviceId(),channelId,null,(code,msg,data)->{
                    deferredResultHolder.invokeResult(key,uuid,new NotNullMap(){{putInteger("code",code);putString("msg",msg);put("data",data);}});
                });
            }else{
                String startTimeStr = DateUtil.format(new Date(Long.parseLong(split[2])), DatePattern.NORM_DATETIME_PATTERN);
                String endTimeStr = DateUtil.format(new Date(Long.parseLong(split[3])), DatePattern.NORM_DATETIME_PATTERN);
                log.info("[ZLM HOOK] 回放流未找到, 发起自动点播：{}->{}->{}/{}-{}-{}", hookVo.getMediaServerId(), hookVo.getSchema(), hookVo.getApp(), hookVo.getStream(), startTimeStr, endTimeStr);
                String key = String.format("%s_%s_%s_%s",deviceVo.getDeviceId(),channelId, split[2],split[3]);
                deferredResultHolder.put(key,uuid,deferredResult);
                playService.playBack(sipServer,mediaServerVo,deviceVo, channelId,null, startTimeStr, endTimeStr, (code, msg, data)->{
                    deferredResultHolder.invokeResult(key,uuid, new NotNullMap(){{putInteger("code",code);putString("msg",msg);put("data",data);}});
                });
            }
        }else {
            //拉流代理
            StreamProxyVo streamProxyVo = streamProxyVoService.findAppStream(hookVo.getApp(), hookVo.getStream());
            if(streamProxyVo != null && streamProxyVo.getEnableDisableNoneReader() == ConstEnum.Flag.YES.getValue()){
                streamProxyVoService.start(streamProxyVo);
            }
            deferredResult.complete(new NotNullMap(){{putInteger("code",0);putString("msg","success");}});
        }
        return deferredResult;
    }

    /**
     * 服务器启动事件，可以用于监听服务器崩溃重启；此事件对回复不敏感。
     * @param ip 流媒体服务 回调ip
     * @param hookVo
     */
    public NotNullMap onServerStarted(String ip,JSONObject hookVo){
        hookVo.putOpt("ip",ip);
        ZLMServerConfig zlmServerConfig = JSONUtil.toBean(hookVo, ZLMServerConfig.class);
        MediaHookSubscribe mediaHookSubscribe = MediaService.getMediaHookSubscribe();
        ZlmService zlmService = SpringUtil.getBean(ZlmService.class);
        ThreadUtil.execute(()->{
            zlmServerConfig.setRestart(ConstEnum.Flag.NO.getValue());
            mediaHookSubscribe.sendNotify(MediaHookVo.builder().type(HookType.on_server_started).onAll(ConstEnum.Flag.YES.getValue()).mediaServerVo(null).hookVo(zlmServerConfig).build());
            zlmService.zlmOnline(zlmServerConfig);
        });
        return new NotNullMap(){{putInteger("code",0);putString("msg","success");}};
    }

    /**
     * 发送rtp(startSendRtp)被动关闭时回调
     */
    public NotNullMap onSendRtpStopped(OnSendRtpStoppedHookVo hookVo){
        log.info("[ZLM HOOK] rtp发送关闭：{}->{}/{}", hookVo.getMediaServerId(), hookVo.getApp(), hookVo.getStream());
        SendRtpManager sendRtpManager = RedisService.getSendRtpManager();
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        if("rtp".equals(hookVo.getApp())){
            ThreadUtil.execute(()->{
                List<SendRtp> sendRtpList = sendRtpManager.querySendRTPServerByStream(hookVo.getStream());
                for (SendRtp sendRtp : sendRtpList) {
                    ssrcConfigManager.releaseSsrc(sendRtp.getMediaServerId(),sendRtp.getSsrc());
                    // 设备编号 或 上级平台
                    String platformId = sendRtp.getPlatform();
                    ParentPlatformVo parentPlatformVo = parentPlatformVoService.getParentPlatformByServerGbId(platformId);
                    DeviceVo deviceVo = deviceVoService.findDeviceGbId(platformId);
                    try {
                        if(parentPlatformVo != null){
                            sipCommanderForPlatform.streamByeCmd(sipServer, parentPlatformVo,sendRtp,null,null);
                            sendRtpManager.deleteSendRTPServer(sendRtp.getPlatformId(),sendRtp.getChannelId(),sendRtp.getStreamId(),sendRtp.getCallId());
                        }else {
                            sipCommander.streamByeCmd(sipServer, deviceVo,sendRtp.getChannelId(),sendRtp.getStreamId(),sendRtp.getCallId(),null,null,null);
                            sendRtpManager.deleteSendRTPServer(sendRtp.getPlatformId(),sendRtp.getChannelId(),sendRtp.getStreamId(),sendRtp.getCallId());
                        }
                    }catch (Exception e){
                        log.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                    }
                }
            });
        }
        return new NotNullMap(){{putInteger("code",0);putString("msg","success");}};
    }

    /**
     * rtpServer收流超时
     */
    public NotNullMap onRtpServerTimeout(OnRtpServerTimeoutHookVo hookVo){
        log.info("[ZLM HOOK] rtpServer收流超时：{}->{}({})", hookVo.getMediaServerId(), hookVo.getStream_id(), hookVo.getSsrc());
        ThreadUtil.execute(()->{
            List<HookEvent> subscribes = mediaHookSubscribe.getSubscribes(HookType.on_rtp_server_timeout);
            if(subscribes != null && !subscribes.isEmpty()){
                for (HookEvent subscribe : subscribes) {
                    subscribe.response(null,hookVo);
                }
            }
        });
        return new NotNullMap(){{putInteger("code",0);putString("msg","success");}};
    }
}
