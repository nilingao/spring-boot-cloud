package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.VdnSchedule;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 日程表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = VdnSchedule.class, reverseConvertGenerate = false)
public class VdnScheduleBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @NotNull(message = "companyId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 日程名称。 */
    @Size(max = 20, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 优先级。 */
    @Min(value = 0, message = "levelValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer levelValue;

    /** 1:指定时间,2:相对时间。 */
    @Min(value = 0, message = "type不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer type;

    /** 开始时间。 */
    @Size(max = 50, message = "startDay长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String startDay;

    /** 结束时间。 */
    @Size(max = 50, message = "endDay长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String endDay;

    /** 开始时间。 */
    @Size(max = 50, message = "startTime长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String startTime;

    /** 结束时间。 */
    @Size(max = 50, message = "endTime长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String endTime;

    /** 周一。 */
    @Min(value = 0, message = "mon不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer mon;

    /** 周二。 */
    @Min(value = 0, message = "tue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer tue;

    /** 周三。 */
    @Min(value = 0, message = "wed不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer wed;

    /** 周四。 */
    @Min(value = 0, message = "thu不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer thu;

    /** 周五。 */
    @Min(value = 0, message = "fri不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer fri;

    /** 周六。 */
    @Min(value = 0, message = "sat不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer sat;

    /** 周天。 */
    @Min(value = 0, message = "sun不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer sun;

    /** 状态。 */
    private Integer status;
}
