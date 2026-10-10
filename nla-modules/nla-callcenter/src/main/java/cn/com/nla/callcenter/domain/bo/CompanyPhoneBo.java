package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.CompanyPhone;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/** 企业号码输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = CompanyPhone.class, reverseConvertGenerate = false)
public class CompanyPhoneBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业id。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 号码。 */
    @Size(max = 255, message = "phone长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String phone;

    /** 1:呼入号码,2:主叫显号,3:被叫显号。 */
    @Min(value = 0, message = "type不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer type;

    /** 1:未启用,2:启用。 */
    @Min(value = 0, message = "status不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer status;
}
