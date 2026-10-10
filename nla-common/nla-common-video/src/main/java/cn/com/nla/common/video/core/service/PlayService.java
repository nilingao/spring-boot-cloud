package cn.com.nla.common.video.core.service;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.video.core.service.authentication.CurrentUserProvider;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.enums.*;
import cn.com.nla.common.video.basic.exception.SsrcTransactionNotFoundException;
import cn.com.nla.common.video.basic.vo.media.HookKey;
import cn.com.nla.common.video.basic.vo.media.MediaRestResult;
import cn.com.nla.common.video.basic.vo.media.OnStreamChangedHookVo;
import cn.com.nla.common.video.basic.vo.sip.SSRCInfo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.core.demo.Gb28181Sdp;
import cn.com.nla.common.video.core.demo.InviteInfo;
import cn.com.nla.common.video.core.demo.SsrcTransaction;
import cn.com.nla.common.video.core.demo.StreamInfo;
import cn.com.nla.common.video.core.media.client.MediaClient;
import cn.com.nla.common.video.core.model.EventResult;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.com.nla.common.video.core.properties.VideoProperties;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.InviteStreamManager;
import cn.com.nla.common.video.core.redis.impl.SsrcConfigManager;
import cn.com.nla.common.video.core.redis.impl.SsrcTransactionManager;
import cn.com.nla.common.video.core.redis.subscribe.media.HookEvent;
import cn.com.nla.common.video.core.redis.subscribe.media.HookKeyFactory;
import cn.com.nla.common.video.core.redis.subscribe.media.MediaHookSubscribe;
import cn.com.nla.common.video.core.service.video.DeviceChannelVoService;
import cn.com.nla.common.video.core.service.video.DeviceVoService;
import cn.com.nla.common.video.core.service.video.MediaServerVoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.callback.InviteErrorCallback;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.common.video.core.utils.SipUtils;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.Vector;

/**
 * 播放功能实现
 */
@Slf4j
public class PlayService {

    @Autowired
    private VideoProperties videoProperties;
    @Autowired
    private DynamicTask dynamicTask;
    @Autowired
    private SIPCommander sipCommander;
    @Autowired
    private MediaHookSubscribe mediaHookSubscribe;


