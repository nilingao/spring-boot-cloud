package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.Company;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import cn.com.nla.common.sensitive.annotation.Sensitive;
import cn.com.nla.common.sensitive.core.SensitiveStrategy;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 企业信息表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = Company.class)
public class CompanyVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String companyCode;
    private String contact;
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ToString.Exclude
    private String phone;
    private BigDecimal balance;
    private Integer billType;
    private Integer payType;
    private Integer hiddenCustomer;
    private Integer secretType;
    private Integer ivrLimit;
    private Integer agentLimit;
    private Integer groupLimit;
    private Integer groupAgentLimit;
    private Integer recordStorage;
    private Integer status;
    private Integer conferenceLimit;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
