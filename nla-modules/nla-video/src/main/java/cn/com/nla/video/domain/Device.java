package cn.com.nla.video.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.time.LocalDateTime;

/** 设备信息；业务服务与协议接入在阶段 6.5 实现。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_device")
public class Device extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 设备国标编号。 */
    @TableField(value = "`device_id`")
    private String deviceId;

    /** 设备名。 */
    @TableField(value = "`name`")
    private String name;

    /** 生产厂商。 */
    @TableField(value = "`manufacturer`")
    private String manufacturer;

    /** 型号。 */
    @TableField(value = "`model`")
    private String model;

    /** 风格名词。 */
    @TableField(value = "`custom_name`")
    private String customName;

    /** 固件版本。 */
    @TableField(value = "`firmware`")
    private String firmware;

    /** 传输协议 1.UDP 2.TCP。 */
    @TableField(value = "`transport`")
    private Integer transport;

    /** wan地址_ip。 */
    @TableField(value = "`ip`")
    private String ip;

    /** wan地址_port。 */
    @TableField(value = "`port`")
    private Integer port;

    /** wan地址。 */
    @TableField(value = "`host_address`")
    private String hostAddress;

    /** 设备密码。 */
    @TableField(value = "`password`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 收流IP。 */
    @TableField(value = "`sdp_ip`")
    private String sdpIp;

    /** SIP交互IP（设备访问平台的IP）。 */
    @TableField(value = "`local_ip`")
    private String localIp;

    /** 字符集, 1.UTF-8 2.GB2312。 */
    @TableField(value = "`charset`")
    private Integer charset;

    /** 树类型 国标规定了两种树的展现方式 216.行政区划：CivilCode 215.业务分组:BusinessGroup。 */
    @TableField(value = "`tree_type`")
    private Integer treeType;

    /** 地理坐标系， 目前支持 1.WGS84,2.GCJ02。 */
    @TableField(value = "`geo_coord_sys`")
    private Integer geoCoordSys;

    /** 在线状态；保留旧库默认值2，协议业务阶段核定0/1/2含义。 */
    @TableField(value = "`online`")
    private Integer online;

    /** 注册时间。 */
    @TableField(value = "`register_time`")
    private LocalDateTime registerTime;

    /** 心跳时间。 */
    @TableField(value = "`keepalive_time`")
    private LocalDateTime keepaliveTime;

    /** 心跳间隔 (最低25秒)。 */
    @TableField(value = "`heart_beat_interval`")
    private Integer heartBeatInterval;

    /** 心跳间隔 (最低2次)。 */
    @TableField(value = "`heart_beat_count`")
    private Integer heartBeatCount;

    /** 定位功能支持情况。取值:0-不支持;1-支持 GPS定位;2-支持北斗定位(可选,默认取值为0。 */
    @TableField(value = "`position_capability`")
    private Integer positionCapability;

    /** 数据流传输模式 0.UDP:udp传输 1.TCP-PASSIVE：tcp被动模式 2.TCP-ACTIVE：tcp主动模式。 */
    @TableField(value = "`stream_mode`")
    private Integer streamMode;

    /** 注册有效期（单位：秒 默认1天）。 */
    @TableField(value = "`expires`")
    private Integer expires;

    /** 移动设备位置信息上报时间间隔,单位:秒,默认值5 (秒)。 */
    @TableField(value = "`mobile_position_submission_interval`")
    private Integer mobilePositionSubmissionInterval;

    /** 目录订阅周期，0为不订阅 (秒)。 */
    @TableField(value = "`subscribe_cycle_for_catalog`")
    private Integer subscribeCycleForCatalog;

    /** 移动设备位置订阅周期，0为不订阅 (秒)。 */
    @TableField(value = "`subscribe_cycle_for_mobile_position`")
    private Integer subscribeCycleForMobilePosition;

    /** 报警心跳时间订阅周期，0为不订阅 (秒)。 */
    @TableField(value = "`subscribe_cycle_for_alarm`")
    private Integer subscribeCycleForAlarm;

    /** 是否开启ssrc校验，默认关闭，开启可以防止串流。 */
    @TableField(value = "`ssrc_check`")
    private Integer ssrcCheck;

    /** 流媒体编号 默认为null。 */
    @TableField(value = "`media_server_id`")
    private String mediaServerId;

    /** 是否作为消息通道 1.是 0.否。 */
    @TableField(value = "`as_message_channel`")
    private Integer asMessageChannel;

    /** 是否开启超管权限(只有超级管理员才可查看)。 */
    @TableField(value = "`has_administrator`")
    private Integer hasAdministrator;

    /** 是否开启主子码流开关 1.是 0.否。 */
    @TableField(value = "`switch_primary_sub_stream`")
    private Integer switchPrimarySubStream;

    /** 删除标志：0 存在、1 删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
