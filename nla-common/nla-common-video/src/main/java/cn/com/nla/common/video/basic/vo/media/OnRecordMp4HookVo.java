package cn.com.nla.common.video.basic.vo.media;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OnRecordMp4HookVo extends HookVo{
    /**
     * 应用名
     */
    private String app;
    /**
     * 流ID
     */
    private String stream;
    /**
     * 文件名
     */
    private String file_name;
    /**
     * 文件绝对路径
     */
    private String file_path;
    /**
     * 文件大小，单位字节
     */
    private Integer file_size;
    /**
     * 文件所在目录路径
     */
    private String folder;
    /**
     * 开始录制时间戳(秒)
     */
    private Integer start_time;
    /**
     * 录制时长，单位秒
     */
    private BigDecimal time_len;
    /**
     * http/rtsp/rtmp点播相对url路径
     */
    private String url;
}
