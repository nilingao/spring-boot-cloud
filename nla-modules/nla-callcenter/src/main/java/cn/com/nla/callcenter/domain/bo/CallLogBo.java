package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.CallLog;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/** 话单表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = CallLog.class, reverseConvertGenerate = false)
public class CallLogBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业id。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 话单id。 */
    @Min(value = 0, message = "callId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long callId;

    /** 主叫显号。 */
    @Size(max = 100, message = "callerDisplay长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String callerDisplay;

    /** 主叫。 */
    @Size(max = 100, message = "caller长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String caller;

    /** 被叫显号。 */
    @Size(max = 100, message = "calledDisplay长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String calledDisplay;

    /** 被叫。 */
    @Size(max = 100, message = "called长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String called;

    /** 客户号码归属地。 */
    @Size(max = 100, message = "numberLocation长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String numberLocation;

    /** 坐席。 */
    @Size(max = 50, message = "agentKey长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String agentKey;

    /** 坐席名称。 */
    @Size(max = 255, message = "agentName长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String agentName;

    /** 技能组。 */
    @Min(value = 0, message = "groupId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long groupId;

    /** 1:sip号,2:webrtc,3:手机。 */
    @Min(value = 0, message = "loginType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer loginType;

    /** 任务ID。 */
    @Min(value = 0, message = "taskId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long taskId;

    /** ivr。 */
    @Min(value = 0, message = "ivrId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long ivrId;

    /** 机器人id。 */
    @Min(value = 0, message = "botId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long botId;

    /** 呼叫开始时间。 */
    @Min(value = 0, message = "callTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long callTime;

    /** 接听时间。 */
    @Min(value = 0, message = "answerTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long answerTime;

    /** 结束时间。 */
    @Min(value = 0, message = "endTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long endTime;

    /** 呼叫类型。 */
    @Size(max = 32, message = "callType长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String callType;

    /** 呼叫方向。 */
    @Size(max = 32, message = "direction长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String direction;

    /** 通话标识(0:接通,1:坐席未接用户未接,2:坐席接通用户未接通,3:用户接通坐席未接通)。 */
    @Min(value = 0, message = "answerFlag不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer answerFlag;

    /** 累计等待时长。 */
    @Min(value = 0, message = "waitTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long waitTime;

    /** 应答设备数。 */
    @Min(value = 0, message = "answerCount不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer answerCount;

    /** 挂机方向(1:主叫挂机,2:被叫挂机,3:系统挂机)。 */
    @Min(value = 0, message = "hangupDir不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer hangupDir;

    /** 是否sdk挂机(1:sdk挂机)。 */
    @Min(value = 0, message = "sdkHangup不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer sdkHangup;

    /** 挂机原因。 */
    @Min(value = 0, message = "hangupCode不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer hangupCode;

    /** 媒体服务器。 */
    @Size(max = 255, message = "mediaHost长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String mediaHost;

    /** cti地址。 */
    @Size(max = 255, message = "ctiHost长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String ctiHost;

    /** 客户端地址。 */
    @Size(max = 255, message = "clientHost长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String clientHost;

    /** 录音地址。 */
    @Size(max = 255, message = "record长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String record;

    /** 备用录音地址。 */
    @Size(max = 255, message = "record2长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String record2;

    /** 备用录音地址。 */
    @Size(max = 255, message = "record3长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String record3;

    /** 录音状态。 */
    @Min(value = 0, message = "recordType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer recordType;

    /** 录音开始时间。 */
    @Min(value = 0, message = "recordStartTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long recordStartTime;

    /** 录音时间。 */
    @Min(value = 0, message = "recordTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long recordTime;

    /** 通话时长。 */
    @Min(value = 0, message = "talkTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long talkTime;

    /** 第一次进队列时间。 */
    @Min(value = 0, message = "fristQueueTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long fristQueueTime;

    /** 进队列时间。 */
    @Min(value = 0, message = "queueStartTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long queueStartTime;

    /** 出队列时间。 */
    @Min(value = 0, message = "queueEndTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long queueEndTime;

    /** 月份。 */
    @Size(max = 50, message = "monthTime长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String monthTime;

    /** 通话随路数据(2048)。 */
    @Size(max = 4096, message = "followData长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String followData;

    /** 状态。 */
    private Integer status;
}
