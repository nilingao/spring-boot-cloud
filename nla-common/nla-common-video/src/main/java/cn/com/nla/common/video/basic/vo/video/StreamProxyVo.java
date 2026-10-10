package cn.com.nla.common.video.basic.vo.video;

import cn.com.nla.common.video.basic.model.LongIdEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
    * 拉流代理的信息
    */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class StreamProxyVo extends LongIdEntity {
    /**
     * 类型 0.其他 1.ffmpeg
     */
    private Integer type;

    /**
     * 应用名
     */
    private String app;

    /**
     * 流ID
     */
    private String stream;

    /**
     * 名称
     */
    private String name;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 流媒体服务ID
     */
    private String mediaServerId;

    /**
     * 拉流地址
     */
    private String url;

    /**
     * 拉流地址
     */
    private String srcUrl;

    /**
     * 目标地址
     */
    private String dstUrl;

    /**
     * 超时时间
     */
    private Integer timeoutMs;

    /**
     * ffmpeg模板KEY
     */
    private String ffmpegCmdKey;

    /**
     * rtsp拉流时，拉流方式，0：tcp，1：udp，2：组播
     */
    private Integer rtpType;

    /**
     * 是否启用
     */
    private Integer enable;

    /**
     * 是否启用音频
     */
    private Integer enableAudio;

    /**
     * 是否启用MP4
     */
    private Integer enableMp4;

    /**
     * 是否 无人观看时删除
     */
    private Integer enableRemoveNoneReader;

    /**
     * 是否 无人观看时自动停用
     */
    private Integer enableDisableNoneReader;
}