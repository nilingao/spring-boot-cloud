package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.OverflowFront;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 溢出策略前置条件视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = OverflowFront.class)
public class OverflowFrontVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private Long overflowId;
    private Integer frontType;
    private Integer compareCondition;
    private Integer rankValueStart;
    private Integer rankValue;
    private Integer status;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
