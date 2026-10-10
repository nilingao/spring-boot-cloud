package cn.com.nla.video.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.video.domain.StreamPush;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 推流信息输入，不接受审计与删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = StreamPush.class, reverseConvertGenerate = false)
public class StreamPushBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 应用名。 */
    @NotBlank(message = "app不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 255, message = "app长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String app;

    /** 流id。 */
    @NotBlank(message = "stream不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 255, message = "stream长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String stream;

    /** 观看总人数，包括hls/rtsp/rtmp/http-flv/ws-flv。 */
    @Size(max = 50, message = "totalReaderCount长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String totalReaderCount;

    /** 产生源类型 unknown = 0,rtmp_push=1,rtsp_push=2,rtp_push=3,pull=4,ffmpeg_pull=5,mp4_vod=6,device_chn=7。 */
    private Integer originType;

    /** 产生源类型的字符串描述。 */
    @Size(max = 50, message = "originTypeStr长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String originTypeStr;

    /** 存活时间，单位秒。 */
    @Min(value = 0, message = "aliveSecond不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer aliveSecond;

    /** 使用的流媒体ID。 */
    @Size(max = 50, message = "mediaServerId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String mediaServerId;

    /** 使用的服务ID。 */
    @NotBlank(message = "serverId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "serverId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String serverId;

    /** 推流时间。 */
    private LocalDateTime pushTime;

    /** 状态。 */
    private Integer status;

    /** 是否正在推流。 */
    private Integer pushIng;

    /** 是否自己平台的推流。 */
    private Integer onSelf;
}
