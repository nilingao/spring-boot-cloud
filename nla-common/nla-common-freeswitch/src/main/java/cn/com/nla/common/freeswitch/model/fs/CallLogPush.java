package cn.com.nla.common.freeswitch.model.fs;

import cn.com.nla.common.freeswitch.model.call.CallDetail;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * 话单推送信息
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CallLogPush implements Serializable {

    /**
     *
     */
    private Long id;

    /**
     * 企业id
     */
    private Long companyId;

    /**
     * 话单id
     */
    private Long callId;

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
    @JsonProperty(defaultValue = "0")
    private Long groupId;

    /**
     * 1:sip号,2:webrtc,3:手机
     */
    @JsonProperty(defaultValue = "1")
    private Integer loginType;

    /**
     * 任务ID
     */
    @JsonProperty(defaultValue = "0")
    private Long taskId;

    /**
     * ivr
     */
    @JsonProperty(defaultValue = "0")
    private Long ivrId;

    /**
     * 机器人id
     */
    @JsonProperty(defaultValue = "0")
    private Long botId;

    /**
     * 呼叫开始时间
     */
    @JsonProperty(defaultValue = "0")
    private Long callTime;

    /**
     * 接听时间
     */
    @JsonProperty(defaultValue = "0")
    private Long answerTime;

    /**
     * 结束时间
     */
    @JsonProperty(defaultValue = "0")
    private Long endTime;

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
    @JsonProperty(defaultValue = "0")
    private Integer answerFlag;

    /**
     * 累计等待时长
     */
    @JsonProperty(defaultValue = "0")
    private Long waitTime;

    /**
     * 应答设备数
     */
    @JsonProperty(defaultValue = "0")
    private Integer answerCount;

    /**
     * 挂机方向(1:主叫挂机,2:被叫挂机,3:系统挂机)
     */
    @JsonProperty(defaultValue = "3")
    private Integer hangupDir;

    /**
     * sdk挂机(1:坐席sdk挂机)
     */
    private Integer sdkHangup;

    /**
     * 挂机原因
     */
    private Integer hangupCode;

    /**
     * 录音文件下载地址
     */
    private String ossServer;

    /**
     * 媒体地址
     */
    private String mediaHost;

    /**
     * 服务地址
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
     * 录音地址
     */
    private String record2;

    /**
     * 录音地址
     */
    private String record3;

    /**
     * 录音状态
     */
    private Integer recordType;

    /**
     * 录音开始时间
     */
    @JsonProperty(defaultValue = "0")
    private Long recordTime;

    /**
     * 通话时长(秒)
     */
    @JsonProperty(defaultValue = "0")
    private Long talkTime;

    /**
     * 第一次进队列时间
     */
    @JsonProperty(defaultValue = "0")
    private Long fristQueueTime;

    /**
     * 进入技能组时间
     */
    @JsonProperty(defaultValue = "0")
    private Long queueStartTime;

    /**
     * 出技能组时间
     */
    @JsonProperty(defaultValue = "0")
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
     * 扩展1
     */
    private String uuid1;

    /**
     * 扩展2
     */
    private String uuid2;

    /**
     * 扩展3
     */
    private String ext1;

    /**
     * 扩展4
     */
    private String ext2;

    /**
     * 扩展5
     */
    private String ext3;

    private List<CallDeviceInfo> callDeviceList;

    private List<CallDetail> callDetailList;
}
