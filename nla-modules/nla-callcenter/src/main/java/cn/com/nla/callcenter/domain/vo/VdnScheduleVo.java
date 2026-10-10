package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.VdnSchedule;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 日程表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = VdnSchedule.class)
public class VdnScheduleVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private String name;
    private Integer levelValue;
    private Integer type;
    private String startDay;
    private String endDay;
    private String startTime;
    private String endTime;
    private Integer mon;
    private Integer tue;
    private Integer wed;
    private Integer thu;
    private Integer fri;
    private Integer sat;
    private Integer sun;
    private Integer status;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
