package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.AgentStateLog;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 坐席状态历史表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = AgentStateLog.class)
public class AgentStateLogVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private Long groupId;
    private Long agentId;
    private String agentKey;
    private String agentName;
    private Long callId;
    private Integer loginType;
    private Integer workType;
    private String host;
    private String remoteAddress;
    private String beforeState;
    private Long beforeTime;
    private String state;
    private Long stateTime;
    private Integer duration;
    private String busyDesc;
    private Integer status;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
