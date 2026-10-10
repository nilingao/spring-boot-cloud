package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.Platform;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 平台信息输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = Platform.class, reverseConvertGenerate = false)
public class PlatformBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 本机IP。 */
    @Size(max = 30, message = "localIp长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String localIp;

    /** 外网IP。 */
    @Size(max = 30, message = "remoteIp长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String remoteIp;

    /** 注册端口。 */
    @Min(value = 0, message = "internalPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer internalPort;

    /** 中继端口。 */
    @Min(value = 0, message = "externalPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer externalPort;

    /** 起始RTP端口。 */
    @Min(value = 0, message = "startRtpPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer startRtpPort;

    /** 结束RTP端口。 */
    @Min(value = 0, message = "endRtpPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer endRtpPort;

    /** ws端口。 */
    @Min(value = 0, message = "wsPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer wsPort;

    /** wss端口。 */
    @Min(value = 0, message = "wssPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer wssPort;

    /** 音频编码。 */
    @Size(max = 256, message = "audioCode长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String audioCode;

    /** 视频编码。 */
    @Size(max = 256, message = "videoCode长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String videoCode;

    /** 分辨率。 */
    @Size(max = 30, message = "frameRate长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String frameRate;

    /** 码率。 */
    @Size(max = 30, message = "bitRate长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String bitRate;

    /** 是否启动ice。 */
    private Integer iceStart;

    /** stun地址。 */
    @Size(max = 30, message = "stunAddress长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String stunAddress;

    /** 名称。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 是否启用。 */
    private Integer enable;

    /** 在线状态。 */
    private Integer status;

    /** 是否开启音频。 */
    private Integer audioRecord;

    /** 是否开启视频。 */
    private Integer videoRecord;

    /** 音频存储地址。 */
    @Size(max = 256, message = "audioRecordPath长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String audioRecordPath;

    /** 视频存储地址。 */
    @Size(max = 256, message = "videoRecordPath长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String videoRecordPath;

    /** 声音文件地址。 */
    @Size(max = 256, message = "soundRilePath长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String soundRilePath;

    /** 交换服务文件路径。 */
    @Size(max = 256, message = "freeswitchPath长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String freeswitchPath;

    /** 交换服务日志路径。 */
    @Size(max = 256, message = "freeswitchLogPath长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String freeswitchLogPath;
}
