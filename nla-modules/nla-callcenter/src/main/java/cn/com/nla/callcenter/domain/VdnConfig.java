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

/** 呼入路由字码表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_vdn_config")
public class VdnConfig extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业ID。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 子码日程。 */
    @TableField(value = "`name`")
    private String name;

    /** 呼入路由编号。 */
    @TableField(value = "`vdn_id`")
    private Long vdnId;

    /** 日程id。 */
    @TableField(value = "`schedule_id`")
    private Long scheduleId;

    /** 路由类型(1:技能组,2:放音,3:ivr,4:坐席,5:外呼)。 */
    @TableField(value = "`route_type`")
    private Integer routeType;

    /** 路由类型值。 */
    @TableField(value = "`route_value`")
    private String routeValue;

    /** 放音类型(1:按键导航,2:技能组,3:ivr,4:路由字码,5:挂机)。 */
    @TableField(value = "`play_type`")
    private Integer playType;

    /** 放音类型对应值。 */
    @TableField(value = "`play_value`")
    private Long playValue;

    /** 结束音。 */
    @TableField(value = "`dtmf_end`")
    private String dtmfEnd;

    /** 重复播放次数。 */
    @TableField(value = "`retry`")
    private Integer retry;

    /** 最大收键长度。 */
    @TableField(value = "`dtmf_max`")
    private Integer dtmfMax;

    /** 最小收键长度。 */
    @TableField(value = "`dtmf_min`")
    private Integer dtmfMin;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
