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

/** 技能组中坐席分配策略；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_group_agent_strategy")
public class GroupAgentStrategy extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业ID。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 技能组id。 */
    @TableField(value = "`group_id`")
    private Long groupId;

    /** 1:内置策略,2:自定义。 */
    @TableField(value = "`strategy_type`")
    private Integer strategyType;

    /** (策略值 1当前最长空闲时间、2空闲次数最多、3最少应答次数、4累计最少通话时长、5累计话后时长、6轮选、7随机)。 */
    @TableField(value = "`strategy_value`")
    private Integer strategyValue;

    /** 自定义表达式。 */
    @TableField(value = "`custom_expression`")
    private String customExpression;

    /** status。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
