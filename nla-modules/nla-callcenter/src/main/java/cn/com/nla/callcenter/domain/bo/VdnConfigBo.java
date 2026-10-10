package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.VdnConfig;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 呼入路由字码表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = VdnConfig.class, reverseConvertGenerate = false)
public class VdnConfigBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 子码日程。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 呼入路由编号。 */
    @Min(value = 0, message = "vdnId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long vdnId;

    /** 日程id。 */
    @Min(value = 0, message = "scheduleId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long scheduleId;

    /** 路由类型(1:技能组,2:放音,3:ivr,4:坐席,5:外呼)。 */
    private Integer routeType;

    /** 路由类型值。 */
    @Size(max = 255, message = "routeValue长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String routeValue;

    /** 放音类型(1:按键导航,2:技能组,3:ivr,4:路由字码,5:挂机)。 */
    private Integer playType;

    /** 放音类型对应值。 */
    @Min(value = 0, message = "playValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long playValue;

    /** 结束音。 */
    @Size(max = 255, message = "dtmfEnd长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String dtmfEnd;

    /** 重复播放次数。 */
    @Min(value = 0, message = "retry不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer retry;

    /** 最大收键长度。 */
    @Min(value = 0, message = "dtmfMax不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer dtmfMax;

    /** 最小收键长度。 */
    @Min(value = 0, message = "dtmfMin不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer dtmfMin;

    /** 状态。 */
    @Min(value = 0, message = "status不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer status;
}
