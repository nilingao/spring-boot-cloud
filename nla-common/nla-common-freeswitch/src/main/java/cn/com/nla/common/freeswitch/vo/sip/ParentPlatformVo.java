package cn.com.nla.common.freeswitch.vo.sip;

import cn.com.nla.common.freeswitch.vo.BaseVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
    * 平台信息
    */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ParentPlatformVo extends BaseVo {
    /**
     * 是否启用
     */
    private Integer enable;

    /**
     * 名称
     */
    private String name;

    /**
     * SIP服务国标编码
     */
    private String serverGbId;

    /**
     * SIP服务国标域
     */
    private String serverGbDomain;

    /**
     * SIP服务IP
     */
    private String serverIp;

    /**
     * SIP服务端口
     */
    private Integer serverPort;

    /**
     * 设备国标编号
     */
    private String deviceGbId;

    /**
     * 设备ip
     */
    private String deviceIp;

    /**
     * 设备端口
     */
    private Integer devicePort;

    /**
     * SIP认证用户名(默认使用设备国标编号)
     */
    private String username;

    /**
     * SIP认证密码
     */
    private String password;

    /**
     * 注册周期 (秒)
     */
    private Integer expires;

    /**
     * 心跳周期(秒)
     */
    private Integer keepTimeout;

    /**
     * 传输协议 1.UDP 2.TCP
     */
    private Integer transport;

    /**
     * 字符集, 1.UTF-8 2.GB2312
     */
    private Integer characterSet;

    /**
     * 默认目录Id,自动添加的通道多放在这个目录下
     */
    private String catalogId;

    /**
     * 目录分组-每次向上级发送通道信息时单个包携带的通道数量，取值1,2,4,8
     */
    private Integer catalogGroup;

    /**
     * 是否允许云台控制
     */
    private Integer ptz;

    /**
     * RTCP流保活
     */
    private Integer rtcp;

    /**
     * 在线状态
     */
    private Integer status;

    /**
     * 点播未推流的设备时是否使用redis通知拉起
     */
    private Integer startOfflinePush;

    /**
     * 行政区划
     */
    private String administrativeDivision;

    /**
     * 树类型 国标规定了两种树的展现方式 216.行政区划：CivilCode 215.业务分组:BusinessGroup
     */
    private Integer treeType;

    /**
     * 是否作为消息通道 1.是 0.否
     */
    private Integer asMessageChannel;
}