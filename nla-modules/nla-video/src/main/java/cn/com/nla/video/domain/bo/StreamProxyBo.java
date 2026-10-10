package cn.com.nla.video.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.video.domain.StreamProxy;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

/** 拉流代理的信息输入，不接受审计与删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = StreamProxy.class, reverseConvertGenerate = false)
public class StreamProxyBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 类型。 */
    @NotNull(message = "type不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer type;

    /** 应用名。 */
    @NotBlank(message = "app不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 255, message = "app长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String app;

    /** 流ID。 */
    @NotBlank(message = "stream不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 255, message = "stream长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String stream;

    /** 名称。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 状态。 */
    @NotNull(message = "status不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer status;

    /** 流媒体服务ID。 */
    @Size(max = 50, message = "mediaServerId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String mediaServerId;

    /** 拉流地址。 */
    @Size(max = 255, message = "url长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String url;

    /** 拉流地址。 */
    @Size(max = 255, message = "srcUrl长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String srcUrl;

    /** 目标地址。 */
    @Size(max = 255, message = "dstUrl长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String dstUrl;

    /** 超时时间。 */
    @Min(value = 0, message = "timeoutMs不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer timeoutMs;

    /** ffmpeg模板KEY。 */
    @Size(max = 255, message = "ffmpegCmdKey长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String ffmpegCmdKey;

    /** rtsp拉流时，拉流方式，0：tcp，1：udp，2：组播。 */
    private Integer rtpType;

    /** 是否启用。 */
    private Integer enable;

    /** 是否启用音频。 */
    private Integer enableAudio;

    /** 是否启用MP4。 */
    private Integer enableMp4;

    /** 是否 无人观看时删除。 */
    private Integer enableRemoveNoneReader;

    /** 是否 无人观看时自动停用。 */
    private Integer enableDisableNoneReader;
}
