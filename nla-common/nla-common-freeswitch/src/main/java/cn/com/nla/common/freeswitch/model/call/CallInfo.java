package cn.com.nla.common.freeswitch.model.call;

import cn.com.nla.common.freeswitch.common.fs.Constant;
import cn.com.nla.common.freeswitch.enums.fs.CallTypeEunm;
import cn.com.nla.common.freeswitch.enums.fs.DirectionEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CallInfo implements Serializable {
    /**
     * 通话唯一标识
     */
    private String callId;

    /**
     * 会议号
     */
    private String conference;

    /**
     * 企业id
     */
    private String companyId;

    /**
     * 所在技能组id
     */
    private String groupId;

    /**
     * 隐藏客户号码(0:不隐藏;1:隐藏)
     */
    private int hiddenCustomer;

    /**
     * 主叫显号
     */
    private String callerDisplay;

    /**
     * 主叫
     */
    private String caller;

    /**
     * 被叫显号
     */
    private String calledDisplay;

    /**
     * 被叫
     */
    private String called;

    /**
     * 号码归属地
     */
    private String numberLocation;

    /**
     * 坐席
     */
    private String agentKey;

    /**
     * 坐席名称
     */
    private String agentName;
    /**
     * 坐席登录类型
     */
    private Integer loginType;

    /**
     * 媒体
     */
    private String mediaHost;
    /**
     * 服务地址
     */
    private String ctiHost;

    /**
     *
     */
    private String clientHost;

    /**
     * 录音地址
     */
    private String record;

    /**
     * 录音开始时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime recordTime;

    /**
     * 呼叫开始时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime callTime;

    /**
     * 呼叫类型
     */
    private CallTypeEunm callType;

    /**
     * 呼叫方向(2:外呼,1:呼入)
     */
    private DirectionEnum direction;

    /**
     * 通话标识(0:接通,1:坐席未接用户未接,2:坐席接通用户未接通,3:用户接通坐席未接通)
     */
    private Integer answerFlag;

    /**
     * 等待时长
     */
    private Long waitTime;

    /**
     * 应答设备数
     */
    private int answerCount;

    /**
     * 1主叫挂机, 2:被叫挂机, 3:平台挂机
     */
    private Integer hangupDir;

    /**
     * 挂机原因
     */
    private Integer hangupCode;


    /**
     * 接听时间，被叫 CHANNEL_ANSWER，转接不算
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime answerTime;

    /**
     * 最后一侧电话挂机时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime endTime;

    /**
     * 通话时长(秒)
     */
    private Long talkTime;

    /**
     * 第一次进队列时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime fristQueueTime;

    /**
     * 进入技能组时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime queueStartTime;

    /**
     * 出技能组时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime queueEndTime;

    /**
     * 溢出次数
     */
    private int overflowCount;

    /**
     * uuid1
     */
    private String uuid1;

    /**
     * uuid2
     */
    private String uuid2;

    /**
     * 话单通知地址
     */
    private String cdrNotifyUrl;

    /**
     * 排队等级，默认是进队列时间
     */
    private Long queueLevel;
    /**
     * sip通话中sdp信息
     */
    private String sipSdp;

    /**
     * 当前通话的设备
     */
    @Builder.Default
    private List<String> deviceList = new LinkedList<>();

    /**
     * K-V
     */
    @Builder.Default
    private Map<String, DeviceInfo> deviceInfoMap = new HashMap<>();

    /**
     * 呼叫随路数据(作为落单数据)
     */
    @Builder.Default
    private Map<String, Object> followData = new HashMap<>();

    /**
     * 模块流程间数据
     */
    @Builder.Default
    private Map<String, Object> processData = new HashMap<>();
    /**
     * 执行下一步命令
     */
    @Builder.Default
    private List<NextCommand> nextCommands = new LinkedList<>();

    /**
     * 电话流程
     */
    @Builder.Default
    private List<CallDetail> callDetails = new ArrayList<>();
}
