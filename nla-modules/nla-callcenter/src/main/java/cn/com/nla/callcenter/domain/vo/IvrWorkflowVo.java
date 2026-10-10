package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.IvrWorkflow;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** ivr流程表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = IvrWorkflow.class)
public class IvrWorkflowVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private String name;
    private String ossId;
    private String initParams;
    private String createUser;
    private String verifyUser;
    private String content;
    private String voiceItem;
    private Integer type;
    private Integer status;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
