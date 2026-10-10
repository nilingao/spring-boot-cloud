package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.RouteCall;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/** 字冠路由表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = RouteCall.class, reverseConvertGenerate = false)
public class RouteCallBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 所属企业。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 所属网关组。 */
    @Min(value = 0, message = "routeGroupId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long routeGroupId;

    /** 字冠号码。 */
    @Size(max = 32, message = "routeNum长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String routeNum;

    /** 最长。 */
    @Min(value = 0, message = "numMax不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer numMax;

    /** 最短。 */
    @Min(value = 0, message = "numMin不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer numMin;

    /** 主叫替换规则。 */
    @Min(value = 0, message = "callerChange不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer callerChange;

    /** 替换号码。 */
    @Size(max = 32, message = "callerChangeNum长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String callerChangeNum;

    /** 被叫替换规则。 */
    @Min(value = 0, message = "calledChange不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer calledChange;

    /** 替换号码。 */
    @Size(max = 32, message = "calledChangeNum长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String calledChangeNum;

    /** 状态。 */
    private Integer status;
}
