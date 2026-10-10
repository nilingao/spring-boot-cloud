package cn.com.nla.video.domain.vo;

import cn.com.nla.video.domain.DeviceMobilePosition;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 移动位置信息视图，不包含凭据、原始流地址或删除标志。
 * @author TZY
 */
@Data
@AutoMapper(target = DeviceMobilePosition.class)
public class DeviceMobilePositionVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String deviceId;
    private String channelId;
    private String deviceName;
    private LocalDateTime time;
    private Double longitude;
    private Double latitude;
    private Double altitude;
    private Double speed;
    private Double direction;
    private String reportSource;
    private Double longitudeGcj02;
    private Double latitudeGcj02;
    private Double longitudeWgs84;
    private Double latitudeWgs84;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
