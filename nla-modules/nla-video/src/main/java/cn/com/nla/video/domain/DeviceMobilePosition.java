package cn.com.nla.video.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/** 移动位置信息；业务服务与协议接入在阶段 6.5 实现。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_device_mobile_position")
public class DeviceMobilePosition extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 设备Id。 */
    @TableField(value = "`device_id`")
    private String deviceId;

    /** 通道Id。 */
    @TableField(value = "`channel_id`")
    private String channelId;

    /** 设备名称。 */
    @TableField(value = "`device_name`")
    private String deviceName;

    /** 通知时间。 */
    @TableField(value = "`time`")
    private LocalDateTime time;

    /** 经度。 */
    @TableField(value = "`longitude`")
    private Double longitude;

    /** 纬度。 */
    @TableField(value = "`latitude`")
    private Double latitude;

    /** 海拔高度。 */
    @TableField(value = "`altitude`")
    private Double altitude;

    /** 速度。 */
    @TableField(value = "`speed`")
    private Double speed;

    /** 方向。 */
    @TableField(value = "`direction`")
    private Double direction;

    /** 位置信息上报来源（Mobile Position、GPS Alarm）。 */
    @TableField(value = "`report_source`")
    private String reportSource;

    /** 国内坐标系：经度坐标。 */
    @TableField(value = "`longitude_gcj02`")
    private Double longitudeGcj02;

    /** 国内坐标系：纬度坐标。 */
    @TableField(value = "`latitude_gcj02`")
    private Double latitudeGcj02;

    /** 国内坐标系：经度坐标。 */
    @TableField(value = "`longitude_wgs84`")
    private Double longitudeWgs84;

    /** 国内坐标系：纬度坐标。 */
    @TableField(value = "`latitude_wgs84`")
    private Double latitudeWgs84;

    /** 删除标志：0 存在、1 删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
