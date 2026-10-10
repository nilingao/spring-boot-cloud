package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.CompanyPhoneGroup;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 企业号码与号码池中间表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = CompanyPhoneGroup.class, reverseConvertGenerate = false)
public class CompanyPhoneGroupBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 号码池id。 */
    @Min(value = 0, message = "displayId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long displayId;

    /** 号码id。 */
    @Min(value = 0, message = "phoneId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long phoneId;
}
