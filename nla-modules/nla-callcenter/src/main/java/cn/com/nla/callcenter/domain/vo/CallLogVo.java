package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.CallLog;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import cn.com.nla.common.sensitive.annotation.Sensitive;
import cn.com.nla.common.sensitive.core.SensitiveStrategy;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 话单表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = CallLog.class)
public class CallLogVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private Long callId;
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ToString.Exclude
    private String callerDisplay;
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ToString.Exclude
    private String caller;
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ToString.Exclude
    private String calledDisplay;
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ToString.Exclude
    private String called;
    private String numberLocation;
    private String agentKey;
    private String agentName;
    private Long groupId;
    private Integer loginType;
    private Long taskId;
    private Long ivrId;
    private Long botId;
    private Long callTime;
    private Long answerTime;
    private Long endTime;
    private String callType;
    private String direction;
    private Integer answerFlag;
    private Long waitTime;
    private Integer answerCount;
    private Integer hangupDir;
    private Integer sdkHangup;
    private Integer hangupCode;
    private String mediaHost;
    private String ctiHost;
    private String clientHost;
    private String record;
    private String record2;
    private String record3;
    private Integer recordType;
    private Long recordStartTime;
    private Long recordTime;
    private Long talkTime;
    private Long fristQueueTime;
    private Long queueStartTime;
    private Long queueEndTime;
    private String monthTime;
    private String followData;
    private Integer status;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
