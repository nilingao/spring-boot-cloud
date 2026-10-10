package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.CallDetail;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 通话流程表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = CallDetail.class, reverseConvertGenerate = false)
public class CallDetailBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 开始时间。 */
    @Min(value = 0, message = "startTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long startTime;

    /** 结束时间。 */
    @Min(value = 0, message = "endTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long endTime;

    /** 通话ID。 */
    @Min(value = 0, message = "callId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long callId;

    /** 顺序。 */
    @Min(value = 0, message = "detailIndex不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer detailIndex;

    /** 1:进vdn,2:进ivr,3:技能组,4:按键收号,5:外线,6:机器人,10:服务评价。 */
    @Min(value = 0, message = "transferType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer transferType;

    /** 转接ID。 */
    @Min(value = 0, message = "transferId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long transferId;

    /** 出队列原因:排队挂机或者转坐席。 */
    @Size(max = 50, message = "reason长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String reason;

    /** 状态。 */
    private Integer status;
}
