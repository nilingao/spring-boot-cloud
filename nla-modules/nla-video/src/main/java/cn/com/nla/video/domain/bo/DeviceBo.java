package cn.com.nla.video.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.video.domain.Device;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 设备信息输入，不接受审计与删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = Device.class, reverseConvertGenerate = false)
public class DeviceBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 设备国标编号。 */
    @NotBlank(message = "deviceId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "deviceId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String deviceId;

    /** 设备名。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 生产厂商。 */
    @Size(max = 255, message = "manufacturer长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String manufacturer;

    /** 型号。 */
    @Size(max = 255, message = "model长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String model;

    /** 风格名词。 */
    @Size(max = 255, message = "customName长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String customName;

    /** 固件版本。 */
    @Size(max = 255, message = "firmware长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String firmware;

    /** 传输协议 1.UDP 2.TCP。 */
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer transport;

    /** wan地址_ip。 */
    @Size(max = 50, message = "ip长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String ip;

    /** wan地址_port。 */
    @Min(value = 0, message = "port不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer port;

    /** wan地址。 */
    @Size(max = 50, message = "hostAddress长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String hostAddress;

    /** 设备密码。 */
    @Size(max = 255, message = "password长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 收流IP。 */
    @Size(max = 50, message = "sdpIp长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String sdpIp;

    /** SIP交互IP（设备访问平台的IP）。 */
    @Size(max = 50, message = "localIp长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String localIp;

    /** 字符集, 1.UTF-8 2.GB2312。 */
    private Integer charset;

    /** 树类型 国标规定了两种树的展现方式 216.行政区划：CivilCode 215.业务分组:BusinessGroup。 */
    @Min(value = 0, message = "treeType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer treeType;

    /** 地理坐标系， 目前支持 1.WGS84,2.GCJ02。 */
    private Integer geoCoordSys;

    /** 在线状态；保留旧库默认值2，协议业务阶段核定0/1/2含义。 */
    private Integer online;

    /** 注册时间。 */
    private LocalDateTime registerTime;

    /** 心跳时间。 */
    private LocalDateTime keepaliveTime;

    /** 心跳间隔 (最低25秒)。 */
    @Min(value = 0, message = "heartBeatInterval不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer heartBeatInterval;

    /** 心跳间隔 (最低2次)。 */
    @Min(value = 0, message = "heartBeatCount不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer heartBeatCount;

    /** 定位功能支持情况。取值:0-不支持;1-支持 GPS定位;2-支持北斗定位(可选,默认取值为0。 */
    @Min(value = 0, message = "positionCapability不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer positionCapability;

    /** 数据流传输模式 0.UDP:udp传输 1.TCP-PASSIVE：tcp被动模式 2.TCP-ACTIVE：tcp主动模式。 */
    private Integer streamMode;

    /** 注册有效期（单位：秒 默认1天）。 */
    @Min(value = 0, message = "expires不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer expires;

    /** 移动设备位置信息上报时间间隔,单位:秒,默认值5 (秒)。 */
    @Min(value = 0, message = "mobilePositionSubmissionInterval不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer mobilePositionSubmissionInterval;

    /** 目录订阅周期，0为不订阅 (秒)。 */
    @Min(value = 0, message = "subscribeCycleForCatalog不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer subscribeCycleForCatalog;

    /** 移动设备位置订阅周期，0为不订阅 (秒)。 */
    @Min(value = 0, message = "subscribeCycleForMobilePosition不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer subscribeCycleForMobilePosition;

    /** 报警心跳时间订阅周期，0为不订阅 (秒)。 */
    @Min(value = 0, message = "subscribeCycleForAlarm不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer subscribeCycleForAlarm;

    /** 是否开启ssrc校验，默认关闭，开启可以防止串流。 */
    private Integer ssrcCheck;

    /** 流媒体编号 默认为null。 */
    @Size(max = 255, message = "mediaServerId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String mediaServerId;

    /** 是否作为消息通道 1.是 0.否。 */
    private Integer asMessageChannel;

    /** 是否开启超管权限(只有超级管理员才可查看)。 */
    private Integer hasAdministrator;

    /** 是否开启主子码流开关 1.是 0.否。 */
    private Integer switchPrimarySubStream;
}
