package cn.com.nla.video.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/** 推流信息；业务服务与协议接入在阶段 6.5 实现。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_stream_push")
public class StreamPush extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 应用名。 */
    @TableField(value = "`app`")
    private String app;

    /** 流id。 */
    @TableField(value = "`stream`")
    private String stream;

    /** 观看总人数，包括hls/rtsp/rtmp/http-flv/ws-flv。 */
    @TableField(value = "`total_reader_count`")
    private String totalReaderCount;

    /** 产生源类型 unknown = 0,rtmp_push=1,rtsp_push=2,rtp_push=3,pull=4,ffmpeg_pull=5,mp4_vod=6,device_chn=7。 */
    @TableField(value = "`origin_type`")
    private Integer originType;

    /** 产生源类型的字符串描述。 */
    @TableField(value = "`origin_type_str`")
    private String originTypeStr;

    /** 存活时间，单位秒。 */
    @TableField(value = "`alive_second`")
    private Integer aliveSecond;

    /** 使用的流媒体ID。 */
    @TableField(value = "`media_server_id`")
    private String mediaServerId;

    /** 使用的服务ID。 */
    @TableField(value = "`server_id`")
    private String serverId;

    /** 推流时间。 */
    @TableField(value = "`push_time`")
    private LocalDateTime pushTime;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 是否正在推流。 */
    @TableField(value = "`push_ing`")
    private Integer pushIng;

    /** 是否自己平台的推流。 */
    @TableField(value = "`self`")
    private Integer onSelf;

    /** 删除标志：0 存在、1 删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
