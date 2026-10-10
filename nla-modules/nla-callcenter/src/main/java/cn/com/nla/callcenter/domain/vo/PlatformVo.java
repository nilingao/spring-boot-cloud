package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.Platform;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 平台信息视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = Platform.class)
public class PlatformVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String localIp;
    private String remoteIp;
    private Integer internalPort;
    private Integer externalPort;
    private Integer startRtpPort;
    private Integer endRtpPort;
    private Integer wsPort;
    private Integer wssPort;
    private String audioCode;
    private String videoCode;
    private String frameRate;
    private String bitRate;
    private Integer iceStart;
    private String stunAddress;
    private String name;
    private Integer enable;
    private Integer status;
    private Integer audioRecord;
    private Integer videoRecord;
    private String audioRecordPath;
    private String videoRecordPath;
    private String soundRilePath;
    private String freeswitchPath;
    private String freeswitchLogPath;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
