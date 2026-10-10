package cn.com.nla.video.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/** 国标级联-目录；业务服务与协议接入在阶段 6.5 实现。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_platform_catalog")
public class PlatformCatalog extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 协议字符串主键，由调用方提供。 */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** 父级目录ID。 */
    @TableField(value = "`parent_id`")
    private String parentId;

    /** 名称。 */
    @TableField(value = "`name`")
    private String name;

    /** 平台ID。 */
    @TableField(value = "`platform_id`")
    private String platformId;

    /** 行政区划。 */
    @TableField(value = "`civil_code`")
    private String civilCode;

    /** 目录分组。 */
    @TableField(value = "`business_group_id`")
    private String businessGroupId;

    /** 删除标志：0 存在、1 删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
