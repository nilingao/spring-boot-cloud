package cn.com.nla.video.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前用户的录像下载任务，数据来源于流媒体收流缓存。
 *
 * @author TZY
 */
@Data
public class VideoDownloadTaskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 下载任务缓存键，清除任务时回传
     */
    private String id;

    /**
     * 流应用名
     */
    private String app;

    /**
     * 流ID
     */
    private String stream;

    /**
     * 设备国标编号
     */
    private String deviceId;

    /**
     * 通道国标编号
     */
    private String channelId;

    /**
     * 流媒体服务ID
     */
    private String mediaServerId;

    /**
     * 下载起始时间
     */
    private String startTime;

    /**
     * 下载结束时间
     */
    private String endTime;

    /**
     * 下载进度，取值由流媒体上报
     */
    private Double progress;
}
