package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.UserAgent;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 用户座席中间表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = UserAgent.class, reverseConvertGenerate = false)
public class UserAgentBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 关联数据库雪花ID。 */
    @NotNull(message = "agentId不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long agentId;

    /** 关联数据库雪花ID。 */
    @NotNull(message = "userId不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long userId;
}
