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

/** 通话流程表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_call_detail")
public class CallDetail extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 开始时间。 */
    @TableField(value = "`start_time`")
    private Long startTime;

    /** 结束时间。 */
    @TableField(value = "`end_time`")
    private Long endTime;

    /** 通话ID。 */
    @TableField(value = "`call_id`")
    private Long callId;

    /** 顺序。 */
    @TableField(value = "`detail_index`")
    private Integer detailIndex;

    /** 1:进vdn,2:进ivr,3:技能组,4:按键收号,5:外线,6:机器人,10:服务评价。 */
    @TableField(value = "`transfer_type`")
    private Integer transferType;

    /** 转接ID。 */
    @TableField(value = "`transfer_id`")
    private Long transferId;

    /** 出队列原因:排队挂机或者转坐席。 */
    @TableField(value = "`reason`")
    private String reason;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
