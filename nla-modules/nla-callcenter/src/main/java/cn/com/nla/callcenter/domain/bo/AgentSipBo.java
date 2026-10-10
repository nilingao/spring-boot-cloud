package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.AgentSip;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/** sip表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = AgentSip.class, reverseConvertGenerate = false)
public class AgentSipBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业主键。 */
    private Long companyId;

    /** 坐席主键。 */
    @Min(value = 0, message = "agentId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long agentId;

    /** sip编号。 */
    @Size(max = 32, message = "sip长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String sip;

    /** sip密码。 */
    @Size(max = 255, message = "sipPwd长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String sipPwd;

    /** 状态。 */
    private Integer status;
}
