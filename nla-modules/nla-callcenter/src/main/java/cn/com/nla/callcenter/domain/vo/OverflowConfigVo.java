package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.OverflowConfig;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 溢出策略表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = OverflowConfig.class)
public class OverflowConfigVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private String name;
    private Integer handleType;
    private Integer busyType;
    private Integer queueTimeout;
    private Integer busyTimeoutType;
    private Integer overflowType;
    private Long overflowValue;
    private String lineupExpression;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
