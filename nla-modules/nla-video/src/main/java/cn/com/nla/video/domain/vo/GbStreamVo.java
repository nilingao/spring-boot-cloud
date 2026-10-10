package cn.com.nla.video.domain.vo;

import cn.com.nla.video.domain.GbStream;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 直播流关联国标上级平台视图，不包含凭据、原始流地址或删除标志。
 * @author TZY
 */
@Data
@AutoMapper(target = GbStream.class)
public class GbStreamVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long gbStreamId;
    private String app;
    private String stream;
    private String gbId;
    private String name;
    private Double longitude;
    private Double latitude;
    private Integer streamType;
    private String mediaServerId;

    /** 非持久化查询字段：platformId。 */
    private String platformId;

    /** 非持久化查询字段：catalogId。 */
    private String catalogId;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
