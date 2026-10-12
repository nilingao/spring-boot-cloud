package cn.com.nla.video.domain.vo;

import cn.com.nla.common.video.core.demo.StreamInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频流转码结果，转码进程由 FFmpeg 承载，停止转码需要用到返回的 key。
 *
 * @author TZY
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoConvertVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 转码进程句柄，结束转码时回传
     */
    private String key;

    /**
     * 媒体服务器归属，结束转码时用于定位节点
     */
    private String mediaServerId;

    /**
     * 转码后的各协议播放地址
     */
    private StreamInfo streamInfo;
}
