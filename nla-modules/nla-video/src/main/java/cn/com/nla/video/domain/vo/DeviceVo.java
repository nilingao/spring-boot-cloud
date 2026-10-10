package cn.com.nla.video.domain.vo;

import cn.com.nla.video.domain.Device;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 设备信息视图，不包含凭据、原始流地址或删除标志。
 * @author TZY
 */
@Data
@AutoMapper(target = Device.class)
public class DeviceVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String deviceId;
    private String name;
    private String manufacturer;
    private String model;
    private String customName;
    private String firmware;
    private Integer transport;
    private String ip;
    private Integer port;
    private String hostAddress;
    private String sdpIp;
    private String localIp;
    private Integer charset;
    private Integer treeType;
    private Integer geoCoordSys;
    private Integer online;
    private LocalDateTime registerTime;
    private LocalDateTime keepaliveTime;
    private Integer heartBeatInterval;
    private Integer heartBeatCount;
    private Integer positionCapability;
    private Integer streamMode;
    private Integer expires;
    private Integer mobilePositionSubmissionInterval;
    private Integer subscribeCycleForCatalog;
    private Integer subscribeCycleForMobilePosition;
    private Integer subscribeCycleForAlarm;
    private Integer ssrcCheck;
    private String mediaServerId;
    private Integer asMessageChannel;
    private Integer hasAdministrator;
    private Integer switchPrimarySubStream;

    /** 非持久化查询字段：设备通道。 */
    private Integer channelCount;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
