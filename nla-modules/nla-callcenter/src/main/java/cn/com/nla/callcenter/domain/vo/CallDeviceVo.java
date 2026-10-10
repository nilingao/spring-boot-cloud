package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.CallDevice;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import cn.com.nla.common.sensitive.annotation.Sensitive;
import cn.com.nla.common.sensitive.core.SensitiveStrategy;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 话单明细表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = CallDevice.class)
public class CallDeviceVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private Long callId;
    private String deviceId;
    private String agentKey;
    private String agentName;
    private Integer deviceType;
    private Integer cdrType;
    private String fromAgent;
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ToString.Exclude
    private String caller;
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ToString.Exclude
    private String called;
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ToString.Exclude
    private String display;
    private String calledLocation;
    private String callerLocation;
    private Long callTime;
    private Long ringStartTime;
    private Long ringEndTime;
    private Long answerTime;
    private Long bridgeTime;
    private Long endTime;
    private Long talkTime;
    private Long recordStartTime;
    private Long recordTime;
    private String sipProtocol;
    private String record;
    private String record2;
    private String record3;
    private String channelName;
    private String hangupCause;
    private String ringCause;
    private String sipStatus;
    private Integer status;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
