package cn.com.nla.video.domain.vo;

import cn.com.nla.video.domain.PlatformGbStream;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 国标级联关联直播流视图，不包含凭据、原始流地址或删除标志。
 * @author TZY
 */
@Data
@AutoMapper(target = PlatformGbStream.class)
public class PlatformGbStreamVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String platformId;
    private String catalogId;
    private String gbStreamId;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
