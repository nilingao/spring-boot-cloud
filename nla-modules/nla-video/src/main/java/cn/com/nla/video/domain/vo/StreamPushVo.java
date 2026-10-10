package cn.com.nla.video.domain.vo;

import cn.com.nla.video.domain.StreamPush;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 推流信息视图，不包含凭据、原始流地址或删除标志。
 * @author TZY
 */
@Data
@AutoMapper(target = StreamPush.class)
public class StreamPushVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String app;
    private String stream;
    private String totalReaderCount;
    private Integer originType;
    private String originTypeStr;
    private Integer aliveSecond;
    private String mediaServerId;
    private String serverId;
    private LocalDateTime pushTime;
    private Integer status;
    private Integer pushIng;
    private Integer onSelf;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
