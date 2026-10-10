package cn.com.nla.video.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.video.domain.DeviceMobilePosition;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 移动位置信息输入，不接受审计与删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = DeviceMobilePosition.class, reverseConvertGenerate = false)
public class DeviceMobilePositionBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 设备Id。 */
    @NotBlank(message = "deviceId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "deviceId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String deviceId;

    /** 通道Id。 */
    @NotBlank(message = "channelId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "channelId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String channelId;

    /** 设备名称。 */
    @Size(max = 255, message = "deviceName长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String deviceName;

    /** 通知时间。 */
    @NotNull(message = "time不能为空", groups = {AddGroup.class, EditGroup.class})
    private LocalDateTime time;

    /** 经度。 */
    private Double longitude;

    /** 纬度。 */
    private Double latitude;

    /** 海拔高度。 */
    private Double altitude;

    /** 速度。 */
    private Double speed;

    /** 方向。 */
    private Double direction;

    /** 位置信息上报来源（Mobile Position、GPS Alarm）。 */
    @Size(max = 50, message = "reportSource长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String reportSource;

    /** 国内坐标系：经度坐标。 */
    private Double longitudeGcj02;

    /** 国内坐标系：纬度坐标。 */
    private Double latitudeGcj02;

    /** 国内坐标系：经度坐标。 */
    private Double longitudeWgs84;

    /** 国内坐标系：纬度坐标。 */
    private Double latitudeWgs84;
}
