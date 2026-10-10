package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.VdnConfig;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 呼入路由字码表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = VdnConfig.class)
public class VdnConfigVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private String name;
    private Long vdnId;
    private Long scheduleId;
    private Integer routeType;
    private String routeValue;
    private Integer playType;
    private Long playValue;
    private String dtmfEnd;
    private Integer retry;
    private Integer dtmfMax;
    private Integer dtmfMin;
    private Integer status;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
