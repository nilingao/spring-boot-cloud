package cn.com.nla.video.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;

/** 拉流代理的信息；业务服务与协议接入在阶段 6.5 实现。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_stream_proxy")
public class StreamProxy extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 类型。 */
    @TableField(value = "`type`")
    private Integer type;

    /** 应用名。 */
    @TableField(value = "`app`")
    private String app;

    /** 流ID。 */
    @TableField(value = "`stream`")
    private String stream;

    /** 名称。 */
    @TableField(value = "`name`")
    private String name;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 流媒体服务ID。 */
    @TableField(value = "`media_server_id`")
    private String mediaServerId;

    /** 拉流地址。 */
    @TableField(value = "`url`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String url;

    /** 拉流地址。 */
    @TableField(value = "`src_url`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String srcUrl;

    /** 目标地址。 */
    @TableField(value = "`dst_url`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String dstUrl;

    /** 超时时间。 */
    @TableField(value = "`timeout_ms`")
    private Integer timeoutMs;

    /** ffmpeg模板KEY。 */
    @TableField(value = "`ffmpeg_cmd_key`")
    private String ffmpegCmdKey;

    /** rtsp拉流时，拉流方式，0：tcp，1：udp，2：组播。 */
    @TableField(value = "`rtp_type`")
    private Integer rtpType;

    /** 是否启用。 */
    @TableField(value = "`enable`")
    private Integer enable;

    /** 是否启用音频。 */
    @TableField(value = "`enable_audio`")
    private Integer enableAudio;

    /** 是否启用MP4。 */
    @TableField(value = "`enable_mp4`")
    private Integer enableMp4;

    /** 是否 无人观看时删除。 */
    @TableField(value = "`enable_remove_none_reader`")
    private Integer enableRemoveNoneReader;

    /** 是否 无人观看时自动停用。 */
    @TableField(value = "`enable_disable_none_reader`")
    private Integer enableDisableNoneReader;

    /** 删除标志：0 存在、1 删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
