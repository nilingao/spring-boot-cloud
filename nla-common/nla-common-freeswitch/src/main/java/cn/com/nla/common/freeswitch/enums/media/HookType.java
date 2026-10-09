package cn.com.nla.common.freeswitch.enums.media;

import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * hook类型
 * @author lin
 */

@Getter
public enum HookType implements Serializable {

    on_flow_report("ON_FLOW_REPORT","流量统计事件"),
    on_http_access("ON_HTTP_ACCESS","访问http文件服务器上hls之外的文件时触发"),
    on_play("ON_PLAY","播放器鉴权事件"),
    on_publish("ON_PUBLISH","rtsp/rtmp/rtp推流鉴权事件"),
    on_record_mp4("ON_RECORD_MP4","录制mp4完成后通知事件"),
    on_rtsp_auth("ON_RTSP_AUTH","该rtsp流是否开启rtsp专用方式的鉴权事件"),
    on_rtsp_realm("ON_RTSP_REALM","rtsp专用的鉴权事件，先触发on_rtsp_realm事件然后才会触发on_rtsp_auth事件。"),
    on_shell_login("ON_SHELL_LOGIN","shell登录鉴权"),
    on_stream_changed("ON_STREAM_CHANGED","rtsp/rtmp流注册或注销时触发此事件；此事件对回复不敏感。"),
    on_stream_none_reader("ON_STREAM_NONE_READER","流无人观看时事件"),
    on_stream_not_found("ON_STREAM_NOT_FOUND","流未找到事件"),
    on_server_started("ON_SERVER_STARTED","服务器启动事件"),
    on_rtp_server_timeout("ON_RTP_SERVER_TIMEOUT","调用openRtpServer 接口，rtp server 长时间未收到数据,执行此web hook,对回复不敏感"),
    on_server_keepalive("ON_SERVER_KEEPALIVE","服务器定时上报时间"),
    ;

    private final String code;
    private final String name;
    private static final Map<String, HookType> map = new HashMap<String, HookType>();
    static {
        for (HookType e : HookType.values()) {
            map.put(e.code, e);
        }
    }
    private HookType(String code, String name) {
        this.code = code;
        this.name = name;
    }
    public static HookType getHookType(String code) {
        return map.get(code);
    }
}
