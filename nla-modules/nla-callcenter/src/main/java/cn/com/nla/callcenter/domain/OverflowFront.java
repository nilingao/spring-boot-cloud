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

/** 溢出策略前置条件；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_overflow_front")
public class OverflowFront extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业ID。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 策略ID。 */
    @TableField(value = "`overflow_id`")
    private Long overflowId;

    /** 1:队列长度; 2:队列等待最大时长; 3:呼损率。 */
    @TableField(value = "`front_type`")
    private Integer frontType;

    /** 0:全部; 1:小于或等于; 2:等于; 3:大于或等于; 4:大于 ;5.介于。 */
    @TableField(value = "`compare_condition`")
    private Integer compareCondition;

    /** 介于的开始值。 */
    @TableField(value = "`rank_value_start`")
    private Integer rankValueStart;

    /** 符号条件值。 */
    @TableField(value = "`rank_value`")
    private Integer rankValue;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
