package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.SkillGroup;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 技能组技能表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = SkillGroup.class, reverseConvertGenerate = false)
public class SkillGroupBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** level_value。 */
    @Min(value = 0, message = "levelValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer levelValue;

    /** 技能ID。 */
    @Min(value = 0, message = "skillId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long skillId;

    /** 技能组ID。 */
    @Min(value = 0, message = "groupId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long groupId;

    /** 等级类型(1:全部,2:等于,3:>,4:<,5:介于)。 */
    private Integer rankType;

    /** 介于的开始值。 */
    @Min(value = 0, message = "rankValueStart不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer rankValueStart;

    /** 等级值 技能等级在0-100之间。 */
    @Min(value = 0, message = "rankValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer rankValue;

    /** 匹配规则(1:低到高,2:高到低)。 */
    @Min(value = 0, message = "matchType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer matchType;

    /** 占用率 (占用率在1-100之间)。 */
    @Min(value = 0, message = "shareValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer shareValue;

    /** 状态。 */
    private Integer status;
}
