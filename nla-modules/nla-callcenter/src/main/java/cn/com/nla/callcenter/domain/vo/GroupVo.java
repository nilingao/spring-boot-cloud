package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.Group;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 技能组表视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = Group.class)
public class GroupVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private String name;
    private Integer afterInterval;
    private Long callerDisplayId;
    private Long calledDisplayId;
    private Integer recordType;
    private Integer levelValue;
    private Long ttsEngine;
    private String playContent;
    private Long evaluate;
    private Long queuePlay;
    private Long transferPlay;
    private Integer callTimeOut;
    private Integer groupType;
    private Integer notifyPosition;
    private Integer notifyRate;
    private String notifyContent;
    private Integer callMemory;
    private Integer status;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
