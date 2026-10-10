package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.GroupAgentStrategy;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 技能组中坐席分配策略输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = GroupAgentStrategy.class, reverseConvertGenerate = false)
public class GroupAgentStrategyBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 技能组id。 */
    @Min(value = 0, message = "groupId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long groupId;

    /** 1:内置策略,2:自定义。 */
    @Min(value = 0, message = "strategyType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer strategyType;

    /** (策略值 1当前最长空闲时间、2空闲次数最多、3最少应答次数、4累计最少通话时长、5累计话后时长、6轮选、7随机)。 */
    @Min(value = 0, message = "strategyValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer strategyValue;

    /** 自定义表达式。 */
    @Size(max = 255, message = "customExpression长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String customExpression;

    /** status。 */
    @Min(value = 0, message = "status不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer status;
}
