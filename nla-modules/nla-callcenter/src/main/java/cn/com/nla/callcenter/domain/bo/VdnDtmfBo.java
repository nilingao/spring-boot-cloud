package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.VdnDtmf;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 路由按键导航表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = VdnDtmf.class, reverseConvertGenerate = false)
public class VdnDtmfBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 呼入路由字码ID。 */
    @Min(value = 0, message = "navigateId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long navigateId;

    /** 按键。 */
    @Size(max = 20, message = "dtmf长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String dtmf;

    /** 路由类型(1:技能组,2:IVR,3:路由字码,4:坐席分机,5:挂机)。 */
    private Integer routeType;

    /** 路由值。 */
    @Min(value = 0, message = "routeValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long routeValue;

    /** 状态。 */
    private Integer status;
}
