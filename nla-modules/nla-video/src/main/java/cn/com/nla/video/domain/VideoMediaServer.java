package cn.com.nla.video.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.time.LocalDateTime;

/** 流媒体服务信息；业务服务与协议接入在阶段 6.5 实现。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_media_server")
public class VideoMediaServer extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 协议字符串主键，由调用方提供。 */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** IP。 */
    @TableField(value = "`ip`")
    private String ip;

    /** 是否https。 */
    @TableField(value = "`ssl_status`")
    private Integer sslStatus;

    /** hook使用的IP（zlm访问平台使用的IP）。 */
    @TableField(value = "`hook_ip`")
    private String hookIp;

    /** SDP IP。 */
    @TableField(value = "`sdp_ip`")
    private String sdpIp;

    /** 流IP。 */
    @TableField(value = "`stream_ip`")
    private String streamIp;

    /** HTTP端口。 */
    @TableField(value = "`http_port`")
    private Integer httpPort;

    /** HTTPS端口。 */
    @TableField(value = "`http_ssl_port`")
    private Integer httpSslPort;

    /** RTMP端口。 */
    @TableField(value = "`rtmp_port`")
    private Integer rtmpPort;

    /** RTMPS端口。 */
    @TableField(value = "`rtmp_ssl_port`")
    private Integer rtmpSslPort;

    /** RTP收流端口（单端口模式有用）。 */
    @TableField(value = "`rtp_proxy_port`")
    private Integer rtpProxyPort;

    /** RTSP端口。 */
    @TableField(value = "`rtsp_port`")
    private Integer rtspPort;

    /** RTSPS端口。 */
    @TableField(value = "`rtsp_ssl_port`")
    private Integer rtspSslPort;

    /** 是否开启自动配置ZLM。 */
    @TableField(value = "`auto_config`")
    private Integer autoConfig;

    /** ZLM鉴权参数。 */
    @TableField(value = "`secret`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String secret;

    /** 是否使用多端口模式。 */
    @TableField(value = "`rtp_enable`")
    private Integer rtpEnable;

    /** 启用状态。 */
    @TableField(value = "`enable`")
    private Integer enable;

    /** 心跳时间。 */
    @TableField(value = "`keepalive_time`")
    private LocalDateTime keepaliveTime;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 多端口RTP收流端口范围。 */
    @TableField(value = "`rtp_port_range`")
    private String rtpPortRange;

    /** assist服务端口。 */
    @TableField(value = "`record_assist_port`")
    private Integer recordAssistPort;

    /** 是否是默认ZLM。 */
    @TableField(value = "`default_server`")
    private Integer defaultServer;

    /** keepalive hook触发间隔,单位秒。 */
    @TableField(value = "`hook_alive_interval`")
    private Integer hookAliveInterval;

    /** 流媒体播放 代理前缀。 */
    @TableField(value = "`video_play_prefix`")
    private String videoPlayPrefix;

    /** video请求时前缀。 */
    @TableField(value = "`video_http_prefix`")
    private String videoHttpPrefix;

    /** 删除标志：0 存在、1 删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
