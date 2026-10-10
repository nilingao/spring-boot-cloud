package cn.com.nla.callcenter.domain.vo;

import cn.com.nla.callcenter.domain.FsMediaServer;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 流媒体服务信息视图；凭据不输出，号码按既有响应增强流程脱敏。
 * @author TZY
 */
@Data
@AutoMapper(target = FsMediaServer.class)
public class FsMediaServerVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String ip;
    private Integer sslStatus;
    private String hookIp;
    private String sdpIp;
    private String streamIp;
    private Integer httpPort;
    private Integer httpSslPort;
    private Integer rtmpPort;
    private Integer rtmpSslPort;
    private Integer rtpProxyPort;
    private Integer rtspPort;
    private Integer rtspSslPort;
    private Integer autoConfig;
    private Integer rtpEnable;
    private Integer enable;
    private LocalDateTime keepaliveTime;
    private Integer status;
    private String rtpPortRange;
    private Integer recordAssistPort;
    private Integer defaultServer;
    private Integer hookAliveInterval;
    private String videoPlayPrefix;
    private String videoHttpPrefix;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
