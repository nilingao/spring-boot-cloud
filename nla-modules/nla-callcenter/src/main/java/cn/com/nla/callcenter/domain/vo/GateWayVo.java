package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.GateWay;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 网关中继信息视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = GateWay.class)
public class GateWayVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String routeId;
    private String realm;
    private Integer register;
    private Integer transport;
    private Integer retrySeconds;
    private Integer username;
    private String selected;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
