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

/** 坐席状态历史表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_agent_state_log")
public class AgentStateLog extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业id。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 主技能组id。 */
    @TableField(value = "`group_id`")
    private Long groupId;

    /** 坐席id。 */
    @TableField(value = "`agent_id`")
    private Long agentId;

    /** 坐席编号。 */
    @TableField(value = "`agent_key`")
    private String agentKey;

    /** 坐席名称。 */
    @TableField(value = "`agent_name`")
    private String agentName;

    /** 通话唯一标识。 */
    @TableField(value = "`call_id`")
    private Long callId;

    /** 登录类型。 */
    @TableField(value = "`login_type`")
    private Integer loginType;

    /** 工作类型。 */
    @TableField(value = "`work_type`")
    private Integer workType;

    /** 服务站点。 */
    @TableField(value = "`host`")
    private String host;

    /** 远端地址。 */
    @TableField(value = "`remote_address`")
    private String remoteAddress;

    /** 变更之前状态。 */
    @TableField(value = "`before_state`")
    private String beforeState;

    /** 更变之前时间。 */
    @TableField(value = "`before_time`")
    private Long beforeTime;

    /** 变更之后状态。 */
    @TableField(value = "`state`")
    private String state;

    /** 当前时间(秒)。 */
    @TableField(value = "`state_time`")
    private Long stateTime;

    /** 持续时间(秒)。 */
    @TableField(value = "`duration`")
    private Integer duration;

    /** 忙碌类型。 */
    @TableField(value = "`busy_desc`")
    private String busyDesc;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
