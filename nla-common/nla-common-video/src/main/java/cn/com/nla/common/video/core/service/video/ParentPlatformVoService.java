package cn.com.nla.common.video.core.service.video;

import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.vo.media.MediaRestResult;
import cn.com.nla.common.video.basic.vo.sip.Address;
import cn.com.nla.common.video.basic.vo.sip.ChannelSourceInfo;
import cn.com.nla.common.video.basic.vo.sip.SendRtp;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.demo.SipTransactionInfo;
import cn.com.nla.common.video.core.media.client.MediaClient;
import cn.com.nla.common.video.core.model.EventResult;
import cn.com.nla.common.video.core.model.RestResultEvent;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.SendRtpManager;
import cn.com.nla.common.video.core.redis.impl.SipTransactionManager;
import cn.com.nla.common.video.core.redis.impl.SsrcConfigManager;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeEvent;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommanderForPlatform;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

@Slf4j
public abstract class ParentPlatformVoService {

    @Autowired
    private SipServer sipServer;
    @Autowired
    private DynamicTask dynamicTask;
    @Autowired
    private SIPCommanderForPlatform sipCommanderForPlatform;

    public abstract ParentPlatformVo getParentPlatformByServerGbId(String platformGbId);

    public abstract List<ParentPlatformVo> getParentPlatformByDeviceGbId(String deviceGbId);

    public abstract List<ParentPlatformVo> getParentPlatformByServerGbIdList(List<String> platformGbId);
    public abstract int updateParentPlatformStatus(String platformGbID, boolean online);
    /**
     * 获取所有已启用的平台
     */
    public abstract List<ParentPlatformVo> queryEnableParentPlatformList();
    /**
     * 获取所有已启用的平台
     */
    public abstract List<ParentPlatformVo> queryEnablePlatformListWithAsMessageChannel();
    /**
     * 获取国标通道关联设备通道 与 国标流
     */
    public abstract List<ChannelSourceInfo> getChannelSource(String serverGbId, String channelId);

    public abstract List<ParentPlatformVo> queryPlatFormListForGBWithGBId(String channelId, List<String> allPlatformId);

    public abstract List<ParentPlatformVo> queryPlatFormListForStreamWithGBId(String app, String stream, List<String> allPlatformId);

    public abstract void delPlatformGbStream(String app,String stream);

    public abstract List<ParentPlatformVo> findPlatformGbChannel(String channelId);

    /**
     * 向上级平台注册
     */
    public void login(ParentPlatformVo parentPlatformVo){
        RedisService.getRegisterServerManager().putPlatform(parentPlatformVo.getServerGbId(),parentPlatformVo.getKeepTimeout()+ VideoConstant.DELAY_TIME, Address.builder().gbId(parentPlatformVo.getServerGbId()).ip(sipServer.getAdvertisedIp(null)).port(sipServer.getSipConfigProperties().getPort()).build());
        register(parentPlatformVo,error->{
            log.info("[国标级联] {}, 发起注册，失败", parentPlatformVo.getServerGbId());
        });
    }


    private void register(ParentPlatformVo parentPlatformVo, SipSubscribeEvent errorEvent){
        SipTransactionInfo sipTransactionInfo = RedisService.getSipTransactionManager().findParentPlatform(parentPlatformVo.getServerGbId());
        if(sipTransactionInfo == null){
            sipTransactionInfo = new SipTransactionInfo();
            RedisService.getSipTransactionManager().putParentPlatform(parentPlatformVo.getServerGbId(),sipTransactionInfo);
        }
        //如果注册三次后还未注册成功则取消注册
        if(sipTransactionInfo.getRegisterAliveReply() > 3){
            sipTransactionInfo.setRegisterAliveReply(0);
            sipTransactionInfo.setKeepAliveReply(0);
            RedisService.getSipTransactionManager().putParentPlatform(parentPlatformVo.getServerGbId(),sipTransactionInfo);
            errorEvent.response(new EventResult<RestResultEvent>(new RestResultEvent(RespCode.CODE_2.getValue(),String.format("[国标级联]：%s, 平台注册三次尚未成功则放弃",parentPlatformVo.getServerGbId()))));
            offline(parentPlatformVo);
            return;
        }
        sipTransactionInfo.setKeepAliveReply(0);
        sipTransactionInfo.setRegisterAliveReply(sipTransactionInfo.getRegisterAliveReply() +1);
        RedisService.getSipTransactionManager().putParentPlatform(parentPlatformVo.getServerGbId(),sipTransactionInfo);
        try {
            sipCommanderForPlatform.register(sipServer, parentPlatformVo,null, true, null, error->{
                offline(parentPlatformVo);
                errorEvent.response(error);
            });
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[命令发送失败] 国标级联注册: {}", e.getMessage());
        }
    }

