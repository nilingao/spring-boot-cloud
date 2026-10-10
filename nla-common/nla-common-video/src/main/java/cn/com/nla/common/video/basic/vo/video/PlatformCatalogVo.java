package cn.com.nla.common.video.basic.vo.video;

import cn.com.nla.common.video.basic.model.StringIdEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
    * 国标级联-目录
    */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class PlatformCatalogVo extends StringIdEntity {
    /**
     * 父级目录ID
     */
    private String parentId;

    /**
     * 名称
     */
    private String name;

    /**
     * 平台ID
     */
    private String platformId;

    /**
     * 行政区划
     */
    private String civilCode;

    /**
     * 目录分组
     */
    private String businessGroupId;

    /**
     * 子节点数
     */
    private int childrenCount;
    /**
     * 类型：0 目录, 1 国标通道, 2 直播流
     */
    private int type;
}