    public SSRCInfo play(SipServer sipServer, MediaServerVo mediaServerVo, String deviceId, String channelId,String ssrc, InviteErrorCallback<Object> callback){
        if(mediaServerVo == null){
            throw new ServiceException("未找到可用的zlm");
        }

        DeviceVoService deviceVoService = VideoService.getDeviceService();
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();

        DeviceVo deviceVo = deviceVoService.findDeviceGbId(deviceId);
        //获取播放流
        InviteInfo inviteInfo = inviteStreamManager.getInviteInfoByDeviceAndChannel(VideoStreamType.play, deviceId, channelId);
        if(inviteInfo != null){
            if (inviteInfo.getStreamInfo() == null) {
                log.info("inviteInfo 已存在， StreamInfo 不存在，添加回调等待");
                // 点播发起了但是尚未成功, 仅注册回调等待结果即可
                inviteStreamManager.once(VideoStreamType.play, deviceId, channelId, null, callback);
                return inviteInfo.getSsrcInfo();
            }else {
                StreamInfo streamInfo = inviteInfo.getStreamInfo();
                String streamId = streamInfo.getStream();
                if (streamId == null) {
                    callback.run(InviteErrorCode.ERROR_FOR_CATCH_DATA.getCode(), "点播失败， redis缓存streamId等于null", null);
                    inviteStreamManager.call(VideoStreamType.play, deviceId, channelId, null,
                            InviteErrorCode.ERROR_FOR_CATCH_DATA.getCode(),
                            "点播失败， redis缓存streamId等于null",
                            null);
                    return inviteInfo.getSsrcInfo();
                }
                String mediaServerId = streamInfo.getMediaServerId();
                MediaServerVo media = mediaServerVoService.findOnLineMediaServerId(mediaServerId);
                MediaRestResult mediaList = MediaClient.getMediaList(media, "__defaultVhost__", null, "rtp", streamId);
                if(mediaList != null && mediaList.getCode() == RespCode.CODE_0.getValue() && ObjectUtil.isNotEmpty(mediaList.getData())){
                    callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                    inviteStreamManager.call(VideoStreamType.play, deviceId, channelId, null,
                            InviteErrorCode.SUCCESS.getCode(),
                            InviteErrorCode.SUCCESS.getMsg(),
                            streamInfo);
                    return inviteInfo.getSsrcInfo();
                }else {
                    // 点播发起了但是尚未成功, 仅注册回调等待结果即可
                    inviteStreamManager.once(VideoStreamType.play, deviceId, channelId, null, callback);
                    deviceChannelVoService.stopPlay(streamInfo.getDeviceId(), streamInfo.getChannelId());
                    inviteStreamManager.removeInviteInfoByDeviceAndChannel(VideoStreamType.play, deviceId, channelId);
                }
            }
        }
        // 播放开始
        String streamId = String.format("%s_%s", deviceVo.getDeviceId(),channelId);
        //开启播放相关信息
        SSRCInfo ssrcInfo = MediaClient.openRTPServer(mediaServerVo, streamId, ssrc, deviceVo.getSsrcCheck() == ConstEnum.Flag.YES.getValue(), false, 0,false,deviceVo.getStreamMode());
        if(ssrcInfo == null){
            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(), null);
            inviteStreamManager.call(VideoStreamType.play, deviceVo.getDeviceId(), channelId, null,
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                    null);
            return null;
        }
        play(sipServer, mediaServerVo,ssrcInfo, deviceVo,channelId,callback);
        return ssrcInfo;
    }

    /**
     * 点播实时流
     * @param mediaServerVo 流媒体信息
     * @param ssrcInfo 接收流信息
     * @param deviceVo 设备信息
     * @param channelId 通道编号
     * @param callback 事件回调
     */
    public void play(SipServer sipServer, MediaServerVo mediaServerVo, SSRCInfo ssrcInfo, DeviceVo deviceVo, String channelId,InviteErrorCallback<Object> callback){
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();

        log.info("[点播开始] deviceId: {}, channelId: {},收流端口：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", deviceVo.getDeviceId(), channelId, ssrcInfo.getPort(), deviceVo.getStreamMode(), ssrcInfo.getSsrc(), deviceVo.getSsrcCheck());
        if(ssrcInfo.getPort() <= 0){
            log.error("[点播端口分配异常]，deviceId={},channelId={},ssrcInfo={}", deviceVo.getDeviceId(), channelId, ssrcInfo);
            //释放 ssrc
            ssrcConfigManager.releaseSsrc(mediaServerVo.getId(),ssrcInfo.getSsrc());
            ssrcTransactionManager.remove(deviceVo.getDeviceId(),channelId,ssrcInfo.getStream());

            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "点播端口分配异常", null);
            inviteStreamManager.call(VideoStreamType.play, deviceVo.getDeviceId(), channelId, null,
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "点播端口分配异常", null);
            return;
        }

        // 初始化redis中的invite消息状态
        InviteInfo inviteInfo = new InviteInfo(ObjectUtil.defaultIfNull(cn.hutool.extra.spring.SpringUtil.getBean(CurrentUserProvider.class).getUserId(), VideoConstant.DEFAULT_DOWNLOAD_USER),deviceVo.getDeviceId(), channelId, ssrcInfo.getStream(), ssrcInfo,
                mediaServerVo.getSdpIp(), ssrcInfo.getPort(), deviceVo.getStreamMode(), VideoStreamType.play,
                InviteSessionStatus.ready);
        inviteStreamManager.updateInviteInfo(inviteInfo);

        //超时任务处理
        String timeOutTaskKey = RandomUtil.randomString(32);
        dynamicTask.startDelay(timeOutTaskKey,videoProperties.getPlayTimeout(),()->{
            // 执行超时任务时查询是否已经成功，成功了则不执行超时任务，防止超时任务取消失败的情况
            InviteInfo inviteInfoForTimeOut = inviteStreamManager.getInviteInfoByDeviceAndChannel(VideoStreamType.play, deviceVo.getDeviceId(), channelId);
            if(inviteInfoForTimeOut == null || inviteInfoForTimeOut.getStreamInfo() == null){
                log.info("[点播超时] 收流超时 deviceId: {}, channelId: {}，端口：{}, SSRC: {}", deviceVo.getDeviceId(), channelId, ssrcInfo.getPort(), ssrcInfo.getSsrc());
                // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                callback.run(InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
                inviteStreamManager.call(VideoStreamType.play, deviceVo.getDeviceId(), channelId, null,
                        InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);

                inviteStreamManager.removeInviteInfoByDeviceAndChannel(VideoStreamType.play, deviceVo.getDeviceId(), channelId);
                try {
                    sipCommander.streamByeCmd(sipServer, deviceVo,channelId,ssrcInfo.getStream(),null,null,null,null);
                } catch (InvalidArgumentException | SipException | ParseException| SsrcTransactionNotFoundException  e) {
                    log.error("[点播超时]， 发送BYE失败 {}", e.getMessage());
                }finally {
                    ssrcConfigManager.releaseSsrc(mediaServerVo.getId(),ssrcInfo.getSsrc());
                    ssrcTransactionManager.remove(deviceVo.getDeviceId(),channelId,ssrcInfo.getStream());
                    MediaClient.closeRtpServer(mediaServerVo,ssrcInfo.getStream());
                    // 取消订阅消息监听
                    HookKey hookKey = HookKeyFactory.onStreamChanged("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerVo.getId());
                    mediaHookSubscribe.removeSubscribe(hookKey);
                }
            }
        });

        try {
            sipCommander.playStreamCmd(sipServer, mediaServerVo,ssrcInfo, deviceVo,channelId,videoProperties.getAutoApplyPlay(), (media, response)->{
                log.info("收到订阅消息： " + JSONUtil.toJsonStr(response));
                dynamicTask.stop(timeOutTaskKey);
                OnStreamChangedHookVo vo = (OnStreamChangedHookVo) response;
                StreamInfo streamInfo = new StreamInfo(mediaServerVo, "rtp", vo.getStream(), vo.getTracks(), null, null, deviceVo.getDeviceId(), channelId);
                InviteInfo invite = inviteStreamManager.getInviteInfoByDeviceAndChannel(VideoStreamType.play, deviceVo.getDeviceId(), channelId);
                if (invite != null) {
                    invite.setStatus(InviteSessionStatus.ok);
                    invite.setStreamInfo(streamInfo);
                    inviteStreamManager.updateInviteInfo(invite);
                }
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                inviteStreamManager.call(VideoStreamType.play, deviceVo.getDeviceId(), channelId, null,
                        InviteErrorCode.SUCCESS.getCode(),
                        InviteErrorCode.SUCCESS.getMsg(),
                        streamInfo);
                //返回成功流信息
                //可获取视频截图
            },(okEvent) ->{
                inviteOKHandler(sipServer,mediaServerVo,deviceVo,channelId,ssrcInfo,inviteInfo,okEvent,timeOutTaskKey,callback);
            },(errEvent) ->{
                log.error("收到订阅错误消息： " + JSONUtil.toJsonStr(errEvent));
                //发送错误后处理
                dynamicTask.stop(timeOutTaskKey);
                ssrcConfigManager.releaseSsrc(mediaServerVo.getId(),ssrcInfo.getSsrc());
                ssrcTransactionManager.remove(deviceVo.getDeviceId(),channelId,ssrcInfo.getStream());
                MediaClient.closeRtpServer(mediaServerVo,ssrcInfo.getStream());
                callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_ERROR.getCode(),
                        String.format("点播失败， 错误码： %s, %s", errEvent.getStatusCode(), errEvent.getMsg()), null);
                inviteStreamManager.call(VideoStreamType.play, deviceVo.getDeviceId(), channelId, null,
                        InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                        String.format("点播失败， 错误码： %s, %s", errEvent.getStatusCode(), errEvent.getMsg()), null);

                inviteStreamManager.removeInviteInfoByDeviceAndChannel(VideoStreamType.play, deviceVo.getDeviceId(), channelId);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            //发送异常处理
            log.error("[命令发送失败] 点播消息: {}", e.getMessage());
            dynamicTask.stop(timeOutTaskKey);
            ssrcConfigManager.releaseSsrc(mediaServerVo.getId(),ssrcInfo.getSsrc());
            ssrcTransactionManager.remove(deviceVo.getDeviceId(),channelId,ssrcInfo.getStream());
            MediaClient.closeRtpServer(mediaServerVo,ssrcInfo.getStream());
            callback.run(InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(), InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);
            inviteStreamManager.call(VideoStreamType.play, deviceVo.getDeviceId(), channelId, null,
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);
            inviteStreamManager.removeInviteInfoByDeviceAndChannel(VideoStreamType.play, deviceVo.getDeviceId(), channelId);
        }
    }

    public SSRCInfo playBack(SipServer sipServer,MediaServerVo mediaServerVo,DeviceVo deviceVo, String channelId, String ssrc,String startTime, String endTime, InviteErrorCallback<Object> callback) {
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();
        //流编号
        String streamId = String.format("%s_%s_%s_%s",deviceVo.getDeviceId(),channelId, DateUtil.parse(startTime).getTime(),DateUtil.parse(endTime).getTime());
        //获取播放流
        InviteInfo inviteInfo = inviteStreamManager.getInviteInfo(VideoStreamType.playback, deviceVo.getDeviceId(), channelId,streamId,null);
        if(inviteInfo != null){
            if (inviteInfo.getStreamInfo() == null) {
                log.info("inviteInfo 已存在， StreamInfo 不存在，添加回调等待");
                // 点播发起了但是尚未成功, 仅注册回调等待结果即可
                inviteStreamManager.once(VideoStreamType.playback, deviceVo.getDeviceId(), channelId, streamId, callback);
                return inviteInfo.getSsrcInfo();
            }else {
                StreamInfo streamInfo = inviteInfo.getStreamInfo();
                streamId = streamInfo.getStream();
                if (streamId == null) {
                    callback.run(InviteErrorCode.ERROR_FOR_CATCH_DATA.getCode(), "回放播放失败， redis缓存streamId等于null", null);
                    inviteStreamManager.call(VideoStreamType.playback, deviceVo.getDeviceId(), channelId, streamId,
                            InviteErrorCode.ERROR_FOR_CATCH_DATA.getCode(),
                            "回放播放失败， redis缓存streamId等于null",
                            null);
                    return inviteInfo.getSsrcInfo();
                }
                String mediaServerId = streamInfo.getMediaServerId();
                MediaServerVo media = mediaServerVoService.findOnLineMediaServerId(mediaServerId);
                MediaRestResult mediaList = MediaClient.getMediaList(media, "__defaultVhost__", null, "rtp", streamId);
                if(mediaList != null && mediaList.getCode() == RespCode.CODE_0.getValue() && ObjectUtil.isNotEmpty(mediaList.getData())){
                    callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                    inviteStreamManager.call(VideoStreamType.playback, deviceVo.getDeviceId(), channelId, streamId,
                            InviteErrorCode.SUCCESS.getCode(),
                            InviteErrorCode.SUCCESS.getMsg(),
                            streamInfo);
                    return inviteInfo.getSsrcInfo();
                }else {
                    // 点播发起了但是尚未成功, 重新发起
                    inviteStreamManager.once(VideoStreamType.playback, deviceVo.getDeviceId(), channelId, streamId, callback);
                    inviteStreamManager.removeInviteInfoByDeviceAndChannel(VideoStreamType.playback, deviceVo.getDeviceId(), channelId);
                }
            }
        }
        // 播放开始
        SSRCInfo ssrcInfo = MediaClient.openRTPServer(mediaServerVo, streamId, ssrc, deviceVo.getSsrcCheck() == ConstEnum.Flag.YES.getValue(), true, 0,false,deviceVo.getStreamMode());
        if(ssrcInfo == null){
            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(), null);
            return null;
        }
        playBack(sipServer, mediaServerVo,ssrcInfo, deviceVo, channelId, startTime, endTime, callback);
        return ssrcInfo;
    }


    public void playBack(SipServer sipServer, MediaServerVo mediaServerVo, SSRCInfo ssrcInfo, DeviceVo deviceVo, String channelId, String startTime, String endTime, InviteErrorCallback<Object> callback){
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();

        if(mediaServerVo == null || ssrcInfo == null){
            callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                    InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                    null);
            return;
        }
        log.info("[录像回放] deviceId: {}, channelId: {}, 开始时间: {}, 结束时间： {}, 收流端口：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                deviceVo.getDeviceId(), channelId, startTime, endTime, ssrcInfo.getPort(), deviceVo.getStreamMode(),
                ssrcInfo.getSsrc(), deviceVo.getSsrcCheck());
        InviteInfo inviteInfo = new InviteInfo(ObjectUtil.defaultIfNull(cn.hutool.extra.spring.SpringUtil.getBean(CurrentUserProvider.class).getUserId(), VideoConstant.DEFAULT_DOWNLOAD_USER),deviceVo.getDeviceId(), channelId, ssrcInfo.getStream(), ssrcInfo,
                mediaServerVo.getSdpIp(), ssrcInfo.getPort(), deviceVo.getStreamMode(), VideoStreamType.playback,
                InviteSessionStatus.ready);
        inviteStreamManager.updateInviteInfo(inviteInfo);
        //回放超时处理
        String timeOutTaskKey = RandomUtil.randomString(32);
        dynamicTask.startDelay(timeOutTaskKey,videoProperties.getPlayTimeout(),()->{
            log.warn(String.format("设备回放超时，deviceId：%s ，channelId：%s", deviceVo.getId(), channelId));
            inviteStreamManager.removeInviteInfo(inviteInfo);
            callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getMsg(), null);
            try {
                sipCommander.streamByeCmd(sipServer, deviceVo,channelId,ssrcInfo.getStream(),null,null,null,null);
            } catch (InvalidArgumentException | SipException | ParseException| SsrcTransactionNotFoundException  e) {
                log.error("[录像流]回放超时 发送BYE失败 {}", e.getMessage());
            }finally {
                ssrcConfigManager.releaseSsrc(mediaServerVo.getId(),ssrcInfo.getSsrc());
                ssrcTransactionManager.remove(deviceVo.getDeviceId(),channelId,ssrcInfo.getStream());
                MediaClient.closeRtpServer(mediaServerVo,ssrcInfo.getStream());
                //关闭流变化监听订阅消息   playStreamCmd 中开启的监听
                HookKey hookKey = HookKeyFactory.onStreamChanged("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerVo.getId());
                mediaHookSubscribe.removeSubscribe(hookKey);
            }
        });

        HookEvent hookEvent = (media, response)->{
            OnStreamChangedHookVo vo = (OnStreamChangedHookVo) response;
            log.info("收到回放订阅消息： " + JSONUtil.toJsonStr(response));
            dynamicTask.stop(timeOutTaskKey);
            StreamInfo streamInfo = new StreamInfo(mediaServerVo, "rtp", vo.getStream(), vo.getTracks(), null, null, deviceVo.getDeviceId(), channelId);
            streamInfo.setStartTime(startTime);
            streamInfo.setEndTime(endTime);
            InviteInfo invite = inviteStreamManager.getInviteInfo(VideoStreamType.playback, deviceVo.getDeviceId(), channelId,vo.getStream(),null);
            if (invite != null) {
                invite.setStatus(InviteSessionStatus.ok);
                invite.setStreamInfo(streamInfo);
                inviteStreamManager.updateInviteInfo(invite);
            }
            callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
            inviteStreamManager.call(VideoStreamType.playback, deviceVo.getDeviceId(), channelId, vo.getStream(),
                    InviteErrorCode.SUCCESS.getCode(),
                    InviteErrorCode.SUCCESS.getMsg(),
                    streamInfo);
            log.info("[录像回放] 成功 deviceId: {}, channelId: {},  开始时间: {}, 结束时间： {}", deviceVo.getDeviceId(), channelId, startTime, endTime);
        };
        try {
            sipCommander.playbackStreamCmd(sipServer, mediaServerVo,ssrcInfo, deviceVo,channelId,startTime,endTime,videoProperties.getAutoApplyPlay(),hookEvent,(ok)->{
                inviteOKHandler(sipServer,mediaServerVo,deviceVo,channelId,ssrcInfo,inviteInfo,ok,timeOutTaskKey,callback);
            },(error)->{
                log.info("[录像回放] 失败，{} {}", error.getStatusCode(), error.getMsg());
                dynamicTask.stop(timeOutTaskKey);
                ssrcConfigManager.releaseSsrc(mediaServerVo.getId(),ssrcInfo.getSsrc());
                ssrcTransactionManager.remove(deviceVo.getDeviceId(),channelId,ssrcInfo.getStream());
                MediaClient.closeRtpServer(mediaServerVo,ssrcInfo.getStream());
                callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_ERROR.getCode(), String.format("回放失败， 错误码： %s, %s", error.getStatusCode(), error.getMsg()), null);
                inviteStreamManager.removeInviteInfo(inviteInfo);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            //发送异常处理
            log.error("[命令发送失败] 回放点播消息: {}", e.getMessage());
            dynamicTask.stop(timeOutTaskKey);
            ssrcConfigManager.releaseSsrc(mediaServerVo.getId(),ssrcInfo.getSsrc());
            ssrcTransactionManager.remove(deviceVo.getDeviceId(),channelId,ssrcInfo.getStream());
            MediaClient.closeRtpServer(mediaServerVo,ssrcInfo.getStream());
            callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_ERROR.getCode(), String.format("回放失败， 错误码： %s, %s", InviteErrorCode.ERROR_FOR_SIGNALLING_ERROR.getCode(), InviteErrorCode.ERROR_FOR_SIGNALLING_ERROR.getMsg()), null);
            inviteStreamManager.removeInviteInfo(inviteInfo);
        }
    }

    /**
     * 下载方法
     */
    public SSRCInfo download(SipServer sipServer,MediaServerVo mediaServerVo,DeviceVo device, String channelId, String ssrc,String startTime, String endTime, int downloadSpeed, InviteErrorCallback<Object> callback){
        SSRCInfo ssrcInfo = MediaClient.openRTPServer(mediaServerVo, null, ssrc, device.getSsrcCheck() == ConstEnum.Flag.YES.getValue(), true, 0,false,device.getStreamMode());
        if(ssrcInfo == null){
            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(), null);
            return null;
        }
        download(sipServer,mediaServerVo,ssrcInfo,device,channelId,startTime,endTime,downloadSpeed,callback);
        return ssrcInfo;
    }

    /**
     * 下载
     */
    public void download(SipServer sipServer, MediaServerVo mediaServerVo, SSRCInfo ssrcInfo, DeviceVo deviceVo, String channelId, String startTime, String endTime, int downloadSpeed, InviteErrorCallback<Object> callback){
        if (mediaServerVo == null || ssrcInfo == null) {
            callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(), InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(), null);
            return;
        }
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        log.info("[录像下载] deviceId: {}, channelId: {},收流端口： {}, 收流模式：{}, SSRC: {}, SSRC校验：{}", deviceVo.getDeviceId(), channelId, ssrcInfo.getPort(), deviceVo.getStreamMode(), ssrcInfo.getSsrc(), deviceVo.getSsrcCheck());
        InviteInfo inviteInfo = new InviteInfo(ObjectUtil.defaultIfNull(cn.hutool.extra.spring.SpringUtil.getBean(CurrentUserProvider.class).getUserId(), VideoConstant.DEFAULT_DOWNLOAD_USER),deviceVo.getDeviceId(), channelId, ssrcInfo.getStream(), ssrcInfo, mediaServerVo.getSdpIp(), ssrcInfo.getPort(), deviceVo.getStreamMode(), VideoStreamType.download, InviteSessionStatus.ready);
        inviteStreamManager.updateInviteInfo(inviteInfo);
        String timeOutTaskKey = RandomUtil.randomString(32);
        dynamicTask.startDelay(timeOutTaskKey,videoProperties.getPlayTimeout(),()->{
            log.warn(String.format("录像下载请求超时，deviceId：%s ，channelId：%s", deviceVo.getDeviceId(), channelId));
            inviteStreamManager.removeInviteInfo(inviteInfo);
            callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getMsg(), null);
            try {
                sipCommander.streamByeCmd(sipServer,deviceVo, channelId, ssrcInfo.getStream(),null,null,null,null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                log.error("[录像流]录像下载请求超时， 发送BYE失败 {}", e.getMessage());
            } catch (SsrcTransactionNotFoundException e) {
                ssrcConfigManager.releaseSsrc(mediaServerVo.getId(), ssrcInfo.getSsrc());
                MediaClient.closeRtpServer(mediaServerVo, ssrcInfo.getStream());
                ssrcTransactionManager.remove(deviceVo.getDeviceId(), channelId, ssrcInfo.getStream());
            }
        });
        try {
            sipCommander.downloadStreamCmd(sipServer,mediaServerVo,ssrcInfo,deviceVo,channelId,startTime,endTime,downloadSpeed,videoProperties.getSeniorSdp(),(media,res)->{
                OnStreamChangedHookVo vo = (OnStreamChangedHookVo) res;
                log.info("[录像下载]收到订阅消息： " + vo.getCallId());
                dynamicTask.stop(timeOutTaskKey);
                StreamInfo streamInfo = new StreamInfo(mediaServerVo, "rtp", vo.getStream(), vo.getTracks(), null, null, deviceVo.getDeviceId(), channelId);
                streamInfo.setStartTime(startTime);
                streamInfo.setEndTime(endTime);
                streamInfo.setProgress(0);
                InviteInfo invite = inviteStreamManager.getInviteInfo(VideoStreamType.download, deviceVo.getDeviceId(), channelId,streamInfo.getStream(),null);
                if (invite != null) {
                    log.info("[录像下载] 更新invite消息中的stream信息");
                    invite.setStatus(InviteSessionStatus.ok);
                    invite.setStreamInfo(streamInfo);
                    inviteStreamManager.updateInviteInfo(invite);
                }
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                inviteStreamManager.call(VideoStreamType.download, deviceVo.getDeviceId(), channelId, null,
                        InviteErrorCode.SUCCESS.getCode(),
                        InviteErrorCode.SUCCESS.getMsg(),
                        streamInfo);
            },ok->{
                inviteOKHandler(sipServer,mediaServerVo,deviceVo,channelId,ssrcInfo,inviteInfo,ok,timeOutTaskKey,callback);
            },error->{
                dynamicTask.stop(timeOutTaskKey);
                ssrcTransactionManager.remove(deviceVo.getDeviceId(), channelId, ssrcInfo.getStream());
                callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(), String.format("录像下载失败， 错误码： %s, %s", error.getStatusCode(), error.getMsg()), null);
                inviteStreamManager.removeInviteInfo(inviteInfo);
            });
        }catch (InvalidArgumentException | SipException | ParseException e){
            log.error("[命令发送失败] 录像下载: {}", e.getMessage());
            dynamicTask.stop(timeOutTaskKey);
            ssrcTransactionManager.remove(deviceVo.getDeviceId(), channelId, ssrcInfo.getStream());
            callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(), String.format("录像下载失败， 错误码： %s, %s", InviteErrorCode.ERROR_FOR_SIGNALLING_ERROR.getCode(), InviteErrorCode.ERROR_FOR_SIGNALLING_ERROR.getMsg()), null);
            inviteStreamManager.removeInviteInfo(inviteInfo);
        }
    }

    private void inviteOKHandler(SipServer sipServer,MediaServerVo mediaServerVo,DeviceVo deviceVo, String channelId, SSRCInfo ssrcInfo, InviteInfo inviteInfo, EventResult okEvent,String timeOutTaskKey, InviteErrorCallback<Object> callback){
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();

        inviteInfo.setStatus(InviteSessionStatus.ok);
        //发送成功后操作
        ResponseEvent responseEvent = (ResponseEvent) okEvent.getEvent();
        String contentString = new String(responseEvent.getResponse().getRawContent());
        String ssrcInResponse = SipUtils.getSsrcFromSdp(contentString);
        // 检查是否有y字段
        if (ssrcInResponse != null) {
            // 查询到ssrc不一致且开启了ssrc校验则需要针对处理
            if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
                if(mediaServerVo.getRtpEnable()==ConstEnum.Flag.NO.getValue() && deviceVo.getStreamMode().equals(StreamModeType.TCP_ACTIVE.getValue())){
                    log.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                }else if (deviceVo.getStreamMode().equals(StreamModeType.TCP_ACTIVE.getValue())) {
                    tcpActiveHandler(deviceVo,channelId,contentString,mediaServerVo,ssrcInfo,timeOutTaskKey,callback);
                    inviteStreamManager.updateInviteInfo(inviteInfo);
                }
                return;
            }
            log.info("[invite 200] 收到, 发现下级自定义了ssrc: {}", ssrcInResponse);
            //单端口模式streamId也有变化，需要重新设置监听
            if(mediaServerVo.getRtpEnable() == ConstEnum.Flag.NO.getValue()){
                SsrcTransaction paramOne = ssrcTransactionManager.getParamOne(inviteInfo.getDeviceId(), inviteInfo.getChannelId(), null, inviteInfo.getStream(),null);
                ssrcTransactionManager.remove(inviteInfo.getDeviceId(), inviteInfo.getChannelId(),inviteInfo.getStream());
                inviteStreamManager.updateInviteInfoForSSRC(inviteInfo,ssrcInResponse);
                ssrcTransactionManager.put(inviteInfo.getDeviceId(), inviteInfo.getChannelId(),paramOne.getCallId(),"rtp",inviteInfo.getStream(),ssrcInResponse,mediaServerVo.getId(),(SIPResponse) responseEvent.getResponse(),paramOne.getType());
                return;
            }
            //当前服务已使用相同的 下级 ssrc 时
            log.info("[Invite 200OK] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);
            ssrcConfigManager.releaseSsrc(mediaServerVo.getId(),ssrcInfo.getSsrc());
            // 更新ssrc
            MediaRestResult result = MediaClient.updateRtpServerSsrc(mediaServerVo, ssrcInfo.getStream(), ssrcInResponse);
            if(result == null || result.getCode() != RespCode.CODE_0.getValue()){
                try {
                    log.warn("[Invite 200OK] 更新ssrc失败，停止点播 {}/{}", deviceVo.getDeviceId(), channelId);
                    sipCommander.streamByeCmd(sipServer,deviceVo, channelId, ssrcInfo.getStream(), null,null,null,null);
                } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                    log.error("[命令发送失败] 停止点播， 发送BYE: {}", e.getMessage());
                }
                dynamicTask.stop(timeOutTaskKey);
                ssrcTransactionManager.remove(deviceVo.getDeviceId(),channelId,ssrcInfo.getStream());
                callback.run(InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                        "下级自定义了ssrc,重新设置收流信息失败", null);
                inviteStreamManager.call(VideoStreamType.play, deviceVo.getDeviceId(), channelId, null,
                        InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                        "下级自定义了ssrc,重新设置收流信息失败", null);
                return;
            }
            ssrcInfo.setSsrc(ssrcInResponse);
            inviteInfo.setSsrcInfo(ssrcInfo);
            inviteInfo.setStream(ssrcInfo.getStream());
            if (deviceVo.getStreamMode().equals(StreamModeType.TCP_ACTIVE.getValue())) {
                tcpActiveHandler(deviceVo,channelId,contentString,mediaServerVo,ssrcInfo,timeOutTaskKey,callback);
            }
            inviteStreamManager.updateInviteInfo(inviteInfo);
        }
    }

    private void tcpActiveHandler(DeviceVo deviceVo,String channelId,String contentString,MediaServerVo mediaServerVo,SSRCInfo ssrcInfo,String timeOutTaskKey, InviteErrorCallback<Object> callback){
        if (!deviceVo.getStreamMode().equals(StreamModeType.TCP_ACTIVE.getValue())) {
            return;
        }
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        try {
            Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
            SessionDescription sdp = gb28181Sdp.getBaseSdb();
            int port = -1;
            Vector mediaDescriptions = sdp.getMediaDescriptions(true);
            for (Object description : mediaDescriptions) {
                MediaDescription mediaDescription = (MediaDescription) description;
                Media media = mediaDescription.getMedia();
                Vector mediaFormats = media.getMediaFormats(false);
                if (mediaFormats.contains("96")) {
                    port = media.getMediaPort();
                    break;
                }
            }
            log.info("[TCP主动连接对方] deviceId: {}, channelId: {}, 连接对方的地址：{}:{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", deviceVo.getDeviceId(), channelId, sdp.getConnection().getAddress(), port, deviceVo.getStreamMode(), ssrcInfo.getSsrc(), deviceVo.getSsrcCheck());
            MediaRestResult result = MediaClient.connectRtpServer(mediaServerVo, sdp.getConnection().getAddress(), port, ssrcInfo.getStream());
            log.info("[TCP主动连接对方] 结果： {}", result);
        } catch (SdpException e) {
            log.error("[TCP主动连接对方] deviceId: {}, channelId: {}, 解析200OK的SDP信息失败", deviceVo.getDeviceId(), channelId, e);
            dynamicTask.stop(timeOutTaskKey);
            MediaClient.closeRtpServer(mediaServerVo,ssrcInfo.getStream());
            // 释放ssrc
            ssrcConfigManager.releaseSsrc(mediaServerVo.getId(), ssrcInfo.getSsrc());
            ssrcTransactionManager.remove(deviceVo.getDeviceId(), channelId, ssrcInfo.getStream());
            callback.run(InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
            inviteStreamManager.call(VideoStreamType.play, deviceVo.getDeviceId(), channelId, null,
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
        }
    }
}
