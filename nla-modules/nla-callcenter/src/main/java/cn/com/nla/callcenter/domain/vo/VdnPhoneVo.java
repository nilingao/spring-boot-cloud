package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.VdnPhone;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import cn.com.nla.common.sensitive.annotation.Sensitive;
import cn.com.nla.common.sensitive.core.SensitiveStrategy;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 路由号码表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = VdnPhone.class)
public class VdnPhoneVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private Long vdnId;
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ToString.Exclude
    private String phone;
    private Integer status;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
