package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.VdnPhone;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/** 路由号码表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = VdnPhone.class, reverseConvertGenerate = false)
public class VdnPhoneBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 路由码。 */
    @Min(value = 0, message = "vdnId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long vdnId;

    /** 特服号。 */
    @Size(max = 20, message = "phone长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String phone;

    /** 状态。 */
    private Integer status;
}
