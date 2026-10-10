package cn.com.nla.common.video.core.sip.cmd;

import cn.com.nla.common.video.basic.enums.VideoStreamType;
import cn.com.nla.common.video.basic.exception.SsrcTransactionNotFoundException;
import cn.com.nla.common.video.basic.vo.sip.SSRCInfo;
import cn.com.nla.common.video.basic.vo.video.DeviceAlarmVo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.core.demo.StreamInfo;
import cn.com.nla.common.video.core.redis.subscribe.media.HookEvent;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeEvent;
import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * @description:设备能力接口，用于定义设备的控制、查询能力
 * @author TZY
 * @date:   2020年5月3日 下午9:16:34
 */
public interface SIPCommander {

	/**
	 * 云台缩放控制
	 *
	 * @param deviceVo  控制设备
	 * @param channelId  预览通道
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
	 */
	void ptzZoomCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, int inOut, int moveSpeed, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * 云台控制，支持方向与缩放控制
	 *
	 * @param deviceVo  控制设备
	 * @param channelId  预览通道
	 * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
     * @param moveSpeed  镜头移动速度
     * @param zoomSpeed  镜头缩放速度
	 */
	void ptzCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 前端控制，包括PTZ指令、FI指令、预置位指令、巡航指令、扫描指令和辅助开关指令
	 *
	 * @param deviceVo  		控制设备
	 * @param channelId		预览通道
	 * @param cmdCode		指令码
     * @param parameter1	数据1
     * @param parameter2	数据2
     * @param combineCode2	组合码2
	 */
	void frontEndCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, int cmdCode, int parameter1, int parameter2, int combineCode2, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException;

