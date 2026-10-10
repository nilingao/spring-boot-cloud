package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.CompanyConference;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 企业会议室表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = CompanyConference.class)
public class CompanyConferenceVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private String name;
    private String code;
    private Integer status;
    private Integer enable;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
