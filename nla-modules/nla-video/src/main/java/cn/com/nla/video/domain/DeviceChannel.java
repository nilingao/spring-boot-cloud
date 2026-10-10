package cn.com.nla.video.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.time.LocalDateTime;

/** 通道信息；业务服务与协议接入在阶段 6.5 实现。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_device_channel")
public class DeviceChannel extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 父级id。 */
    @TableField(value = "`parent_id`")
    private String parentId;

    /** 通道国标编号。 */
    @TableField(value = "`channel_id`")
    private String channelId;

    /** 设备国标编号。 */
    @TableField(value = "`device_id`")
    private String deviceId;

    /** 通道名。 */
    @TableField(value = "`name`")
    private String name;

    /** 生产厂商。 */
    @TableField(value = "`manufacture`")
    private String manufacture;

    /** 型号。 */
    @TableField(value = "`model`")
    private String model;

    /** 设备归属。 */
    @TableField(value = "`owner`")
    private String owner;

    /** 行政区域。 */
    @TableField(value = "`civil_code`")
    private String civilCode;

    /** 警区。 */
    @TableField(value = "`block`")
    private String block;

    /** 安装地址。 */
    @TableField(value = "`address`")
    private String address;

    /** 是否有子设备 1有, 0没有。 */
    @TableField(value = "`parental`")
    private Integer parental;

    /** 信令安全模式  缺省为0; 0:不采用; 2: S/MIME签名方式; 3: S/ MIME加密签名同时采用方式; 4:数字摘要方式。 */
    @TableField(value = "`safety_way`")
    private Integer safetyWay;

    /** 注册方式 缺省为1;1:符合IETFRFC3261标准的认证注册模 式; 2:基于口令的双向认证注册模式; 3:基于数字证书的双向认证注册模式。 */
    @TableField(value = "`register_way`")
    private Integer registerWay;

    /** 证书序列号。 */
    @TableField(value = "`cert_num`")
    private String certNum;

    /** 证书有效标识 缺省为0;证书有效标识:0:无效1: 有效。 */
    @TableField(value = "`certifiable`")
    private Integer certifiable;

    /** 证书无效原因码。 */
    @TableField(value = "`err_code`")
    private Integer errCode;

    /** 证书终止有效期。 */
    @TableField(value = "`end_time`")
    private LocalDateTime endTime;

    /** 保密属性 缺省为0; 0:不涉密, 1:涉密。 */
    @TableField(value = "`secrecy`")
    private Integer secrecy;

    /** IP地址。 */
    @TableField(value = "`ip_address`")
    private String ipAddress;

    /** 端口号。 */
    @TableField(value = "`port`")
    private Integer port;

    /** 密码。 */
    @TableField(value = "`password`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 云台类型 0.未知 1.球机 2.半球 3.固定枪机 4.遥控枪机。 */
    @TableField(value = "`ptz_type`")
    private Integer ptzType;

    /** 云台类型描述字符串。 */
    @TableField(value = "`ptz_type_text`")
    private String ptzTypeText;

    /** 在线/离线， 1在线,0离线。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 经度。 */
    @TableField(value = "`longitude`")
    private Double longitude;

    /** 纬度。 */
    @TableField(value = "`latitude`")
    private Double latitude;

    /** 经度 GCJ02。 */
    @TableField(value = "`longitude_gcj02`")
    private Double longitudeGcj02;

    /** 纬度 GCJ02。 */
    @TableField(value = "`latitude_gcj02`")
    private Double latitudeGcj02;

    /** 经度 WGS84。 */
    @TableField(value = "`longitude_wgs84`")
    private Double longitudeWgs84;

    /** 纬度 WGS84。 */
    @TableField(value = "`latitude_wgs84`")
    private Double latitudeWgs84;

    /** 子设备数。 */
    @TableField(value = "`sub_count`")
    private Integer subCount;

    /** 流唯一编号，存在表示正在直播。 */
    @TableField(value = "`stream_id`")
    private String streamId;

    /** 是否开启录像。 */
    @TableField(value = "`has_record`")
    private Integer hasRecord;

    /** 是否开启音频。 */
    @TableField(value = "`has_audio`")
    private Integer hasAudio;

    /** 标记通道的类型，0->国标通道 1->直播流通道 2->业务分组/虚拟组织/行政区划。 */
    @TableField(value = "`channel_type`")
    private Integer channelType;

    /** 业务分组。 */
    @TableField(value = "`business_group_id`")
    private String businessGroupId;

    /** GPS的更新时间。 */
    @TableField(value = "`gps_time`")
    private LocalDateTime gpsTime;

    /** 删除标志：0 存在、1 删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
