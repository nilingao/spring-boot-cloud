package cn.com.nla.common.freeswitch.model.fs;

import cn.com.nla.common.freeswitch.common.fs.Constant;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 话单信息
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CallLogInfo implements Serializable {

    /**
     *
     */
    private Long id;

    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime createTime;
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime updateTime;

    /**
     * 企业id
     */
    private String companyId;

    /**
     * 话单id
     */
    private String callId;

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
     * 技能组
     */
    private String groupId;

    /**
     * 1:sip号,2:webrtc,3:手机
     */
    private Integer loginType;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * ivr
     */
    private Long ivrId;

    /**
     * 机器人id
     */
    private Long botId;

    /**
     * 呼叫开始时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime callTime;

    /**
     * 接听时间
     */
    private Long answerTime;

    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime endTime;

    /**
     * 呼叫类型
     */
    private String callType;

    /**
     * 呼叫方向
     */
    private String direction;

    /**
     * 通话标识(0:接通,1:坐席未接用户未接,2:坐席接通用户未接通,3:用户接通坐席未接通)
     */
    private Integer answerFlag;

    /**
     * 累计等待时长
     */
    private Long waitTime;

    /**
     * 应答设备数
     */
    private Integer answerCount;

    /**
     * 挂机方向(1:主叫挂机,2:被叫挂机,3:系统挂机)
     */
    private Integer hangupDir;

    /**
     * 是否sdk挂机(1:sdk挂机)
     */
    private Integer sdkHangup;

    /**
     * 挂机原因
     */
    private Integer hangupCode;

    /**
     * 媒体服务器
     */
    private String mediaHost;

    /**
     * cti地址
     */
    private String ctiHost;

    /**
     * 客户端地址
     */
    private String clientHost;

    /**
     * 录音地址
     */
    private String record;

    /**
     * 备用录音地址
     */
    private String record2;

    /**
     * 备用录音地址
     */
    private String record3;

    /**
     * 录音状态
     */
    private Integer recordType;

    /**
     * 录音开始时间
     */
    private Long recordStartTime;

    /**
     * 录音时间
     */
    private Long recordTime;

    /**
     * 通话时长
     */
    private Long talkTime;

    /**
     * 第一次进队列时间
     */
    private Long fristQueueTime;

    /**
     * 进队列时间
     */
    private Long queueStartTime;

    /**
     * 出队列时间
     */
    private Long queueEndTime;

    /**
     * 月份
     */
    private String monthTime;

    /**
     * 通话随路数据(2048)
     */
    private String followData;
    /**
     * 状态
     */
    private Integer status;
}
