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

/** 日程表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_vdn_schedule")
public class VdnSchedule extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业ID。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 日程名称。 */
    @TableField(value = "`name`")
    private String name;

    /** 优先级。 */
    @TableField(value = "`level_value`")
    private Integer levelValue;

    /** 1:指定时间,2:相对时间。 */
    @TableField(value = "`type`")
    private Integer type;

    /** 开始时间。 */
    @TableField(value = "`start_day`")
    private String startDay;

    /** 结束时间。 */
    @TableField(value = "`end_day`")
    private String endDay;

    /** 开始时间。 */
    @TableField(value = "`start_time`")
    private String startTime;

    /** 结束时间。 */
    @TableField(value = "`end_time`")
    private String endTime;

    /** 周一。 */
    @TableField(value = "`mon`")
    private Integer mon;

    /** 周二。 */
    @TableField(value = "`tue`")
    private Integer tue;

    /** 周三。 */
    @TableField(value = "`wed`")
    private Integer wed;

    /** 周四。 */
    @TableField(value = "`thu`")
    private Integer thu;

    /** 周五。 */
    @TableField(value = "`fri`")
    private Integer fri;

    /** 周六。 */
    @TableField(value = "`sat`")
    private Integer sat;

    /** 周天。 */
    @TableField(value = "`sun`")
    private Integer sun;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
