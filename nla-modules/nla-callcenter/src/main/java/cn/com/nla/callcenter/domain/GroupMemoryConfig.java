package cn.com.nla.callcenter.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/** 技能组坐席记忆配置表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_group_memory_config")
public class GroupMemoryConfig extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业ID。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 技能组ID。 */
    @TableField(value = "`group_id`")
    private Long groupId;

    /** 匹配成功策略 1:等待记忆坐席 2:超时转其他空闲坐席 3:忙碌转空闲坐席。 */
    @TableField(value = "`success_strategy`")
    private Integer successStrategy;

    /** 匹配成功策略值（暂未用到）。 */
    @TableField(value = "`success_strategy_value`")
    private Long successStrategyValue;

    /** 匹配失败策略 1:其他空闲坐席 2:其他技能组 3:vdn 4:ivr 5:挂机。 */
    @TableField(value = "`fail_strategy`")
    private Integer failStrategy;

    /** 匹配失败策略值（所处编号值）。 */
    @TableField(value = "`fail_strategy_value`")
    private Long failStrategyValue;

    /** 记忆天数。 */
    @TableField(value = "`memory_day`")
    private Integer memoryDay;

    /** 呼入覆盖。 */
    @TableField(value = "`inbound_cover`")
    private Integer inboundCover;

    /** 外呼覆盖。 */
    @TableField(value = "`outbound_cover`")
    private Integer outboundCover;

    /** status。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
