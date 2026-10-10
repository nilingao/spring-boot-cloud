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

/** 技能组技能表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_skill_group")
public class SkillGroup extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业ID。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** level_value。 */
    @TableField(value = "`level_value`")
    private Integer levelValue;

    /** 技能ID。 */
    @TableField(value = "`skill_id`")
    private Long skillId;

    /** 技能组ID。 */
    @TableField(value = "`group_id`")
    private Long groupId;

    /** 等级类型(1:全部,2:等于,3:>,4:<,5:介于)。 */
    @TableField(value = "`rank_type`")
    private Integer rankType;

    /** 介于的开始值。 */
    @TableField(value = "`rank_value_start`")
    private Integer rankValueStart;

    /** 等级值 技能等级在0-100之间。 */
    @TableField(value = "`rank_value`")
    private Integer rankValue;

    /** 匹配规则(1:低到高,2:高到低)。 */
    @TableField(value = "`match_type`")
    private Integer matchType;

    /** 占用率 (占用率在1-100之间)。 */
    @TableField(value = "`share_value`")
    private Integer shareValue;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
