package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.RouteGateway;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 媒体网关表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = RouteGateway.class)
public class RouteGatewayVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String mediaHost;
    private Integer mediaPort;
    private String callerPrefix;
    private String calledPrefix;
    private String profile;
    private String sipHeader1;
    private String sipHeader2;
    private String sipHeader3;
    private Integer status;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
