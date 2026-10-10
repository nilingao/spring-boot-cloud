package cn.com.nla.common.video.basic.vo.media;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OnStreamChangedHookVo extends HookVo{

    /**
     * 注册/注销
     */
    private boolean regist;

    /**
     * 应用名
     */
    private String app;

    /**
     * 流id
     */
    private String stream;



    /**
     * 观看总人数，包括hls/rtsp/rtmp/http-flv/ws-flv
     */
    private Integer totalReaderCount;

    /**
     * 协议 包括hls/rtsp/rtmp/http-flv/ws-flv
     */
    private String schema;


    /**
     * 产生源类型，
     * unknown = 0,
     * rtmp_push=1,
     * rtsp_push=2,
     * rtp_push=3,
     * pull=4,
     * ffmpeg_pull=5,
     * mp4_vod=6,
     * device_chn=7
     */
    private int originType;

    /**
     * 客户端和服务器网络信息，可能为null类型
     */
    private OriginSock originSock;

    /**
     * 产生源类型的字符串描述
     */
    private String originTypeStr;

    /**
     * 产生源的url
     */
    private String originUrl;

    /**
     * 服务器id
     */
    private String mediaServerId;

    /**
     * GMT unix系统时间戳，单位秒
     */
    private Long createStamp;

    /**
     * 存活时间，单位秒
     */
    private Long aliveSecond;

    /**
     * 数据产生速度，单位byte/s
     */
    private Long bytesSpeed;

    /**
     * 音视频轨道
     */
    private List<MediaTrack> tracks;

    /**
     * 音视频轨道
     */
    private String vhost;

    /**
     * 是否是docker部署， docker部署不会自动更新zlm使用的端口，需要自己手动修改
     */
    private boolean docker;

    @Data
    public static class MediaTrack implements Serializable {
        /**
         * 音频通道数
         */
        private int channels;

        /**
         * H264 = 0, H265 = 1, AAC = 2, G711A = 3, G711U = 4
         */
        @JsonProperty(value = "codec_id")
        private int codecId;

        /**
         * 编码类型名称 CodecAAC CodecH264
         */
        @JsonProperty(value = "codec_id_name")
        private String codecIdName;

        /**
         * Video = 0, Audio = 1
         */
        @JsonProperty(value = "codec_type")
        private int codecType;

        /**
         * 轨道是否准备就绪
         */
        private Boolean ready;

        /**
         * 音频采样位数
         */
        @JsonProperty(value = "sample_bit")
        private int sampleBit;

        /**
         * 音频采样率
         */
        @JsonProperty(value = "sample_rate")
        private int sampleRate;

        /**
         * 视频fps
         */
        private int fps;

        /**
         * 视频高
         */
        private int height;

        /**
         * 视频宽
         */
        private int width;
        /**
         * 丢包率
         */
        private int loss;
    }

    @Data
    public static class OriginSock implements Serializable {
        private String identifier;
        @JsonProperty(value = "local_ip")
        private String localIp;
        @JsonProperty(value = "local_port")
        private int localPort;
        @JsonProperty(value = "peer_ip")
        private String peerIp;
        @JsonProperty(value = "peer_port")
        private int peerPort;
    }

}
