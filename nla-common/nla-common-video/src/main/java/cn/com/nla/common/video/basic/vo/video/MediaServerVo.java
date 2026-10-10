package cn.com.nla.common.video.basic.vo.video;

import cn.com.nla.common.video.basic.model.StringIdEntity;
import cn.com.nla.common.video.basic.common.DateFormats;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
* 流媒体服务信息
*/
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class MediaServerVo extends StringIdEntity {
    /**
     * IP
     */
    private String ip;

    /**
     * hook使用的IP（zlm访问平台使用的IP）
     */
    private String hookIp;

    /**
     * 是否ssl链接
     */
    private Integer sslStatus;

    /**
     * SDP IP
     */
    private String sdpIp;

    /**
     * 流IP
     */
    private String streamIp;

    /**
     * HTTP端口
     */
    private Integer httpPort;

    /**
     * HTTPS端口
     */
    private Integer httpSslPort;

    /**
     * RTMP端口
     */
    private Integer rtmpPort;

    /**
     * RTMPS端口
     */
    private Integer rtmpSslPort;

    /**
     * RTP收流端口（单端口模式有用）
     */
    private Integer rtpProxyPort;

    /**
     * RTSP端口
     */
    private Integer rtspPort;

    /**
     * RTSPS端口
     */
    private Integer rtspSslPort;

    /**
     * 是否开启自动配置ZLM
     */
    private Integer autoConfig;

    /**
     * ZLM鉴权参数
     */
    private String secret;

    /**
     * 是否使用多端口模式
     */
    private Integer rtpEnable;

    /**
     * 启用状态
     */
    private Integer enable;

    /**
     * 心跳时间
     */

    @JsonFormat(pattern =  DateFormats.DATE_TIME_FORMAT)
    private LocalDateTime keepaliveTime;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 多端口RTP收流端口范围
     */
    private String rtpPortRange;

    /**
     * assist服务端口
     */
    private Integer recordAssistPort;

    /**
     * 是否是默认ZLM
     */
    private Integer defaultServer;

    /**
     * keepalive hook触发间隔,单位秒
     */
    private Integer hookAliveInterval;


    /**
     * 流媒体播放 代理前缀
     */
    private String  videoPlayPrefix;

    /**
     * video请求时前缀
     */
    private String  videoHttpPrefix;
}