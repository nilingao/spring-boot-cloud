package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.CallDevice;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/** 话单明细表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = CallDevice.class, reverseConvertGenerate = false)
public class CallDeviceBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业id。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 通话ID。 */
    @Min(value = 0, message = "callId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long callId;

    /** 设备id。 */
    @Size(max = 50, message = "deviceId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String deviceId;

    /** 坐席。 */
    @Size(max = 50, message = "agentKey长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String agentKey;

    /** 坐席名称。 */
    @Size(max = 50, message = "agentName长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String agentName;

    /** 1:坐席,2:客户,3:外线。 */
    @Min(value = 0, message = "deviceType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer deviceType;

    /** 1:呼入,2:外呼,3:内呼,4:转接,5:咨询,6:监听,7:强插。 */
    @Min(value = 0, message = "cdrType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer cdrType;

    /** 转接或咨询发起者。 */
    @Size(max = 50, message = "fromAgent长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String fromAgent;

    /** 主叫。 */
    @Size(max = 50, message = "caller长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String caller;

    /** 被叫。 */
    @Size(max = 50, message = "called长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String called;

    /** 显号。 */
    @Size(max = 50, message = "display长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String display;

    /** 被叫归属地。 */
    @Size(max = 100, message = "calledLocation长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String calledLocation;

    /** 被叫归属地。 */
    @Size(max = 100, message = "callerLocation长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String callerLocation;

    /** 呼叫开始时间。 */
    @Min(value = 0, message = "callTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long callTime;

    /** 振铃开始时间。 */
    @Min(value = 0, message = "ringStartTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long ringStartTime;

    /** 振铃结束时间。 */
    @Min(value = 0, message = "ringEndTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long ringEndTime;

    /** 接通时间。 */
    @Min(value = 0, message = "answerTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long answerTime;

    /** 桥接时间。 */
    @Min(value = 0, message = "bridgeTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long bridgeTime;

    /** 结束时间。 */
    @Min(value = 0, message = "endTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long endTime;

    /** 通话时长。 */
    @Min(value = 0, message = "talkTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long talkTime;

    /** 录音开始时间。 */
    @Min(value = 0, message = "recordStartTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long recordStartTime;

    /** 录音时长。 */
    @Min(value = 0, message = "recordTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long recordTime;

    /** 信令协议(tcp/udp)。 */
    @Size(max = 50, message = "sipProtocol长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String sipProtocol;

    /** 录音地址。 */
    @Size(max = 255, message = "record长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String record;

    /** 备用录音地址。 */
    @Size(max = 255, message = "record2长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String record2;

    /** 备用录音地址。 */
    @Size(max = 255, message = "record3长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String record3;

    /** 呼叫地址。 */
    @Size(max = 50, message = "channelName长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String channelName;

    /** 挂机原因。 */
    @NotBlank(message = "hangupCause不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "hangupCause长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String hangupCause;

    /** 回铃音识别。 */
    @Size(max = 50, message = "ringCause长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String ringCause;

    /** sip状态。 */
    @Size(max = 50, message = "sipStatus长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String sipStatus;

    /** 状态。 */
    private Integer status;
}
