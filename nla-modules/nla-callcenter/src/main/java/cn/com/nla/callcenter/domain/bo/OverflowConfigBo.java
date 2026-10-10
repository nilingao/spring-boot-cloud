package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.OverflowConfig;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 溢出策略表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = OverflowConfig.class, reverseConvertGenerate = false)
public class OverflowConfigBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业id。 */
    private Long companyId;

    /** 名称。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 1:排队,2:溢出,3:挂机。 */
    @Min(value = 0, message = "handleType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer handleType;

    /** 排队方式(1:先进先出,2:vip,3:自定义)。 */
    private Integer busyType;

    /** 排队超时时间。 */
    private Integer queueTimeout;

    /** 排队超时(1:溢出,2:挂机)。 */
    private Integer busyTimeoutType;

    /** 溢出(1:group,2:ivr,3:vdn)。 */
    private Integer overflowType;

    /** 溢出目标雪花ID。 */
    private Long overflowValue;

    /** 自定义排队表达式。 */
    @Size(max = 255, message = "lineupExpression长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String lineupExpression;
}
