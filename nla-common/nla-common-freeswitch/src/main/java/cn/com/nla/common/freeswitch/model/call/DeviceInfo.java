package cn.com.nla.common.freeswitch.model.call;

import cn.com.nla.common.freeswitch.common.fs.Constant;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DeviceInfo implements Serializable {
    /**
     * 通话唯一标识
     */
    private String callId;

    /**
     * 会议模式
     */
    private String conference;

    /**
     * 设备id
     */
    private String deviceId;

    /**
     * 坐席
     */
    private String agentKey;

    /**
     * 坐席
     */
    private String agentName;

    /**
     * 1:坐席,2:客户,3:外线
     */
    private Integer deviceType;

    /**
     * 1:呼入,2:外呼,3:内呼,4:转接,5:咨询,6:监听,7:强插,8:耳语 9:会议
     */
    private Integer cdrType;

    /**
     * 咨询或转接来源
     */
    private String fromAgent;

    /**
     * 主叫
     */
    private String caller;

    /**
     * 被叫
     */
    private String called;

    /**
     * 显号
     */
    private String display;

    /**
     * 被叫归属地
     */
    private String calledLocation;

    /**
     * 被叫归属地
     */
    private String callerLocation;

    /**
     * 呼叫开始时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime callTime;

    /**
     * 振铃开始时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime ringStartTime;

    /**
     * 振铃结束时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime ringEndTime;

    /**
     * 接通时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime answerTime;

    /**
     * 桥接时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime bridgeTime;

    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime endTime;

    /**
     * 通话时长
     */
    private Long talkTime;

    /**
     * 信令协议(tcp/udp)
     */
    private String sipProtocol;

    /**
     * 呼叫地址
     */
    private String channelName;

    /**
     * 挂机原因
     */
    private String hangupCause;

    /**
     * 回铃音识别
     */
    private String ringCause;

    /**
     * sip状态
     */
    private String sipStatus;

    /**
     * 录音地址
     */
    private String record;

    /**
     * 录音时长
     */
    private Long recordTime;

    /**
     * 录音开始时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime recordStartTime;

    /**
     * 当前设备状态
     */
    private String state;
}
