package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.AgentGroup;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 坐席技能组表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = AgentGroup.class, reverseConvertGenerate = false)
public class AgentGroupBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 坐席id。 */
    @Min(value = 0, message = "agentId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long agentId;

    /** 坐席key。 */
    @Size(max = 50, message = "agentKey长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String agentKey;

    /** 坐席类型 1.普通员工 2.组长。 */
    @Min(value = 0, message = "agentType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer agentType;

    /** 技能组id。 */
    @Min(value = 0, message = "groupId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long groupId;

    /** 状态。 */
    private Integer status;
}
