package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.OverflowFront;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 溢出策略前置条件输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = OverflowFront.class, reverseConvertGenerate = false)
public class OverflowFrontBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 策略ID。 */
    @Min(value = 0, message = "overflowId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long overflowId;

    /** 1:队列长度; 2:队列等待最大时长; 3:呼损率。 */
    @Min(value = 0, message = "frontType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer frontType;

    /** 0:全部; 1:小于或等于; 2:等于; 3:大于或等于; 4:大于 ;5.介于。 */
    @Min(value = 0, message = "compareCondition不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer compareCondition;

    /** 介于的开始值。 */
    @Min(value = 0, message = "rankValueStart不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer rankValueStart;

    /** 符号条件值。 */
    @Min(value = 0, message = "rankValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer rankValue;

    /** 状态。 */
    private Integer status;
}
