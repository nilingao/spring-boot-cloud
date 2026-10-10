package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.GroupMemoryConfig;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 技能组坐席记忆配置表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = GroupMemoryConfig.class, reverseConvertGenerate = false)
public class GroupMemoryConfigBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 技能组ID。 */
    @Min(value = 0, message = "groupId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long groupId;

    /** 匹配成功策略 1:等待记忆坐席 2:超时转其他空闲坐席 3:忙碌转空闲坐席。 */
    @Min(value = 0, message = "successStrategy不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer successStrategy;

    /** 匹配成功策略值（暂未用到）。 */
    @Min(value = 0, message = "successStrategyValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long successStrategyValue;

    /** 匹配失败策略 1:其他空闲坐席 2:其他技能组 3:vdn 4:ivr 5:挂机。 */
    @Min(value = 0, message = "failStrategy不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer failStrategy;

    /** 匹配失败策略值（所处编号值）。 */
    @Min(value = 0, message = "failStrategyValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long failStrategyValue;

    /** 记忆天数。 */
    @Min(value = 0, message = "memoryDay不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer memoryDay;

    /** 呼入覆盖。 */
    @Min(value = 0, message = "inboundCover不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer inboundCover;

    /** 外呼覆盖。 */
    @Min(value = 0, message = "outboundCover不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer outboundCover;

    /** status。 */
    @Min(value = 0, message = "status不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer status;
}
