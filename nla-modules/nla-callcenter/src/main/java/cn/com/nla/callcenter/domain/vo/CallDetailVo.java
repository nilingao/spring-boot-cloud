package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.CallDetail;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 通话流程表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = CallDetail.class)
public class CallDetailVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long startTime;
    private Long endTime;
    private Long callId;
    private Integer detailIndex;
    private Integer transferType;
    private Long transferId;
    private String reason;
    private Integer status;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
