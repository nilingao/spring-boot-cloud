package cn.com.nla.video.domain.vo;

import cn.com.nla.video.domain.DeviceAlarm;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 报警信息视图，不包含凭据、原始流地址或删除标志。
 * @author TZY
 */
@Data
@AutoMapper(target = DeviceAlarm.class)
public class DeviceAlarmVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String deviceId;
    private String channelId;
    private Integer alarmPriority;
    private Integer alarmMethod;
    private LocalDateTime alarmTime;
    private String alarmDescription;
    private Double longitude;
    private Double latitude;
    private Integer alarmType;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
