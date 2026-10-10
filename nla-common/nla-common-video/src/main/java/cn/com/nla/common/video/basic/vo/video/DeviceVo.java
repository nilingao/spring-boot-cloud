package cn.com.nla.common.video.basic.vo.video;

import cn.com.nla.common.video.basic.model.LongIdEntity;
import cn.com.nla.common.video.basic.common.DateFormats;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
    * 设备信息
    */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DeviceVo extends LongIdEntity {
    /**
     * 设备国标编号
     */
    private String deviceId;

    /**
     * 设备名
     */
    private String name;

    /**
     * 生产厂商
     */
    private String manufacturer;

    /**
     * 型号
     */
    private String model;

    /**
     * 风格名词
     */
    private String customName;

    /**
     * 固件版本
     */
    private String firmware;

    /**
     * 传输协议 1.UDP 2.TCP
     */
    private Integer transport;

    /**
     * wan地址_ip
     */
    private String ip;

    /**
     * wan地址_port
     */
    private Integer port;

    /**
     * wan地址
     */
    private String hostAddress;

    /**
     * 设备密码
     */
    private String password;

    /**
     * 收流IP
     */
    private String sdpIp;

    /**
     * SIP交互IP（设备访问平台的IP）
     */
    private String localIp;

    /**
     * 字符集, 1.UTF-8 2.GB2312
     */
    private Integer charset= 2;

    /**
     * 树类型 国标规定了两种树的展现方式 216.行政区划：CivilCode 215.业务分组:BusinessGroup
     */
    private Integer treeType= 215;

    /**
     * 地理坐标系， 目前支持 1.WGS84,2.GCJ02
     */
    private Integer geoCoordSys= 1;

    /**
     * 是否在线，1.为在线，0.为离线
     */
    private Integer online= 0;

    /**
     * 注册时间
     */

    @JsonFormat(pattern =  DateFormats.DATE_TIME_FORMAT)
    private LocalDateTime registerTime;
    /**
     * 心跳时间
     */

    @JsonFormat(pattern =  DateFormats.DATE_TIME_FORMAT)
    private LocalDateTime keepaliveTime;

    /**
     * 心跳间隔
     */
    private Integer heartBeatInterval;

    /**
     * 心跳间隔
     */
    private Integer heartBeatCount;

    /**
     * 定位功能支持情况
     */
    private Integer positionCapability;

    /**
     * 数据流传输模式 0.UDP:udp传输 1.TCP-PASSIVE：tcp被动模式 2.TCP-ACTIVE：tcp主动模式
     */
    private Integer streamMode = 0;

    /**
     * 注册有效期
     */
    private Integer expires;
    /**
     * 目录订阅周期，0为不订阅
     */
    private Integer subscribeCycleForCatalog= 0;

    /**
     * 移动设备位置信息上报时间间隔,单位:秒,默认值5
     */
    private Integer mobilePositionSubmissionInterval= 0;
    /**
     * 移动设备位置订阅周期，0为不订阅
     */
    private Integer subscribeCycleForMobilePosition= 0;

    /**
     * 报警心跳时间订阅周期，0为不订阅
     */
    private Integer subscribeCycleForAlarm= 0;

    /**
     * 是否开启ssrc校验，默认关闭，开启可以防止串流
     */
    private Integer ssrcCheck = 0;

    /**
     * 流媒体编号
     */
    private String mediaServerId;

    /**
     * 是否作为消息通道 1.是 0.否
     */
    private Integer asMessageChannel = 0;

    /**
     * 是否开启超管权限(只有超级管理员才可查看) 1.是 0.否 默认 ：0
     */
    private Integer hasAdministrator;

    /**
     * 开启主子码流切换的开关
     */
    private Integer switchPrimarySubStream;


    public boolean keepalive(){
        if(keepaliveTime == null || heartBeatInterval == null){
            return true;
        }
        LocalDateTime endTime = keepaliveTime.plusSeconds(heartBeatInterval + VideoConstant.DELAY_TIME);
        return endTime.isBefore(LocalDateTime.now());
    }

}