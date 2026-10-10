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

/** 话单表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_call_log")
public class CallLog extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业id。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 话单id。 */
    @TableField(value = "`call_id`")
    private Long callId;

    /** 主叫显号。 */
    @TableField(value = "`caller_display`")
    @ToString.Exclude
    private String callerDisplay;

    /** 主叫。 */
    @TableField(value = "`caller`")
    @ToString.Exclude
    private String caller;

    /** 被叫显号。 */
    @TableField(value = "`called_display`")
    @ToString.Exclude
    private String calledDisplay;

    /** 被叫。 */
    @TableField(value = "`called`")
    @ToString.Exclude
    private String called;

    /** 客户号码归属地。 */
    @TableField(value = "`number_location`")
    private String numberLocation;

    /** 坐席。 */
    @TableField(value = "`agent_key`")
    private String agentKey;

    /** 坐席名称。 */
    @TableField(value = "`agent_name`")
    private String agentName;

    /** 技能组。 */
    @TableField(value = "`group_id`")
    private Long groupId;

    /** 1:sip号,2:webrtc,3:手机。 */
    @TableField(value = "`login_type`")
    private Integer loginType;

    /** 任务ID。 */
    @TableField(value = "`task_id`")
    private Long taskId;

    /** ivr。 */
    @TableField(value = "`ivr_id`")
    private Long ivrId;

    /** 机器人id。 */
    @TableField(value = "`bot_id`")
    private Long botId;

    /** 呼叫开始时间。 */
    @TableField(value = "`call_time`")
    private Long callTime;

    /** 接听时间。 */
    @TableField(value = "`answer_time`")
    private Long answerTime;

    /** 结束时间。 */
    @TableField(value = "`end_time`")
    private Long endTime;

    /** 呼叫类型。 */
    @TableField(value = "`call_type`")
    private String callType;

    /** 呼叫方向。 */
    @TableField(value = "`direction`")
    private String direction;

    /** 通话标识(0:接通,1:坐席未接用户未接,2:坐席接通用户未接通,3:用户接通坐席未接通)。 */
    @TableField(value = "`answer_flag`")
    private Integer answerFlag;

    /** 累计等待时长。 */
    @TableField(value = "`wait_time`")
    private Long waitTime;

    /** 应答设备数。 */
    @TableField(value = "`answer_count`")
    private Integer answerCount;

    /** 挂机方向(1:主叫挂机,2:被叫挂机,3:系统挂机)。 */
    @TableField(value = "`hangup_dir`")
    private Integer hangupDir;

    /** 是否sdk挂机(1:sdk挂机)。 */
    @TableField(value = "`sdk_hangup`")
    private Integer sdkHangup;

    /** 挂机原因。 */
    @TableField(value = "`hangup_code`")
    private Integer hangupCode;

    /** 媒体服务器。 */
    @TableField(value = "`media_host`")
    private String mediaHost;

    /** cti地址。 */
    @TableField(value = "`cti_host`")
    private String ctiHost;

    /** 客户端地址。 */
    @TableField(value = "`client_host`")
    private String clientHost;

    /** 录音地址。 */
    @TableField(value = "`record`")
    private String record;

    /** 备用录音地址。 */
    @TableField(value = "`record2`")
    private String record2;

    /** 备用录音地址。 */
    @TableField(value = "`record3`")
    private String record3;

    /** 录音状态。 */
    @TableField(value = "`record_type`")
    private Integer recordType;

    /** 录音开始时间。 */
    @TableField(value = "`record_start_time`")
    private Long recordStartTime;

    /** 录音时间。 */
    @TableField(value = "`record_time`")
    private Long recordTime;

    /** 通话时长。 */
    @TableField(value = "`talk_time`")
    private Long talkTime;

    /** 第一次进队列时间。 */
    @TableField(value = "`frist_queue_time`")
    private Long fristQueueTime;

    /** 进队列时间。 */
    @TableField(value = "`queue_start_time`")
    private Long queueStartTime;

    /** 出队列时间。 */
    @TableField(value = "`queue_end_time`")
    private Long queueEndTime;

    /** 月份。 */
    @TableField(value = "`month_time`")
    private String monthTime;

    /** 通话随路数据(2048)。 */
    @TableField(value = "`follow_data`")
    private String followData;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
