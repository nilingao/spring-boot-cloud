package cn.com.nla.video.domain.vo;

import cn.com.nla.video.domain.DeviceChannel;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 通道信息视图，不包含凭据、原始流地址或删除标志。
 * @author TZY
 */
@Data
@AutoMapper(target = DeviceChannel.class)
public class DeviceChannelVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String parentId;
    private String channelId;
    private String deviceId;
    private String name;
    private String manufacture;
    private String model;
    private String owner;
    private String civilCode;
    private String block;
    private String address;
    private Integer parental;
    private Integer safetyWay;
    private Integer registerWay;
    private String certNum;
    private Integer certifiable;
    private Integer errCode;
    private LocalDateTime endTime;
    private Integer secrecy;
    private String ipAddress;
    private Integer port;
    private Integer ptzType;
    private String ptzTypeText;
    private Integer status;
    private Double longitude;
    private Double latitude;
    private Double longitudeGcj02;
    private Double latitudeGcj02;
    private Double longitudeWgs84;
    private Double latitudeWgs84;
    private Integer subCount;
    private String streamId;
    private Integer hasRecord;
    private Integer hasAudio;
    private Integer channelType;
    private String businessGroupId;
    private LocalDateTime gpsTime;

    /** 非持久化查询字段：平台Id。 */
    private String platformId;

    /** 非持久化查询字段：目录Id。 */
    private String catalogId;

    /** 非持久化查询字段：生产厂商。 */
    private String manufacturer;

    /** 非持久化查询字段：wan地址。 */
    private String hostAddress;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
