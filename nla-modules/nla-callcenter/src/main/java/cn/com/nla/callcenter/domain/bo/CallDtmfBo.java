package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.CallDtmf;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 呼叫按键表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = CallDtmf.class, reverseConvertGenerate = false)
public class CallDtmfBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 按键号码。 */
    @Size(max = 255, message = "dtmfKey长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String dtmfKey;

    /** 业务流程id。 */
    @Min(value = 0, message = "processId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long processId;

    /** 通话标识id。 */
    @Min(value = 0, message = "callId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long callId;

    /** 按键时间。 */
    @Min(value = 0, message = "dtmfTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long dtmfTime;

    /** 状态。 */
    private Integer status;
}
