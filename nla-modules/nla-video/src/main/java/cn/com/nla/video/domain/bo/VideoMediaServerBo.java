package cn.com.nla.video.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.video.domain.VideoMediaServer;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 流媒体服务信息输入，不接受审计与删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = VideoMediaServer.class, reverseConvertGenerate = false)
public class VideoMediaServerBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "协议主键不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 255, groups = {AddGroup.class, EditGroup.class})
    private String id;

    /** IP。 */
    @NotBlank(message = "ip不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "ip长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String ip;

    /** 是否https。 */
    private Integer sslStatus;

    /** hook使用的IP（zlm访问平台使用的IP）。 */
    @NotBlank(message = "hookIp不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "hookIp长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String hookIp;

    /** SDP IP。 */
    @NotBlank(message = "sdpIp不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "sdpIp长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String sdpIp;

    /** 流IP。 */
    @Size(max = 50, message = "streamIp长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String streamIp;

    /** HTTP端口。 */
    @Min(value = 0, message = "httpPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer httpPort;

    /** HTTPS端口。 */
    @Min(value = 0, message = "httpSslPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer httpSslPort;

    /** RTMP端口。 */
    @Min(value = 0, message = "rtmpPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer rtmpPort;

    /** RTMPS端口。 */
    @Min(value = 0, message = "rtmpSslPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer rtmpSslPort;

    /** RTP收流端口（单端口模式有用）。 */
    @Min(value = 0, message = "rtpProxyPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer rtpProxyPort;

    /** RTSP端口。 */
    @Min(value = 0, message = "rtspPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer rtspPort;

    /** RTSPS端口。 */
    @Min(value = 0, message = "rtspSslPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer rtspSslPort;

    /** 是否开启自动配置ZLM。 */
    private Integer autoConfig;

    /** ZLM鉴权参数。 */
    @Size(max = 50, message = "secret长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String secret;

    /** 是否使用多端口模式。 */
    private Integer rtpEnable;

    /** 启用状态。 */
    private Integer enable;

    /** 心跳时间。 */
    private LocalDateTime keepaliveTime;

    /** 状态。 */
    private Integer status;

    /** 多端口RTP收流端口范围。 */
    @Size(max = 50, message = "rtpPortRange长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String rtpPortRange;

    /** assist服务端口。 */
    @Min(value = 0, message = "recordAssistPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer recordAssistPort;

    /** 是否是默认ZLM。 */
    private Integer defaultServer;

    /** keepalive hook触发间隔,单位秒。 */
    @Min(value = 0, message = "hookAliveInterval不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer hookAliveInterval;

    /** 流媒体播放 代理前缀。 */
    @Size(max = 50, message = "videoPlayPrefix长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String videoPlayPrefix;

    /** video请求时前缀。 */
    @Size(max = 50, message = "videoHttpPrefix长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String videoHttpPrefix;
}
