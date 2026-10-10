package cn.com.nla.common.video.core.sip.cmd;


import cn.com.nla.common.video.basic.vo.sip.RecordInfo;
import cn.com.nla.common.video.basic.vo.sip.SendRtp;
import cn.com.nla.common.video.basic.vo.video.*;
import cn.com.nla.common.video.core.demo.NotifySubscribeInfo;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeEvent;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.header.WWWAuthenticateHeader;
import java.text.ParseException;
import java.util.List;

public interface SIPCommanderForPlatform {

    /**
     * 向上级平台注册
     * @param parentPlatformVo
     * @return
     */
    void register(SipServer sipServer, ParentPlatformVo parentPlatformVo, WWWAuthenticateHeader www, boolean isRegister, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException;
    /**
     * 向上级平台注销
     * @param parentPlatformVo
     * @return
     */
    void unregister(SipServer sipServer, ParentPlatformVo parentPlatformVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException;


    /**
     * 向上级平发送心跳信息
     * @param parentPlatformVo
     * @return callId(作为接受回复的判定)
     */
    String keepalive(SipServer sipServer, ParentPlatformVo parentPlatformVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException;


    /**
     * 向上级回复通道信息
     * @param channel 通道信息
     * @param parentPlatformVo 平台信息
     * @param sn
     * @param fromTag
     * @param size
     * @return
     */
    void catalogQuery(SipServer sipServer, DeviceChannelVo channel, ParentPlatformVo parentPlatformVo, String sn, String fromTag, int size, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException;
    void catalogQuery(SipServer sipServer, List<DeviceChannelVo> channels, ParentPlatformVo parentPlatformVo, String sn, String fromTag, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException;

    /**
     * 向上级回复DeviceInfo查询信息
     * @param parentPlatformVo 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    void deviceInfoResponse(SipServer sipServer, ParentPlatformVo parentPlatformVo, DeviceVo deviceVo, String sn, String fromTag, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 向上级回复DeviceStatus查询信息
     * @param parentPlatformVo 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    void deviceStatusResponse(SipServer sipServer, ParentPlatformVo parentPlatformVo, String channelId, String sn, String fromTag, int status, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 向上级回复移动位置订阅消息
     * @param parentPlatformVo 平台信息
     * @param deviceMobilePositionVo GPS信息
     * @return
     */
    void sendNotifyMobilePosition(SipServer sipServer, ParentPlatformVo parentPlatformVo, DeviceMobilePositionVo deviceMobilePositionVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException;

    /**
     * 向上级发送报警消息
     * @param parentPlatformVo 平台信息
     * @param deviceAlarmVo 报警信息信息
     * @return
     */
    void sendAlarmMessage(SipServer sipServer, ParentPlatformVo parentPlatformVo, DeviceAlarmVo deviceAlarmVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 回复catalog事件-增加/更新
     * @param parentPlatformVo
     * @param deviceChannelVos
     */
    void sendNotifyForCatalogAddOrUpdate(SipServer sipServer, String type, ParentPlatformVo parentPlatformVo, List<DeviceChannelVo> deviceChannelVos, NotifySubscribeInfo catalogSubscribe, Integer index, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException;

    /**
     * 回复catalog事件-删除
     * @param parentPlatformVo
     * @param deviceChannelVos
     */
    void sendNotifyForCatalogOther(SipServer sipServer, String type, ParentPlatformVo parentPlatformVo, List<DeviceChannelVo> deviceChannelVos, NotifySubscribeInfo catalogSubscribe, Integer index, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException;

    /**
     * 回复recordInfo
     * @param deviceChannelVo 通道信息
     * @param parentPlatformVo 平台信息
     * @param fromTag fromTag
     * @param recordInfo 录像信息
     */
    void recordInfo(SipServer sipServer, DeviceChannelVo deviceChannelVo, ParentPlatformVo parentPlatformVo, String fromTag, RecordInfo recordInfo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 录像播放推送完成时发送MediaStatus消息
     * @param platform
     * @param sendRtpItem
     * @return
     */
    void sendMediaStatusNotify(SipServer sipServer, ParentPlatformVo platform, SendRtp sendRtpItem, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 向发起点播的上级回复bye
     * 点播时检查是否开启过
     * @param platform 平台信息
     */
    void streamByeCmd(SipServer sipServer, ParentPlatformVo platform, SendRtp sendRtpItem, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException;
}
