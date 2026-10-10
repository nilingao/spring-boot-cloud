package cn.com.nla.video.domain.vo;

import cn.com.nla.video.domain.StreamProxy;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 拉流代理的信息视图，不包含凭据、原始流地址或删除标志。
 * @author TZY
 */
@Data
@AutoMapper(target = StreamProxy.class)
public class StreamProxyVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer type;
    private String app;
    private String stream;
    private String name;
    private Integer status;
    private String mediaServerId;
    private Integer timeoutMs;
    private String ffmpegCmdKey;
    private Integer rtpType;
    private Integer enable;
    private Integer enableAudio;
    private Integer enableMp4;
    private Integer enableRemoveNoneReader;
    private Integer enableDisableNoneReader;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
