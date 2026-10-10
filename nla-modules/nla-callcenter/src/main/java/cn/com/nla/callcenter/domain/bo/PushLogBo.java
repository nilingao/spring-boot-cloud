package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.PushLog;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/** 话单推送记录表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = PushLog.class, reverseConvertGenerate = false)
public class PushLogBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业id。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** callid。 */
    @Min(value = 0, message = "callId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long callId;

    /** 发送次数。 */
    @Size(max = 255, message = "cdrNotifyUrl长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String cdrNotifyUrl;

    /** 推送内容。 */
    @Size(max = 10240, message = "content长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String content;

    /** 推送次数。 */
    @Min(value = 0, message = "pushTimes不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer pushTimes;

    /** 推送返回值。 */
    @Size(max = 255, message = "pushResponse长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String pushResponse;

    /** 状态(1:推送，0:不推送)。 */
    @Min(value = 0, message = "status不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer status;
}