	/**
	 * 前端控制指令（用于转发上级指令）
	 * @param deviceVo		控制设备
	 * @param channelId		预览通道
	 * @param cmdString		前端控制指令串
	 */
	void fronEndCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, String cmdString, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 请求预览视频流
	 * @param deviceVo  视频设备
	 * @param channelId  预览通道
	 */
	void playStreamCmd(SipServer sipServer, MediaServerVo mediaServerVoItem, SSRCInfo ssrcInfo, DeviceVo deviceVo, String channelId, boolean isSeniorSdp, HookEvent hookEvent, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 请求回放视频流
	 *
	 * @param deviceVo  视频设备
	 * @param channelId  预览通道
	 * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
	 */
	void playbackStreamCmd(SipServer sipServer, MediaServerVo mediaServerVoItem, SSRCInfo ssrcInf, DeviceVo deviceVo, String channelId, String startTime, String endTime, boolean isSeniorSdp, HookEvent hookEvent, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 请求历史媒体下载
	 *
	 * @param deviceVo  视频设备
	 * @param channelId  预览通道
	 * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param downloadSpeed 下载倍速参数
	 */
	void downloadStreamCmd(SipServer sipServer, MediaServerVo mediaServerVoItem, SSRCInfo ssrcInfo, DeviceVo deviceVo, String channelId, String startTime, String endTime, int downloadSpeed, boolean isSeniorSdp,HookEvent hookEvent, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 视频流停止
	 */
	void streamByeCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, String stream, String callId, VideoStreamType type, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;

	/**
	 * 回放暂停
	 */
	void playPauseCmd(SipServer sipServer, DeviceVo deviceVo, StreamInfo streamInfo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * 回放恢复
	 */
	void playResumeCmd(SipServer sipServer, DeviceVo deviceVo, StreamInfo streamInfo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * 回放拖动播放
	 */
	void playSeekCmd(SipServer sipServer, DeviceVo deviceVo, StreamInfo streamInfo, long seekTime, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * 回放倍速播放
	 */
	void playSpeedCmd(SipServer sipServer, DeviceVo deviceVo, StreamInfo streamInfo, Double speed, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException;

	/**
	 * 回放控制
	 * @param deviceVo
	 * @param streamInfo
	 * @param content
	 */
	void playbackControlCmd(SipServer sipServer, DeviceVo deviceVo, StreamInfo streamInfo, String content, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException;


	/**
	 * 语音广播
	 *
	 * @param deviceVo  视频设备
	 */
	void audioBroadcastCmd(SipServer sipServer, DeviceVo deviceVo,String channelId,SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 音视频录像控制
	 *
	 * @param deviceVo  		视频设备
	 * @param channelId  	预览通道
	 * @param recordCmdStr	录像命令：Record / StopRecord
	 */
	void recordCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, String recordCmdStr, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 远程启动控制命令
	 *
	 * @param deviceVo	视频设备
	 */
	void teleBootCmd(SipServer sipServer, DeviceVo deviceVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 报警布防/撤防命令
	 *
	 * @param deviceVo  	视频设备
	 * @param guardCmdStr "SetGuard"/"ResetGuard"
	 */
	void guardCmd(SipServer sipServer, DeviceVo deviceVo,String channelId, String guardCmdStr, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 报警复位命令
	 *
	 * @param deviceVo		视频设备
	 * @param alarmMethod	报警方式（可选）
	 * @param alarmType		报警类型（可选）
	 */
	void alarmCmd(SipServer sipServer, DeviceVo deviceVo, String alarmMethod, String alarmType, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 强制关键帧命令,设备收到此命令应立刻发送一个IDR帧
	 *
	 * @param deviceVo  视频设备
	 * @param channelId  预览通道
	 */
	void iFrameCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 看守位控制命令
	 *
	 * @param deviceVo		视频设备
	 * @param enabled		看守位使能：1 = 开启，0 = 关闭
	 * @param resetTime		自动归位时间间隔，开启看守位时使用，单位:秒(s)
	 * @param presetIndex	调用预置位编号，开启看守位时使用，取值范围0~255
	 */
	void homePositionCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, String enabled, String resetTime, String presetIndex, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 设备配置命令：basicParam
	 *
	 * @param deviceVo  			视频设备
	 * @param channelId			通道编码（可选）
	 * @param name				设备/通道名称（可选）
	 * @param expiration		注册过期时间（可选）
	 * @param heartBeatInterval	心跳间隔时间（可选）
	 * @param heartBeatCount	心跳超时次数（可选）
	 */
	void deviceBasicConfigCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, String name, String expiration, String heartBeatInterval, String heartBeatCount, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询设备状态
	 *
	 * @param deviceVo 视频设备
	 */
	void deviceStatusQuery(SipServer sipServer, DeviceVo deviceVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询设备信息
	 *
	 * @param deviceVo 视频设备
	 * @return
	 */
	void deviceInfoQuery(SipServer sipServer, DeviceVo deviceVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询目录列表
	 *
	 * @param deviceVo 视频设备
	 */
	void catalogQuery(SipServer sipServer, DeviceVo deviceVo, int sn, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException;

	/**
	 * 查询录像信息
	 *
	 * @param deviceVo 视频设备
	 * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param endTime 结束时间,格式要求：yyyy-MM-dd HH:mm:ss
	 * @param sn
	 */
	void recordInfoQuery(SipServer sipServer, DeviceVo deviceVo, String channelId, String startTime, String endTime, int sn, Integer secrecy, String type, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询报警信息
	 *
	 * @param deviceVo		视频设备
	 * @param startPriority	报警起始级别（可选）
	 * @param endPriority	报警终止级别（可选）
	 * @param alarmMethod	报警方式条件（可选）
	 * @param alarmType		报警类型
	 * @param startTime		报警发生起始时间（可选）
	 * @param endTime		报警发生终止时间（可选）
	 * @return				true = 命令发送成功
	 */
	void alarmInfoQuery(SipServer sipServer, DeviceVo deviceVo, String startPriority, String endPriority, String alarmMethod,
                        String alarmType, String startTime, String endTime, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询设备配置
	 *
	 * @param deviceVo 		视频设备
	 * @param channelId		通道编码（可选）
	 * @param configType	配置类型：
	 */
	void deviceConfigQuery(SipServer sipServer, DeviceVo deviceVo, String channelId, String configType, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询设备预置位置
	 *
	 * @param deviceVo 视频设备
	 */
	void presetQuery(SipServer sipServer, DeviceVo deviceVo, String channelId, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 查询移动设备位置数据
	 *
	 * @param deviceVo 视频设备
	 */
	void mobilePostitionQuery(SipServer sipServer, DeviceVo deviceVo,String channelId ,SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 订阅、取消订阅移动位置
	 *
	 * @param deviceVo	视频设备
	 * @return			true = 命令发送成功
	 */
	SIPRequest mobilePositionSubscribe(SipServer sipServer, DeviceVo deviceVo,String channelId , SIPRequest request, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 订阅、取消订阅目录信息
	 * @param deviceVo		视频设备
	 * @return				true = 命令发送成功
	 */
	SIPRequest catalogSubscribe(SipServer sipServer, DeviceVo deviceVo, SIPRequest request, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 订阅、取消订阅目录信息
	 * @param deviceVo		视频设备
	 * @return				true = 命令发送成功
	 */
	SIPRequest alarmSubscribe(SipServer sipServer, DeviceVo deviceVo,String channelId, SIPRequest request, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;

	/**
	 * 拉框控制命令
	 *
	 * @param deviceVo    控制设备
	 * @param channelId 通道id
	 * @param cmdString 前端控制指令串
	 */
	void dragZoomCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, String cmdString, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;


	/**
	 * 向设备发送报警NOTIFY消息， 用于互联结构下，此时将设备当成一个平级平台看待
	 * @param deviceVo 设备
	 * @param deviceAlarmVo 报警信息信息
	 * @return
	 */
	void sendAlarmMessage(SipServer sipServer, DeviceVo deviceVo, DeviceAlarmVo deviceAlarmVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException;


	public void sendAckMessage(SipServer sipServer, SessionDescription sdp, ResponseEventExt event, SIPResponse response, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException, SdpParseException;
}