    /**
     * 向上级平台注销
     */
    public void unregister(ParentPlatformVo parentPlatformVo,SipSubscribeEvent okEvent,SipSubscribeEvent errorEvent){
        try {
            sipCommanderForPlatform.unregister(sipServer, parentPlatformVo, (ok->{
                if(okEvent != null){
                    okEvent.response(ok);
                }
            }), (eventResult)->{
                log.info("[国标级联] {}, 发起注销，失败", parentPlatformVo.getServerGbId());
                if(errorEvent != null){
                    errorEvent.response(eventResult);
                }
            });
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[命令发送失败] 国标级联注销: {}", e.getMessage());
            offline(parentPlatformVo);
        }
    }

    public void online(ParentPlatformVo parentPlatformVo, SipTransactionInfo sipTransactionInfo){
        log.info("[国标级联]：{}, 平台上线", parentPlatformVo.getServerGbId());
        if(sipTransactionInfo != null){
            SipTransactionManager sipTransactionManager = RedisService.getSipTransactionManager();
            sipTransactionManager.putParentPlatform(parentPlatformVo.getServerGbId(),sipTransactionInfo);
        }
        //设置上线
        this.updateParentPlatformStatus(parentPlatformVo.getServerGbId(), true);
        //添加注册任务
        this.registerTask(parentPlatformVo,false);
        //添加保活任务
        this.keepaliveTask(parentPlatformVo,false);
        RedisService.getRegisterServerManager().putPlatform(parentPlatformVo.getServerGbId(),parentPlatformVo.getKeepTimeout()+ VideoConstant.DELAY_TIME, Address.builder().gbId(parentPlatformVo.getServerGbId()).ip(sipServer.getAdvertisedIp(null)).port(sipServer.getSipConfigProperties().getPort()).build());
    }

    public void offline(ParentPlatformVo parentPlatformVo){
        log.info("[平台离线]：{}", parentPlatformVo.getServerGbId());
        this.updateParentPlatformStatus(parentPlatformVo.getServerGbId(), false);
        // 停止所有推流
        log.info("[平台离线] {}, 停止所有推流", parentPlatformVo.getServerGbId());
        stopAllPush(parentPlatformVo.getServerGbId());
        //清除注册定时
        this.registerTask(parentPlatformVo,true);
        //清除心跳定时
        this.keepaliveTask(parentPlatformVo,true);
        // 停止目录订阅回复
        log.info("[平台离线] {}, 停止订阅回复", parentPlatformVo.getServerGbId());
        RedisService.getPlatformNotifySubscribeManager().removeAllSubscribe(parentPlatformVo.getServerGbId());
        RedisService.getRegisterServerManager().delPlatform(parentPlatformVo.getServerGbId());
    }

    /**
     * 上级平台注册任务（到注册时间后重新注册）
     */
    private void registerTask(ParentPlatformVo parentPlatformVo, boolean isClose){
        // 设置超时重发， 后续从底层支持消息重发
        String key = VideoConstant.PLATFORM_REGISTER_TASK_CATCH_PREFIX + parentPlatformVo.getServerGbId();
        if(isClose){
            dynamicTask.stop(key);
            return;
        }
        if (dynamicTask.isAlive(key)) {
            return;
        }
        int expires= Math.max(parentPlatformVo.getExpires(),20)- VideoConstant.DELAY_TIME;
        dynamicTask.startCron(key, expires, expires,()->{
            log.info("[国标级联] 平台：{}注册即将到期，开始续订", parentPlatformVo.getServerGbId());
            register(parentPlatformVo,error -> {
                log.error(String.format("[国标级联] 平台：{}注册即将到期，开始续订失败 code : %s,msg : %s",error.getStatusCode(),error.getMsg()));
                dynamicTask.stop(key);
            });
        });
    }

