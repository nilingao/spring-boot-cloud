package cn.com.nla.video.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/** 国标级联关联通道信息；业务服务与协议接入在阶段 6.5 实现。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_platform_gb_channel")
public class PlatformGbChannel extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 平台国标ID。 */
    @TableField(value = "`platform_id`")
    private String platformId;

    /** 目录ID。 */
    @TableField(value = "`catalog_id`")
    private String catalogId;

    /** 通道国标ID。 */
    @TableField(value = "`device_channel_id`")
    private String deviceChannelId;

    /** 删除标志：0 存在、1 删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
