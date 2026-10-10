package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.OverflowExp;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 自定义溢出策略优先级输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = OverflowExp.class, reverseConvertGenerate = false)
public class OverflowExpBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 溢出策略ID。 */
    @Min(value = 0, message = "overflowId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long overflowId;

    /** 自定义值。 */
    @Size(max = 30, message = "expKey长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String expKey;

    /** 权重。 */
    @Min(value = 0, message = "rate不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer rate;

    /** 状态。 */
    private Integer status;
}
