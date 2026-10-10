package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.GroupMemory;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/** 坐席与客户记忆表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = GroupMemory.class, reverseConvertGenerate = false)
public class GroupMemoryBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 技能组ID。 */
    @Min(value = 0, message = "groupId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long groupId;

    /** 坐席。 */
    @Size(max = 32, message = "agentKey长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String agentKey;

    /** 客户电话。 */
    @Size(max = 255, message = "phone长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String phone;

    /** 状态。 */
    private Integer status;
}
