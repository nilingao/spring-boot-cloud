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

/** 溢出策略表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_overflow_config")
public class OverflowConfig extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业id。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 名称。 */
    @TableField(value = "`name`")
    private String name;

    /** 1:排队,2:溢出,3:挂机。 */
    @TableField(value = "`handle_type`")
    private Integer handleType;

    /** 排队方式(1:先进先出,2:vip,3:自定义)。 */
    @TableField(value = "`busy_type`")
    private Integer busyType;

    /** 排队超时时间。 */
    @TableField(value = "`queue_timeout`")
    private Integer queueTimeout;

    /** 排队超时(1:溢出,2:挂机)。 */
    @TableField(value = "`busy_timeout_type`")
    private Integer busyTimeoutType;

    /** 溢出(1:group,2:ivr,3:vdn)。 */
    @TableField(value = "`overflow_type`")
    private Integer overflowType;

    /** 溢出目标雪花ID。 */
    @TableField(value = "`overflow_value`")
    private Long overflowValue;

    /** 自定义排队表达式。 */
    @TableField(value = "`lineup_expression`")
    private String lineupExpression;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
