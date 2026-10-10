package cn.com.nla.video.domain.vo;

import cn.com.nla.video.domain.VideoMediaServer;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 流媒体服务信息视图，不包含凭据、原始流地址或删除标志。
 * @author TZY
 */
@Data
@AutoMapper(target = VideoMediaServer.class)
public class VideoMediaServerVo implements Serializable {
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
