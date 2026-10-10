package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.SkillAgent;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 坐席技能表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = SkillAgent.class, reverseConvertGenerate = false)
public class SkillAgentBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业id。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 技能id。 */
    @Min(value = 0, message = "skillId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long skillId;

    /** 坐席id。 */
    @Min(value = 0, message = "agentId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long agentId;

    /** 范围。 */
    @Min(value = 0, message = "rankValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer rankValue;

    /** 状态。 */
    private Integer status;
}
