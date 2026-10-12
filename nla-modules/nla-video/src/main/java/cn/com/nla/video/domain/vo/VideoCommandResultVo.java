package cn.com.nla.video.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频指令异步回执，经 SSE 推送给指令发起用户。
 *
 * @author TZY
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoCommandResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 与受理凭据一致的回执关联号
     */
    private String requestId;

    /**
     * 业务动作标识
     */
    private String action;

    /**
     * 异步回执路由键
     */
    private String key;

    /**
     * 是否执行成功
     */
    private Boolean success;

    /**
     * 协议返回码，0 表示成功，其余沿用 GB28181/ZLM 协议语义
     */
    private Integer code;

    /**
     * 结果描述
     */
    private String message;

    /**
     * 业务数据，如流地址、录像列表、设备状态
     */
    private Object data;
}
