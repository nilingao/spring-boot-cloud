package cn.com.nla.video.command;

import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.core.demo.VideoRestResult;
import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;
import lombok.Getter;

/**
 * 一次视频指令的异步回执句柄，业务侧只通过它回填结果，无需感知路由键与请求号配对。
 *
 * <p>协议侧应答由 {@code nla-common-video} 内的应答处理器按路由键整体释放，本句柄只用于
 * 命令发送失败、设备不存在等本地可判定的失败场景。</p>
 *
 * @author TZY
 */
@Getter
public class VideoCommandHandle {

    /**
     * 默认成功描述，协议侧成功应答常不带文案
     */
    private static final String DEFAULT_SUCCESS_MESSAGE = "成功";

    private final DeferredResultHolder deferredResultHolder;
    private final String key;
    private final String requestId;
    private final VideoRestResult<Object> result;

    VideoCommandHandle(DeferredResultHolder deferredResultHolder, String key, String requestId,
                       VideoRestResult<Object> result) {
        this.deferredResultHolder = deferredResultHolder;
        this.key = key;
        this.requestId = requestId;
        this.result = result;
    }

    /**
     * 回填成功结果。
     *
     * @param data 业务数据
     */
    public void success(Object data) {
        complete(ProtocolResult.result(RespCode.CODE_0.getValue(), DEFAULT_SUCCESS_MESSAGE, data));
    }

    /**
     * 回填业务失败结果，使用协议默认失败码。
     *
     * @param message 失败描述
     */
    public void fail(String message) {
        fail(RespCode.CODE_2.getValue(), message);
    }

    /**
     * 回填失败结果。
     *
     * @param code    协议返回码
     * @param message 失败描述
     */
    public void fail(int code, String message) {
        complete(ProtocolResult.result(code, message));
    }

    /**
     * 回填原始协议载荷，用于需要保留 ZLM/GB28181 原始响应结构的场景。
     *
     * @param payload 协议载荷
     */
    public void complete(Object payload) {
        deferredResultHolder.invokeResult(key, requestId, payload);
    }
}
