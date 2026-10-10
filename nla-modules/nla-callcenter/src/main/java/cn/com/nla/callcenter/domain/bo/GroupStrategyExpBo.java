package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.GroupStrategyExp;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 技能组坐席自定义策略表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = GroupStrategyExp.class, reverseConvertGenerate = false)
public class GroupStrategyExpBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 技能组id。 */
    @Min(value = 0, message = "groupId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long groupId;

    /** 自定义值。 */
    @Size(max = 20, message = "strategyKey长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String strategyKey;

    /** 百分百。 */
    @Min(value = 0, message = "strategyPresent不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer strategyPresent;

    /** 类型。 */
    @Min(value = 0, message = "strategyType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer strategyType;

    /** 状态。 */
    private Integer status;
}
