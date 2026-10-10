package cn.com.nla.video.domain.vo;

import cn.com.nla.video.domain.PlatformCatalog;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 国标级联-目录视图，不包含凭据、原始流地址或删除标志。
 * @author TZY
 */
@Data
@AutoMapper(target = PlatformCatalog.class)
public class PlatformCatalogVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String parentId;
    private String name;
    private String platformId;
    private String civilCode;
    private String businessGroupId;

    /** 非持久化查询字段：childrenCount。 */
    private Integer childrenCount;

    /** 非持久化查询字段：type。 */
    private Integer type;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
