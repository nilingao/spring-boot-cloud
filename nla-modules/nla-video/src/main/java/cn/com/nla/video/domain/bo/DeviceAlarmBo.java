package cn.com.nla.video.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.video.domain.DeviceAlarm;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 报警信息输入，不接受审计与删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = DeviceAlarm.class, reverseConvertGenerate = false)
public class DeviceAlarmBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 设备的国标编号。 */
    @NotBlank(message = "deviceId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "deviceId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String deviceId;

    /** 通道的国标编号。 */
    @NotBlank(message = "channelId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "channelId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String channelId;

    /** 报警级别, 1为一级警情, 2为二级警情, 3为三级警情, 4为四级警情。 */
    @NotNull(message = "alarmPriority不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer alarmPriority;

    /** 报警方式 , 1为电话报警, 2为设备报警, 3为短信报警, 4为 GPS报警, 5为视频报警, 6为设备故障报警,7其他报警;可以为直接组合如12为电话报警或 设备报警-。 */
    private Integer alarmMethod;

    /** 报警时间。 */
    @NotNull(message = "alarmTime不能为空", groups = {AddGroup.class, EditGroup.class})
    private LocalDateTime alarmTime;

    /** 报警内容描述。 */
    @Size(max = 255, message = "alarmDescription长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String alarmDescription;

    /** 经度。 */
    private Double longitude;

    /** 纬度。 */
    private Double latitude;

    /** 报警类型 报警方式为2时， 1-视频丢失报警 2-设备防拆报警 3-存储设备磁盘满报警 4-设备高温报警 5-设备低温报警， 报警方式为5时,取值如下，1-人工视频报警 2-运动目标检测报警 3-遗留物检测报警 4-物体移除检测报警 5-绊线检测报警  6-入侵检测报警 7-逆行检测报警 8-徘徊检测报警 9-流量统计报警 10-密度检测报警 11-视频异常检测报警 12-快速移动报警 报警方式为6时,取值下， 1-存储设备磁盘故障报警 2-存储设备风扇故障报警。 */
    private Integer alarmType;
}
