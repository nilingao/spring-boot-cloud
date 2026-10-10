package cn.com.nla.video.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.video.domain.DeviceChannel;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 通道信息输入，不接受审计与删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = DeviceChannel.class, reverseConvertGenerate = false)
public class DeviceChannelBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 父级id。 */
    @Size(max = 50, message = "parentId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String parentId;

    /** 通道国标编号。 */
    @NotBlank(message = "channelId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "channelId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String channelId;

    /** 设备国标编号。 */
    @NotBlank(message = "deviceId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "deviceId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String deviceId;

    /** 通道名。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 生产厂商。 */
    @Size(max = 50, message = "manufacture长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String manufacture;

    /** 型号。 */
    @Size(max = 50, message = "model长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String model;

    /** 设备归属。 */
    @Size(max = 50, message = "owner长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String owner;

    /** 行政区域。 */
    @Size(max = 50, message = "civilCode长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String civilCode;

    /** 警区。 */
    @Size(max = 50, message = "block长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String block;

    /** 安装地址。 */
    @Size(max = 50, message = "address长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String address;

    /** 是否有子设备 1有, 0没有。 */
    private Integer parental;

    /** 信令安全模式  缺省为0; 0:不采用; 2: S/MIME签名方式; 3: S/ MIME加密签名同时采用方式; 4:数字摘要方式。 */
    private Integer safetyWay;

    /** 注册方式 缺省为1;1:符合IETFRFC3261标准的认证注册模 式; 2:基于口令的双向认证注册模式; 3:基于数字证书的双向认证注册模式。 */
    private Integer registerWay;

    /** 证书序列号。 */
    @Size(max = 50, message = "certNum长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String certNum;

    /** 证书有效标识 缺省为0;证书有效标识:0:无效1: 有效。 */
    private Integer certifiable;

    /** 证书无效原因码。 */
    private Integer errCode;

    /** 证书终止有效期。 */
    private LocalDateTime endTime;

    /** 保密属性 缺省为0; 0:不涉密, 1:涉密。 */
    private Integer secrecy;

    /** IP地址。 */
    @Size(max = 50, message = "ipAddress长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String ipAddress;

    /** 端口号。 */
    @Min(value = 0, message = "port不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer port;

    /** 密码。 */
    @Size(max = 255, message = "password长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 云台类型 0.未知 1.球机 2.半球 3.固定枪机 4.遥控枪机。 */
    private Integer ptzType;

    /** 云台类型描述字符串。 */
    @Size(max = 255, message = "ptzTypeText长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String ptzTypeText;

    /** 在线/离线， 1在线,0离线。 */
    private Integer status;

    /** 经度。 */
    private Double longitude;

    /** 纬度。 */
    private Double latitude;

    /** 经度 GCJ02。 */
    private Double longitudeGcj02;

    /** 纬度 GCJ02。 */
    private Double latitudeGcj02;

    /** 经度 WGS84。 */
    private Double longitudeWgs84;

    /** 纬度 WGS84。 */
    private Double latitudeWgs84;

    /** 子设备数。 */
    @Min(value = 0, message = "subCount不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer subCount;

    /** 流唯一编号，存在表示正在直播。 */
    @Size(max = 255, message = "streamId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String streamId;

    /** 是否开启录像。 */
    private Integer hasRecord;

    /** 是否开启音频。 */
    private Integer hasAudio;

    /** 标记通道的类型，0->国标通道 1->直播流通道 2->业务分组/虚拟组织/行政区划。 */
    private Integer channelType;

    /** 业务分组。 */
    @Size(max = 50, message = "businessGroupId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String businessGroupId;

    /** GPS的更新时间。 */
    private LocalDateTime gpsTime;
}
