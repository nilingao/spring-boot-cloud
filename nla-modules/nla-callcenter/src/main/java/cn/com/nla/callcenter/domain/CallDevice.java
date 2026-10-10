package cn.com.nla.callcenter.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;

/** 话单明细表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_call_device")
public class CallDevice extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业id。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 通话ID。 */
    @TableField(value = "`call_id`")
    private Long callId;

    /** 设备id。 */
    @TableField(value = "`device_id`")
    private String deviceId;

    /** 坐席。 */
    @TableField(value = "`agent_key`")
    private String agentKey;

    /** 坐席名称。 */
    @TableField(value = "`agent_name`")
    private String agentName;

    /** 1:坐席,2:客户,3:外线。 */
    @TableField(value = "`device_type`")
    private Integer deviceType;

    /** 1:呼入,2:外呼,3:内呼,4:转接,5:咨询,6:监听,7:强插。 */
    @TableField(value = "`cdr_type`")
    private Integer cdrType;

    /** 转接或咨询发起者。 */
    @TableField(value = "`from_agent`")
    private String fromAgent;

    /** 主叫。 */
    @TableField(value = "`caller`")
    @ToString.Exclude
    private String caller;

    /** 被叫。 */
    @TableField(value = "`called`")
    @ToString.Exclude
    private String called;

    /** 显号。 */
    @TableField(value = "`display`")
    @ToString.Exclude
    private String display;

    /** 被叫归属地。 */
    @TableField(value = "`called_location`")
    private String calledLocation;

    /** 被叫归属地。 */
    @TableField(value = "`caller_location`")
    private String callerLocation;

    /** 呼叫开始时间。 */
    @TableField(value = "`call_time`")
    private Long callTime;

    /** 振铃开始时间。 */
    @TableField(value = "`ring_start_time`")
    private Long ringStartTime;

    /** 振铃结束时间。 */
    @TableField(value = "`ring_end_time`")
    private Long ringEndTime;

    /** 接通时间。 */
    @TableField(value = "`answer_time`")
    private Long answerTime;

    /** 桥接时间。 */
    @TableField(value = "`bridge_time`")
    private Long bridgeTime;

    /** 结束时间。 */
    @TableField(value = "`end_time`")
    private Long endTime;

    /** 通话时长。 */
    @TableField(value = "`talk_time`")
    private Long talkTime;

    /** 录音开始时间。 */
    @TableField(value = "`record_start_time`")
    private Long recordStartTime;

    /** 录音时长。 */
    @TableField(value = "`record_time`")
    private Long recordTime;

    /** 信令协议(tcp/udp)。 */
    @TableField(value = "`sip_protocol`")
    private String sipProtocol;

    /** 录音地址。 */
    @TableField(value = "`record`")
    private String record;

    /** 备用录音地址。 */
    @TableField(value = "`record2`")
    private String record2;

    /** 备用录音地址。 */
    @TableField(value = "`record3`")
    private String record3;

    /** 呼叫地址。 */
    @TableField(value = "`channel_name`")
    private String channelName;

    /** 挂机原因。 */
    @TableField(value = "`hangup_cause`")
    private String hangupCause;

    /** 回铃音识别。 */
    @TableField(value = "`ring_cause`")
    private String ringCause;

    /** sip状态。 */
    @TableField(value = "`sip_status`")
    private String sipStatus;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
