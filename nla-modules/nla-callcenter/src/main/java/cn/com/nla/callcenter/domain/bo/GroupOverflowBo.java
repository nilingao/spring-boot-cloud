package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.GroupOverflow;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 技能组排队策略表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = GroupOverflow.class, reverseConvertGenerate = false)
public class GroupOverflowBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 技能组ID。 */
    @Min(value = 0, message = "groupId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long groupId;

    /** 溢出策略ID。 */
    @Min(value = 0, message = "overflowId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long overflowId;

    /** 优先级。 */
    @Min(value = 0, message = "levelValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer levelValue;

    /** 状态。 */
    private Integer status;
}
