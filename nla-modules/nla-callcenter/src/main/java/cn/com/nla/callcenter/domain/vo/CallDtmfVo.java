package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.CallDtmf;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 呼叫按键表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = CallDtmf.class)
public class CallDtmfVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String dtmfKey;
    private Long processId;
    private Long callId;
    private Long dtmfTime;
    private Integer status;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
