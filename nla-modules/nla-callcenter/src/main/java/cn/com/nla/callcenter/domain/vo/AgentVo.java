package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.Agent;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import cn.com.nla.common.sensitive.annotation.Sensitive;
import cn.com.nla.common.sensitive.core.SensitiveStrategy;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/** 座席工号表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = Agent.class)
public class AgentVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private String agentId;
    private String agentKey;
    private String agentName;
    private String agentCode;
    private Integer agentType;
    private Integer record;
    private Long groupId;
    private Integer afterInterval;
    private Integer ringTime;
    private String host;
    private Integer state;
    private Integer status;
    private LocalDateTime registerTime;
    private LocalDateTime renewTime;
    private LocalDateTime keepaliveTime;
    private Integer keepTimeout;
    private Integer expires;
    private Integer streamMode;
    private Integer transport;
    private Integer charset;
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ToString.Exclude
    private String display;

    /** 非持久化查询字段，普通CRUD不计算。 */
    private List<String> sipPhoneList;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
