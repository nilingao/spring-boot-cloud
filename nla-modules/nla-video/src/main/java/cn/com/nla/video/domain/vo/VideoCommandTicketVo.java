package cn.com.nla.video.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频指令受理凭据：指令已下发，最终回执经 SSE 推送。
 *
 * @author TZY
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoCommandTicketVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 回执关联号，SSE 推送结果中携带同一编号
     */
    private String requestId;

    /**
     * 业务动作标识，如 play、record-control
     */
    private String action;

    /**
     * 异步回执路由键
     */
    private String key;

    /**
     * 回执等待超时，单位毫秒
     */
    private Long timeout;
}
