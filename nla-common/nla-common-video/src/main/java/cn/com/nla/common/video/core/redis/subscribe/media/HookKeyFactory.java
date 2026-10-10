package cn.com.nla.common.video.core.redis.subscribe.media;

import cn.com.nla.common.video.basic.enums.HookType;
import cn.com.nla.common.video.basic.vo.media.HookKey;
import cn.com.nla.common.video.core.demo.MediaHookVo;
import cn.hutool.core.bean.BeanUtil;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

public class HookKeyFactory {
    public static HookKey onStreamChanged(String app, String stream, boolean regist, String scheam, String mediaServerId){
        StreamChangedContent build = StreamChangedContent.builder()
                .app(app)
                .stream(stream)
                .regist(regist)
                .schema(scheam)
                .mediaServerId(mediaServerId)
                .build();
        return new HookKey(HookType.on_stream_changed,BeanUtil.beanToMap(build));
    }
    public static HookKey onRtpServerTimeout(String streamId,String mediaServerId){
        RtpServerTimeoutContent build = RtpServerTimeoutContent.builder()
                .streamId(streamId)
                .mediaServerId(mediaServerId)
                .build();
        return new HookKey(HookType.on_rtp_server_timeout,BeanUtil.beanToMap(build));
    }
    public static HookKey onServerStarted() {
        ServerStartedContent build = new ServerStartedContent();
        return new HookKey(HookType.on_server_started,BeanUtil.beanToMap(build));
    }
    public static HookKey onServerKeepalive(String mediaServerId) {
        ServerKeepaliveContent build = ServerKeepaliveContent.builder().mediaServerId(mediaServerId).build();
        return new HookKey(HookType.on_server_keepalive,BeanUtil.beanToMap(build));
    }

    public static HookKey onRecordMp4(String mediaServerId) {
        RecordMp4Content build = RecordMp4Content.builder().mediaServerId(mediaServerId).build();
        return new HookKey(HookType.on_record_mp4,BeanUtil.beanToMap(build));
    }

    public static Map<String,Object> buildContent(MediaHookVo vo){
        Map<String, Object> hookResponse = BeanUtil.beanToMap(vo.getHookVo());
        Content content = null;
        if(vo.getType() == HookType.on_stream_changed){
            content = BeanUtil.toBean(hookResponse, StreamChangedContent.class);
        }else if(vo.getType() == HookType.on_rtp_server_timeout){
            content = BeanUtil.toBean(hookResponse, RtpServerTimeoutContent.class);
        }else if(vo.getType() == HookType.on_server_started){
            content = BeanUtil.toBean(hookResponse, ServerStartedContent.class);
        }else if(vo.getType() == HookType.on_server_keepalive){
            content = BeanUtil.toBean(hookResponse, ServerKeepaliveContent.class);
        }else if(vo.getType() == HookType.on_record_mp4){
            content = BeanUtil.toBean(hookResponse, RecordMp4Content.class);
        }
        if( content == null ){
            return null;
        }
        return BeanUtil.beanToMap(content);
    }

    @Data
    @Builder
    public static class StreamChangedContent extends Content{
        private String app;
        private String stream;
        private Boolean regist;
        private String schema;
        private String mediaServerId;
    }
    @Data
    @Builder
    public static class RtpServerTimeoutContent extends Content{
        private String streamId;
        private String mediaServerId;
    }
    @Data
    @Builder
    public static class ServerStartedContent extends Content{
    }
    @Data
    @Builder
    public static class ServerKeepaliveContent extends Content{
        private String mediaServerId;
    }

    @Data
    @Builder
    public static class RecordMp4Content extends Content{
        private String mediaServerId;
    }

    public static class Content{

    }

}
