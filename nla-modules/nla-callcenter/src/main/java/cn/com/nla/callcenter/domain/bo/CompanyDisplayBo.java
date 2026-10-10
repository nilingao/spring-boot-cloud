package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.CompanyDisplay;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 号码池表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = CompanyDisplay.class, reverseConvertGenerate = false)
public class CompanyDisplayBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业id。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 号码池。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 1:呼入号码,2:主叫显号,3:被叫显号。 */
    @Min(value = 0, message = "type不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer type;

    /** 状态。 */
    private Integer status;
}
