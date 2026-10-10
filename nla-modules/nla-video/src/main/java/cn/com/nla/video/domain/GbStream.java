package cn.com.nla.video.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/** 直播流关联国标上级平台；业务服务与协议接入在阶段 6.5 实现。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_gb_stream")
public class GbStream extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "gb_stream_id", type = IdType.ASSIGN_ID)
    private Long gbStreamId;

    /** 应用名。 */
    @TableField(value = "`app`")
    private String app;

    /** 流ID。 */
    @TableField(value = "`stream`")
    private String stream;

    /** 国标ID。 */
    @TableField(value = "`gb_id`")
    private String gbId;

    /** 名称。 */
    @TableField(value = "`name`")
    private String name;

    /** 经度。 */
    @TableField(value = "`longitude`")
    private Double longitude;

    /** 纬度。 */
    @TableField(value = "`latitude`")
    private Double latitude;

    /** 流类型（1.拉流/2.推流）。 */
    @TableField(value = "`stream_type`")
    private Integer streamType;

    /** 流媒体ID。 */
    @TableField(value = "`media_server_id`")
    private String mediaServerId;

    /** 删除标志：0 存在、1 删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
