package cn.com.nla.common.video.core.media.client;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.enums.OriginType;
import cn.com.nla.common.video.basic.exception.SsrcTransactionNotFoundException;
import cn.com.nla.common.video.basic.vo.media.*;
import cn.com.nla.common.video.basic.vo.sip.SendRtp;
import cn.com.nla.common.video.basic.vo.video.*;
import cn.com.nla.common.video.core.demo.InviteInfo;
import cn.com.nla.common.video.core.demo.SsrcTransaction;
import cn.com.nla.common.video.core.media.hook.MediaHookServer;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.*;
import cn.com.nla.common.video.core.redis.subscribe.media.HookKeyFactory;
import cn.com.nla.common.video.core.redis.subscribe.media.MediaHookSubscribe;
import cn.com.nla.common.video.core.service.MediaService;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.*;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.common.video.core.sip.cmd.SIPCommanderForPlatform;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ZlmService {

    private final String zlmKeepaliveKeyPrefix = "zlm-keepalive_";
    @Autowired
    private DynamicTask dynamicTask;
    @Autowired
    private SIPCommander sipCommander;
    @Autowired
    private SIPCommanderForPlatform sipCommanderForPlatform;
    @Autowired
    private SipServer sipServer;


    //流媒体上线
    public void zlmOnline(ZLMServerConfig zlmServerConfig) {
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        MediaServerManager mediaServerManager = RedisService.getMediaServerManager();

        MediaServerVo mediaServerVo = mediaServerVoService.findMediaServerId(zlmServerConfig.getGeneralMediaServerId());
        if(mediaServerVo == null){
            log.error("[未注册的zlm] 拒接接入：{}来自{}：{}", zlmServerConfig.getGeneralMediaServerId(), zlmServerConfig.getIp(),zlmServerConfig.getHttpPort() );
            log.error("请检查ZLM的<general.mediaServerId>配置是否与WVP的<media.id>一致");
            return;
        }
        log.info("[ZLM] 正在连接 : {} -> {}:{}", zlmServerConfig.getGeneralMediaServerId(), zlmServerConfig.getIp(), zlmServerConfig.getHttpPort());
        if (mediaServerVo.getHttpPort() == 0) {
            mediaServerVo.setHttpPort(zlmServerConfig.getHttpPort());
        }
        if (mediaServerVo.getHttpSslPort() == 0) {
            mediaServerVo.setHttpSslPort(zlmServerConfig.getHttpSSLport());
        }
        if (mediaServerVo.getRtmpPort() == 0) {
            mediaServerVo.setRtmpPort(zlmServerConfig.getRtmpPort());
        }
        if (mediaServerVo.getRtmpSslPort() == 0) {
            mediaServerVo.setRtmpSslPort(zlmServerConfig.getRtmpSslPort());
        }
        if (mediaServerVo.getRtspPort() == 0) {
            mediaServerVo.setRtspPort(zlmServerConfig.getRtspPort());
        }
        if (mediaServerVo.getRtspSslPort() == 0) {
            mediaServerVo.setRtspSslPort(zlmServerConfig.getRtspSSlport());
        }
        if (mediaServerVo.getRtpProxyPort() == 0) {
            mediaServerVo.setRtpProxyPort(zlmServerConfig.getRtpProxyPort());
        }
        mediaServerVo.setStatus(ConstEnum.Flag.YES.getValue());
        //修改信息
        mediaServerVoService.updateById(mediaServerVo);
        //存储ZLM的ssrc信息
        if(!ssrcConfigManager.hasMediaServerSSRC(mediaServerVo.getId())){
            ssrcConfigManager.initMediaServerSSRC(mediaServerVo.getId(),null);
        }
        //是否自动zlm配置文件
        if(mediaServerVo.getAutoConfig() == ConstEnum.Flag.YES.getValue()){
            //重启时直接关闭全部留
            boolean b = zlmServerConfig.getRestart() == ConstEnum.Flag.YES.getValue();
            cleanStream(mediaServerVo,b);
            MediaClient.zlmConfigAuto(mediaServerVo,b);
            //MediaClient.zlmConfigAuto(mediaServerVo,true);
        }
        //初始化流媒体信息
        mediaServerManager.resetOnlineServerItem(mediaServerVo);
        //定时检测是否在线
        zlmKeepaliveTask(mediaServerVo,false);
        //定时检测流上下线操作并相关处理
        zlmStreamChanged(mediaServerVo);
        //上线操作
        zlmOnline(mediaServerVo);
    }
    /**
     * 心跳任务
     * @param mediaServerVo
     */
    public void zlmKeepaliveTask(MediaServerVo mediaServerVo,boolean isDel){
        MediaHookSubscribe mediaHookSubscribe = MediaService.getMediaHookSubscribe();
        //心跳事件
        HookKey hookKey = HookKeyFactory.onServerKeepalive(mediaServerVo.getId());
        if(isDel){
            mediaHookSubscribe.removeSubscribe(hookKey);
            zlmKeepalive(mediaServerVo, true);
        }else {
            HookKey key = mediaHookSubscribe.getHookKey(hookKey);
            if(key == null){
                mediaHookSubscribe.addSubscribe(hookKey,(server, response)->{
                    zlmKeepalive(server, false);//更新订阅过期时间
                });
            }
        }
    }
    private void zlmKeepalive(MediaServerVo mediaServerVo,boolean isDel){
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        MediaServerManager mediaServerManager = RedisService.getMediaServerManager();
        MediaHookSubscribe mediaHookSubscribe = MediaService.getMediaHookSubscribe();
        //设置zlm服务上线
        String key = zlmKeepaliveKeyPrefix + mediaServerVo.getId();
        if(isDel){
            dynamicTask.stop(key);
            return;
        }
        //更新流媒体状态
        mediaServerVoService.updateStatus(mediaServerVo.getId(),ConstEnum.Flag.YES.getValue());
        //订阅更新过期时间
        HookKey hookKey = mediaHookSubscribe.getHookKey(HookKeyFactory.onServerKeepalive(mediaServerVo.getId()));
        if(hookKey != null){
            hookKey.updateExpires(null);//更新订阅过期时间
        }

        //保证有一定延迟
        dynamicTask.startCron(key, mediaServerVo.getHookAliveInterval()+ VideoConstant.DELAY_TIME,mediaServerVo.getHookAliveInterval()+ VideoConstant.DELAY_TIME ,()->{
            //订阅更新过期时间
            HookKey hook = mediaHookSubscribe.getHookKey(HookKeyFactory.onServerKeepalive(mediaServerVo.getId()));
            if(hook != null){
                hook.updateExpires(null);
            }
            MediaRestResult zlmConfig = MediaClient.getZlmConfig(mediaServerVo);
            if(zlmConfig != null && zlmConfig.getCode() == 0){
                log.info("[zlm心跳到期]：{}验证后zlm仍在线，请检查zlm是否可以正常向服务发送心跳", mediaServerVo.getId());
                //在线
                if(!ssrcConfigManager.hasMediaServerSSRC(mediaServerVo.getId())){
                    ssrcConfigManager.initMediaServerSSRC(mediaServerVo.getId(),null);
                }
                mediaServerManager.clearRTPServer(mediaServerVo);
            }else {
                //离线
                log.info("[zlm心跳到期]：{}验证后zlm离线，请检查zlm是否可以正常向服务发送心跳", mediaServerVo.getId());
                mediaServerVoService.updateStatus(mediaServerVo.getId(),ConstEnum.Flag.NO.getValue());
                zlmOffline(mediaServerVo);
            }
        });
    }
    /**
     * zlm上线需操作的
     */
    private void zlmOnline(MediaServerVo mediaServerVo){
        GbStreamVoService gbStreamVoService = VideoService.getGbStreamService();
        StreamPushVoService streamPushVoService = VideoService.getStreamPushService();
        StreamProxyVoService streamProxyVoService = VideoService.getStreamProxyService();
        //获取推流
        Map<String, StreamPushVo> streamPushMap = new HashMap<>();
        List<StreamPushVo> streamPushVoList = streamPushVoService.findMediaServiceNotGbId(mediaServerVo.getId());
        if(!streamPushVoList.isEmpty()){
            //获取不是国标的推流信息
            streamPushMap = streamPushVoList.stream().collect(Collectors.toMap(o -> o.getApp() + o.getStream(), o -> o));
        }
        MediaRestResult result = MediaClient.getMediaList(mediaServerVo, "__defaultVhost__", null, null, null);
        if(result != null){
            log.info("zlm上线 解析ZLM getMediaList data:{}",result.getData());
            List<OnStreamChangedHookVo> onStreamChangedHookVos = JSONUtil.toList(JSONUtil.toJsonStr(result.getData()), OnStreamChangedHookVo.class);
            Map<String, StreamPushVo> vo = new HashMap<>();
            for (OnStreamChangedHookVo onStreamChangedHookVo : onStreamChangedHookVos) {
                if(onStreamChangedHookVo.getOriginType() == OriginType.RTSP_PUSH.ordinal() || onStreamChangedHookVo.getOriginType() == OriginType.RTMP_PUSH.ordinal() || onStreamChangedHookVo.getOriginType() == OriginType.RTC_PUSH.ordinal()){
                    StreamPushVo streamPushVo = vo.get(onStreamChangedHookVo.getApp() + onStreamChangedHookVo.getStream());
                    if(streamPushVo == null){
                        vo.put(onStreamChangedHookVo.getApp() + onStreamChangedHookVo.getStream(),new StreamPushVo().transform(onStreamChangedHookVo));
                    }
                }
            }
            if(!vo.isEmpty()){
                for (StreamPushVo value : vo.values()) {
                    streamPushMap.remove(value.getApp()+value.getStream());
                    streamPushVoService.updateStatus(value.getApp(),value.getStream(),true);
                }
            }
            if(!streamPushMap.isEmpty()){
                for (StreamPushVo value : streamPushMap.values()) {
                    streamPushVoService.removePush(value.getApp(),value.getStream());
                }
            }
        }
        //删除无人观看自动移除的流
        List<StreamProxyVo> streamProxyVoList = streamProxyVoService.findAutoRemoveMediaServerIdList(mediaServerVo.getId());
        if(CollUtil.isNotEmpty(streamProxyVoList)){
            for (StreamProxyVo streamProxyVo : streamProxyVoList) {
                gbStreamVoService.delAppStream(streamProxyVo.getApp(), streamProxyVo.getStream());
                streamProxyVoService.del(streamProxyVo.getApp(), streamProxyVo.getStream());
            }
            //修改其他流为离线
            streamProxyVoService.updateStatus(mediaServerVo.getId(),false);
        }
        //恢复流代理
        streamProxyVoList = streamProxyVoService.findEnableInMediaServerList(mediaServerVo.getId(), true);
        for (StreamProxyVo streamProxyVo : streamProxyVoList) {
            log.info("恢复拉流代理，{}/{}", streamProxyVo.getApp(), streamProxyVo.getStream());
            MediaRestResult restResult = MediaClient.addStreamProxyToZlm(mediaServerVo, streamProxyVo);
            if(restResult != null && restResult.getCode() == 0){
                streamProxyVoService.updateStatus(streamProxyVo.getApp(), streamProxyVo.getStream(),null,true);
            }else {
                log.error("恢复流代理失败，{}/{}", streamProxyVo.getApp(), streamProxyVo.getStream());
                streamProxyVoService.updateStatus(streamProxyVo.getApp(), streamProxyVo.getStream(),null,false);
            }
        }
        //国标的流需要重新点击播放
    }
    /**
     * zlm下线
     */
    public void zlmOffline(MediaServerVo mediaServerVo){
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        GbStreamVoService gbStreamVoService = VideoService.getGbStreamService();
        StreamProxyVoService streamProxyVoService = VideoService.getStreamProxyService();
        StreamPushVoService streamPushVoService = VideoService.getStreamPushService();
        SendRtpManager sendRtpManager = RedisService.getSendRtpManager();
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        //取消zlm在线认证
        zlmKeepaliveTask(mediaServerVo,true);
        dynamicTask.stop(streamChangedTaskKey(mediaServerVo.getId()));
        //删除无人观看自动移除的流
        List<StreamProxyVo> streamProxyVoList = streamProxyVoService.findAutoRemoveMediaServerIdList(mediaServerVo.getId());
        if(! streamProxyVoList.isEmpty()){
            for (StreamProxyVo streamProxyVo : streamProxyVoList) {
                gbStreamVoService.delAppStream(streamProxyVo.getApp(), streamProxyVo.getStream());
                streamProxyVoService.del(streamProxyVo.getApp(), streamProxyVo.getStream());
            }
            //修改其他流为离线
            streamProxyVoService.updateStatus(mediaServerVo.getId(),false);
        }
       //移除没有GBid的推流
        List<StreamPushVo> streamPushVoList = streamPushVoService.findMediaServiceNotGbId(mediaServerVo.getId());
        if(! streamPushVoList.isEmpty()){
            for (StreamPushVo streamPushVo : streamPushVoList) {
                streamPushVoService.del(streamPushVo.getApp(), streamPushVo.getStream());
                gbStreamVoService.delAppStream(streamPushVo.getApp(), streamPushVo.getStream());
            }
        }
        // 其他的流设置未启用
        streamPushVoService.updateStatus(mediaServerVo.getId(),false);
        //移除播放等
        List<SendRtp> sendRtpList = sendRtpManager.queryAllSendRTPServer();
        for (SendRtp sendRtp : sendRtpList) {
            if(!sendRtp.getMediaServerId().equals(mediaServerVo.getId())){
                continue;
            }
            ParentPlatformVo parentPlatformVo = parentPlatformVoService.getParentPlatformByServerGbId(sendRtp.getPlatform());
            try {
                sipCommanderForPlatform.streamByeCmd(sipServer, parentPlatformVo,sendRtp,null,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
            }
        }
        List<SsrcTransaction> allSsrc = ssrcTransactionManager.getAllSsrc();
        for (SsrcTransaction ssrcTransaction : allSsrc) {
            if(!ssrcTransaction.getMediaServerId().equals(mediaServerVo.getId())){
                continue;
            }
            DeviceVo deviceVo = deviceVoService.findDeviceGbId(ssrcTransaction.getDeviceId());
            if(deviceVo == null){
                continue;
            }
            try {
                sipCommander.streamByeCmd(sipServer, deviceVo,ssrcTransaction.getChannelId(),ssrcTransaction.getStream(),null,null,null,null);
            }catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e){
                log.error("[无人观看]回放， 发送BYE失败 {}", e.getMessage());
            }
        }
    }
    //定时检测流上下线操作并相关处理
    public void zlmStreamChanged(MediaServerVo mediaServerVo){
        dynamicTask.startCron(streamChangedTaskKey(mediaServerVo.getId()),60,()->{
            cleanStream(mediaServerVo,false);
        });
    }

    private String streamChangedTaskKey(String mediaServerId) {
        return StreamChangedManager.VIDEO_MEDIA_STREAM_CHANGED_PREFIX + mediaServerId;
    }

    private  void cleanStream(MediaServerVo mediaServerVo,boolean delAll){
        StreamChangedManager streamChangedManager = RedisService.getStreamChangedManager();
        MediaServerVoService mediaServerService = VideoService.getMediaServerService();
        MediaHookServer mediaHookServer = SpringUtil.getBean(MediaHookServer.class);
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();

        List<OnStreamChangedHookVo> mediaServerAll = streamChangedManager.getMediaServerAll(mediaServerVo.getId());
        if(mediaServerAll != null && !mediaServerAll.isEmpty()){
            List<OnStreamChangedHookVo> delete = new ArrayList<>();
            for (OnStreamChangedHookVo vo : mediaServerAll) {
                MediaServerVo mediaServer = mediaServerService.findMediaServerId(vo.getMediaServerId());
                if(mediaServer == null){
                    delete.add(vo);
                    continue;
                }
                OnStreamChangedResult result = MediaClient.getMediaInfo(mediaServerVo, null, vo.getSchema(), vo.getApp(), vo.getStream());
                if(ZlmStreamCleanupPolicy.shouldClose(vo, result, delAll, System.currentTimeMillis() / 1000)){
                    delete.add(vo);
                    continue;
                }
            }
            for (OnStreamChangedHookVo vo : delete) {
                OnStreamNoneReaderHookVo build = OnStreamNoneReaderHookVo.builder()
                        .app(vo.getApp())
                        .stream(vo.getStream())
                        .schema(vo.getSchema())
                        .mediaServerId(vo.getMediaServerId())
                        .build();
                log.info("定时任务检测到无人观看流，自动关闭：{}",build);
                mediaHookServer.onStreamNoneReader(build);
                streamChangedManager.remove(vo);
                //只有重启 zlm 时 delAll = true
                // onStreamNoneReader 调用后 streamByeCmd 发送注销 ，这时zlm重启， 不触发hook onStreamChanged，手动删除及修改状态
                if(delAll){
                    InviteInfo inviteInfo = inviteStreamManager.getInviteInfoByStream(null, vo.getStream());
                    if(inviteInfo != null){
                        deviceChannelVoService.stopPlay(inviteInfo.getDeviceId(), inviteInfo.getChannelId());
                        inviteStreamManager.removeInviteInfo(inviteInfo);
                    }
                }
            }

        }
    }
}
