package cn.com.nla.common.video.core.demo;


import cn.com.nla.common.video.basic.vo.media.OnStreamChangedHookVo;
import cn.com.nla.common.video.basic.vo.sip.StreamURL;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.hutool.core.bean.BeanUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamInfo implements Serializable, Cloneable{

    /**
     * 应用名
     */
    private String app;
    /**
     * 流ID
     */
    private String stream;
    /**
     * 设备编号
     */
    private String deviceId;
    /**
     * 通道编号
     */
    private String channelId;
    /**
     * 请求前缀
     */
    private String prefix;
    /**
     * 是否ssl请求
     */
    private Integer sslStatus;
    /**
     * IP
     */
    private String ip;

    /**
     * HTTP-FLV流地址
     */
    private StreamURL flv;

    /**
     * HTTPS-FLV流地址
     */
    private StreamURL httpsFlv;
    /**
     * Websocket-FLV流地址
     */
    private StreamURL wsFlv;
    /**
     * Websockets-FLV流地址
     */
    private StreamURL wssFlv;
    /**
     * HTTP-FMP4流地址
     */
    private StreamURL fmp4;
    /**
     * HTTPS-FMP4流地址
     */
    private StreamURL httpsFmp4;
    /**
     * Websocket-FMP4流地址
     */
    private StreamURL wsFmp4;
    /**
     *Websockets-FMP4流地址
     */
    private StreamURL wssFmp4;
    /**
     * HLS流地址
     */
    private StreamURL hls;
    /**
     * HTTPS-HLS流地址
     */
    private StreamURL httpsHls;
    /**
     * Websocket-HLS流地址
     */
    private StreamURL wsHls;
    /**
     * Websockets-HLS流地址
     */
    private StreamURL wssHls;
    /**
     * HTTP-TS流地址
     */
    private StreamURL ts;
    /**
     * HTTPS-TS流地址
     */
    private StreamURL httpsTs;
    /**
     * Websocket-TS流地址
     */
    private StreamURL wsTs;
    /**
     * Websockets-TS流地址
     */
    private StreamURL wssTs;
    /**
     * RTMP流地址
     */
    private StreamURL rtmp;
    /**
     * RTMPS流地址
     */
    private StreamURL rtmps;
    /**
     * RTSP流地址
     */
    private StreamURL rtsp;
    /**
     * RTSPS流地址
     */
    private StreamURL rtsps;
    /**
     * RTC流地址
     */
    private StreamURL rtc;

    /**
     * RTCS流地址
     */
    private StreamURL rtcs;
    /**
     * 流媒体ID
     */
    private String mediaServerId;
    /**
     * 流编码信息
     */
    private Object tracks;
    /**
     * 开始时间
     */
    private String startTime;
    /**
     * 结束时间
     */
    private String endTime;
    /**
     * 进度（录像下载使用）
     */
    private double progress;

    /**
     * 是否暂停（录像回放使用）
     */
    private boolean pause;

    private SipTransactionInfo transactionInfo;


    public StreamInfo(MediaServerVo mediaServerVo, String app, String stream, List<OnStreamChangedHookVo.MediaTrack> tracks, String addr, String callId, String deviceId, String channelId){
        this.stream = stream;
        this.app = app;
        this.sslStatus = mediaServerVo.getSslStatus();
        this.ip = StringUtils.isEmpty(addr)? mediaServerVo.getStreamIp():addr;
        this.mediaServerId = mediaServerVo.getId();
        this.tracks = ObjectUtils.isEmpty(tracks)?tracks: tracks.stream().map(BeanUtil::beanToMap).collect(Collectors.toList());//类型转换
        this.deviceId = deviceId;
        this.channelId = channelId;
        this.prefix = mediaServerVo.getVideoPlayPrefix();
        String callIdParam = ObjectUtils.isEmpty(callId)?"":"?callId=" + callId;
        this.setRtmp(this.ip, mediaServerVo.getRtmpPort(), mediaServerVo.getRtmpSslPort(), app,  stream, callIdParam);
        this.setRtsp(this.ip, mediaServerVo.getRtspPort(), mediaServerVo.getRtspSslPort(), app,  stream, callIdParam);
        this.setFlv(this.ip, mediaServerVo.getHttpPort(), mediaServerVo.getHttpSslPort(), app,  stream, callIdParam);
        this.setFmp4(this.ip, mediaServerVo.getHttpPort(), mediaServerVo.getHttpSslPort(), app,  stream, callIdParam);
        this.setHls(this.ip, mediaServerVo.getHttpPort(), mediaServerVo.getHttpSslPort(), app,  stream, callIdParam);
        this.setTs(this.ip, mediaServerVo.getHttpPort(), mediaServerVo.getHttpSslPort(), app,  stream, callIdParam);
        this.setRtc(this.ip, mediaServerVo.getHttpPort(), mediaServerVo.getHttpSslPort(), app,  stream, callIdParam);
    }

    public void setRtmp(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s%s", app, stream, callIdParam);
        if(StringUtils.isNotEmpty(prefix)){
            file = String.format("%s/%s",prefix,file);
        }
        if (port > 0) {
            this.rtmp = new StreamURL("rtmp", host, port, file);
        }
        if (sslPort > 0) {
            this.rtmps = new StreamURL("rtmps", host, sslPort, file);
        }
    }

    public void setRtsp(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s%s", app, stream, callIdParam);
        if(StringUtils.isNotEmpty(prefix)){
            file = String.format("%s/%s",prefix,file);
        }
        if (port > 0) {
            this.rtsp = new StreamURL("rtsp", host, port, file);
        }
        if (sslPort > 0) {
            this.rtsps = new StreamURL("rtsps", host, sslPort, file);
        }
    }

    public void setFlv(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s.live.flv%s", app, stream, callIdParam);
        if(StringUtils.isNotEmpty(prefix)){
            file = String.format("%s/%s",prefix,file);
        }
        if (port > 0) {
            this.flv = new StreamURL("http", host, port, file);
        }
        this.wsFlv = new StreamURL("ws", host, port, file);
        if (sslPort > 0) {
            this.httpsFlv = new StreamURL("https", host, sslPort, file);
            this.wssFlv = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setFmp4(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s.live.mp4%s", app, stream, callIdParam);
        if(StringUtils.isNotEmpty(prefix)){
            file = String.format("%s/%s",prefix,file);
        }
        if (port > 0) {
            this.fmp4 = new StreamURL("http", host, port, file);
            this.wsFmp4 = new StreamURL("ws", host, port, file);
        }
        if (sslPort > 0) {
            this.httpsFmp4 = new StreamURL("https", host, sslPort, file);
            this.wssFmp4 = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setHls(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s/hls.m3u8%s", app, stream, callIdParam);
        if(StringUtils.isNotEmpty(prefix)){
            file = String.format("%s/%s",prefix,file);
        }
        if (port > 0) {
            this.hls = new StreamURL("http", host, port, file);
            this.wsHls = new StreamURL("ws", host, port, file);
        }
        if (sslPort > 0) {
            this.httpsHls = new StreamURL("https", host, sslPort, file);
            this.wssHls = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setTs(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        String file = String.format("%s/%s.live.ts%s", app, stream, callIdParam);
        if(StringUtils.isNotEmpty(prefix)){
            file = String.format("%s/%s",prefix,file);
        }
        if (port > 0) {
            this.ts = new StreamURL("http", host, port, file);
            this.wsTs = new StreamURL("ws", host, port, file);
        }
        if (sslPort > 0) {
            this.httpsTs = new StreamURL("https", host, sslPort, file);
            this.wssTs = new StreamURL("wss", host, sslPort, file);
        }
    }

    public void setRtc(String host, int port, int sslPort, String app, String stream, String callIdParam) {
        if (callIdParam != null) {
            callIdParam = Objects.equals(callIdParam, "") ? callIdParam : callIdParam.replace("?", "&");
        }
        String file = String.format("index/api/webrtc?app=%s&stream=%s&type=play%s", app, stream, callIdParam);
        if(StringUtils.isNotEmpty(prefix)){
            file = String.format("%s/%s",prefix,file);
        }
        if (port > 0) {
            this.rtc = new StreamURL("http", host, port, file);
        }
        if (sslPort > 0) {
            this.rtcs = new StreamURL("https", host, sslPort, file);
        }
    }

    public void channgeStreamIp(String localAddr) {
        if (this.flv != null) {
            this.flv.setHost(localAddr);
        }
        if (this.wsFlv != null ){
            this.wsFlv.setHost(localAddr);
        }
        if (this.hls != null ) {
            this.hls.setHost(localAddr);
        }
        if (this.wsHls != null ) {
            this.wsHls.setHost(localAddr);
        }
        if (this.ts != null ) {
            this.ts.setHost(localAddr);
        }
        if (this.wsTs != null ) {
            this.wsTs.setHost(localAddr);
        }
        if (this.fmp4 != null ) {
            this.fmp4.setHost(localAddr);
        }
        if (this.wsFmp4 != null ) {
            this.wsFmp4.setHost(localAddr);
        }
        if (this.rtc != null ) {
            this.rtc.setHost(localAddr);
        }
        if (this.httpsFlv != null) {
            this.httpsFlv.setHost(localAddr);
        }
        if (this.wssFlv != null) {
            this.wssFlv.setHost(localAddr);
        }
        if (this.httpsHls != null) {
            this.httpsHls.setHost(localAddr);
        }
        if (this.wssHls != null) {
            this.wssHls.setHost(localAddr);
        }
        if (this.wssTs != null) {
            this.wssTs.setHost(localAddr);
        }
        if (this.httpsFmp4 != null) {
            this.httpsFmp4.setHost(localAddr);
        }
        if (this.wssFmp4 != null) {
            this.wssFmp4.setHost(localAddr);
        }
        if (this.rtcs != null) {
            this.rtcs.setHost(localAddr);
        }
        if (this.rtsp != null) {
            this.rtsp.setHost(localAddr);
        }
        if (this.rtsps != null) {
            this.rtsps.setHost(localAddr);
        }
        if (this.rtmp != null) {
            this.rtmp.setHost(localAddr);
        }
        if (this.rtmps != null) {
            this.rtmps.setHost(localAddr);
        }
    }

    @Override
    public StreamInfo clone() {
        StreamInfo instance = null;
        try{
            instance = (StreamInfo)super.clone();
            if (this.flv != null) {
                instance.flv=this.flv.clone();
            }
            if (this.wsFlv != null ){
                instance.wsFlv= this.wsFlv.clone();
            }
            if (this.hls != null ) {
                instance.hls= this.hls.clone();
            }
            if (this.wsHls != null ) {
                instance.wsHls= this.wsHls.clone();
            }
            if (this.ts != null ) {
                instance.ts= this.ts.clone();
            }
            if (this.wsTs != null ) {
                instance.wsTs= this.wsTs.clone();
            }
            if (this.fmp4 != null ) {
                instance.fmp4= this.fmp4.clone();
            }
            if (this.wsFmp4 != null ) {
                instance.wsFmp4= this.wsFmp4.clone();
            }
            if (this.rtc != null ) {
                instance.rtc= this.rtc.clone();
            }
            if (this.httpsFlv != null) {
                instance.httpsFlv= this.httpsFlv.clone();
            }
            if (this.wssFlv != null) {
                instance.wssFlv= this.wssFlv.clone();
            }
            if (this.httpsHls != null) {
                instance.httpsHls= this.httpsHls.clone();
            }
            if (this.wssHls != null) {
                instance.wssHls= this.wssHls.clone();
            }
            if (this.wssTs != null) {
                instance.wssTs= this.wssTs.clone();
            }
            if (this.httpsFmp4 != null) {
                instance.httpsFmp4= this.httpsFmp4.clone();
            }
            if (this.wssFmp4 != null) {
                instance.wssFmp4= this.wssFmp4.clone();
            }
            if (this.rtcs != null) {
                instance.rtcs= this.rtcs.clone();
            }
            if (this.rtsp != null) {
                instance.rtsp= this.rtsp.clone();
            }
            if (this.rtsps != null) {
                instance.rtsps= this.rtsps.clone();
            }
            if (this.rtmp != null) {
                instance.rtmp= this.rtmp.clone();
            }
            if (this.rtmps != null) {
                instance.rtmps= this.rtmps.clone();
            }
        }catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return instance;
    }
}
