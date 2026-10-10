package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.CompanyConference;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/** 企业会议室表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = CompanyConference.class, reverseConvertGenerate = false)
public class CompanyConferenceBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业id。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 会议室名。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 会议室号码。 */
    @Size(max = 255, message = "code长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String code;

    /** 会议室密码。 */
    @Size(max = 255, message = "password长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 使用状态(1.未使用 1.使用中)。 */
    @Min(value = 0, message = "status不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer status;

    /** 启用状态(0.禁用 1.启用)。 */
    @Min(value = 0, message = "enable不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer enable;
}