    /**
     * 上级平台保活任务（到保活时间后进行保活）
     */
    private void keepaliveTask(ParentPlatformVo parentPlatformVo, boolean isClose){
        final String keepaliveTaskKey = VideoConstant.PLATFORM_KEEPALIVE_PREFIX + parentPlatformVo.getServerGbId();
        if(isClose){
            dynamicTask.stop(keepaliveTaskKey);
            return;
        }
        if (dynamicTask.isAlive(keepaliveTaskKey)) {
            return;
        }
        log.info("[国标级联]：{}, 定时上级平台心跳保活任务", parentPlatformVo.getServerGbId());
        dynamicTask.startCron(keepaliveTaskKey,1, parentPlatformVo.getKeepTimeout(),()->{
            try {
                SipTransactionInfo parentPlatform = RedisService.getSipTransactionManager().findParentPlatform(parentPlatformVo.getServerGbId());
                if(parentPlatform == null){
                    log.error("[国标级联]：{},心跳时未发现,上级平台注册信息",parentPlatformVo.getServerGbId());
                    return;
                }else if(parentPlatform.getKeepAliveReply() > 3){//心跳发送三次后如果还未回应则 平台下线
                    log.error("[国标级联]：{},心跳发送三次后如 平台还未回应,平台下线",parentPlatformVo.getServerGbId());
                    offline(parentPlatformVo);
                    return;
                }
                parentPlatform.setKeepAliveReply(parentPlatform.getKeepAliveReply()+1);
                RedisService.getSipTransactionManager().putParentPlatform(parentPlatformVo.getServerGbId(),parentPlatform);
                sipCommanderForPlatform.keepalive(sipServer, parentPlatformVo, ok->{
                    log.info("[国标级联]：{}, 定时上级平台心跳保活任务成功", parentPlatformVo.getServerGbId());
                    if(parentPlatform.getKeepAliveReply() > 1){
                        parentPlatform.setKeepAliveReply(0);
                        RedisService.getSipTransactionManager().putParentPlatform(parentPlatformVo.getServerGbId(),parentPlatform);
                    }
                    RedisService.getRegisterServerManager().putPlatform(parentPlatformVo.getServerGbId(),parentPlatformVo.getKeepTimeout()+ VideoConstant.DELAY_TIME, Address.builder().gbId(parentPlatformVo.getServerGbId()).ip(sipServer.getAdvertisedIp(null)).port(sipServer.getSipConfigProperties().getPort()).build());
                },error->{
                    register(parentPlatformVo, errorEvent -> {
                        log.info("[国标级联] {}，心跳超时后再次发起注册仍然失败，开始定时发起注册，间隔为1分钟", parentPlatformVo.getServerGbId());
                        dynamicTask.stop(keepaliveTaskKey);
                    });
                });

            }catch (SipException | InvalidArgumentException | ParseException e){
                log.error("[命令发送失败] 国标级联 发送心跳: {}", e.getMessage());
            }
        });
    }

    private void stopAllPush(String platformId) {
        SendRtpManager sendRtpManager = RedisService.getSendRtpManager();
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        List<SendRtp> sendRtpItems = sendRtpManager.querySendRTPServer(platformId);
        if (sendRtpItems != null && sendRtpItems.size() > 0) {
            for (SendRtp sendRtpItem : sendRtpItems) {
                ssrcConfigManager.releaseSsrc(sendRtpItem.getMediaServerId(),sendRtpItem.getSsrc());
                sendRtpManager.deleteSendRTPServer(sendRtpItem.getPlatformId(), sendRtpItem.getChannelId(), null, null);
                MediaServerVo mediaServerVo = mediaServerVoService.findOnLineMediaServerId(sendRtpItem.getMediaServerId());
                MediaRestResult mediaRestResult = MediaClient.stopSendRtp(mediaServerVo, "__defaultVhost__", sendRtpItem.getApp(), sendRtpItem.getStreamId(), sendRtpItem.getSsrc());
                if (mediaRestResult == null) {
                    log.error("[停止RTP推流] 失败: 请检查ZLM服务");
                } else if (mediaRestResult.getCode() == 0) {
                    log.info("[停止RTP推流] 成功");
                } else {
                    log.error("[停止RTP推流] 失败: {}, 参数：{}->\r\n{}",mediaRestResult.getMsg(), JSONUtil.toJsonStr(sendRtpItem), JSONUtil.toJsonStr(mediaRestResult));
                }
            }
        }
    }

}
