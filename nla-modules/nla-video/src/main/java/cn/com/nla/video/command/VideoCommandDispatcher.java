package cn.com.nla.video.command;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.core.demo.VideoRestResult;
import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;
import cn.com.nla.video.domain.vo.VideoCommandResultVo;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 视频异步指令调度器：HTTP 线程只做「受理」，协议回执经 SSE 推送给发起用户。
 *
 * <p>旧工程同类端点直接返回 {@code DeferredResult}，长连接占用 Web 线程且无法跨节点回收；
 * 迁移后统一改为受理凭据 + 异步推送：注册回执路由 → 下发 SIP 命令 → 立即返回 {@code requestId}，
 * 协议侧按路由键释放结果后由 {@link VideoCommandNotifier} 推送。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，与管理 CRUD 端点解耦。</p>
 *
 * @author TZY
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class VideoCommandDispatcher {

    /**
     * 默认回执超时，与旧工程指令下发超时保持一致
     */
    public static final long DEFAULT_TIMEOUT_MILLIS = 30_000L;

    /**
     * 默认超时描述
     */
    private static final String DEFAULT_TIMEOUT_MESSAGE = "指令执行超时";

    /**
     * 重复指令拒绝描述
     */
    private static final String REPEAT_MESSAGE = "相同指令正在执行中，请等待回执";

    private final DeferredResultHolder deferredResultHolder;
    private final VideoCommandNotifier videoCommandNotifier;

    /**
     * 受理一条视频指令，使用默认超时。
     *
     * @param action 业务动作标识，随回执原样返回
     * @param key    异步回执路由键，见 {@link VideoCommandKeys}
     * @param sender 命令下发逻辑，收到句柄后自行调用 SIP 协议层
     * @return 受理凭据
     * @throws ServiceException 路由键上已有在途指令时抛出
     */
    public VideoCommandTicketVo dispatch(String action, String key, Consumer<VideoCommandHandle> sender) {
        return dispatch(action, key, DEFAULT_TIMEOUT_MILLIS, DEFAULT_TIMEOUT_MESSAGE, sender);
    }

    /**
     * 受理一条视频指令，自定义超时与超时描述。
     *
     * @param action         业务动作标识
     * @param key            异步回执路由键
     * @param timeoutMillis  回执超时毫秒
     * @param timeoutMessage 超时描述
     * @param sender         命令下发逻辑
     * @return 受理凭据
     * @throws ServiceException 路由键上已有在途指令时抛出
     */
    public VideoCommandTicketVo dispatch(String action, String key, long timeoutMillis, String timeoutMessage,
                                         Consumer<VideoCommandHandle> sender) {
        return dispatch(action, key, timeoutMillis,
            () -> ProtocolResult.result(RespCode.CODE_2.getValue(), timeoutMessage), sender);
    }

    /**
     * 受理一条视频指令，自定义超时结果。
     *
     * <p>超时结果以 {@link Supplier} 形式提供，用于超时后还需要回收资源的指令，
     * 例如语音广播超时后要关闭已分配的语音推流事务。</p>
     *
     * @param action        业务动作标识
     * @param key           异步回执路由键
     * @param timeoutMillis 回执超时毫秒
     * @param timeoutResult 超时结果构造逻辑
     * @param sender        命令下发逻辑
     * @return 受理凭据
     * @throws ServiceException 路由键上已有在途指令时抛出
     */
    public VideoCommandTicketVo dispatch(String action, String key, long timeoutMillis, Supplier<Object> timeoutResult,
                                         Consumer<VideoCommandHandle> sender) {
        if (deferredResultHolder.exist(key, null)) {
            throw new ServiceException(REPEAT_MESSAGE);
        }
        String requestId = RandomUtil.randomString(32);
        Long userId = currentUserId();
        VideoRestResult<Object> result = new VideoRestResult<>(timeoutMillis, timeoutResult);
        deferredResultHolder.put(key, requestId, result);
        result.whenComplete((payload, error) ->
            videoCommandNotifier.push(userId, normalize(action, key, requestId, payload, error)));
        VideoCommandHandle handle = new VideoCommandHandle(deferredResultHolder, key, requestId, result);
        try {
            sender.accept(handle);
        } catch (Exception e) {
            log.error("[视频指令下发失败] action：{}，key：{}", action, key, e);
            handle.fail("命令发送失败");
        }
        return new VideoCommandTicketVo(requestId, action, key, timeoutMillis);
    }

    /**
     * 当前登录用户，协议侧回调等无登录上下文场景返回 {@code null}，此时不推送回执。
     *
     * @return 用户主键
     */
    protected Long currentUserId() {
        return LoginHelper.getUserId();
    }

    /**
     * 归一化协议载荷：兼容 {@link ProtocolResult}、{@code {code,msg,data}} 结构的 Map 以及裸对象。
     *
     * @param action    业务动作标识
     * @param key       路由键
     * @param requestId 受理凭据
     * @param payload   协议载荷
     * @param error     异步执行异常
     * @return 回执内容
     */
    private VideoCommandResultVo normalize(String action, String key, String requestId, Object payload, Throwable error) {
        if (error != null) {
            return new VideoCommandResultVo(requestId, action, key, false, RespCode.CODE_2.getValue(),
                error.getMessage(), null);
        }
        if (payload instanceof ProtocolResult<?> protocolResult) {
            return build(requestId, action, key, protocolResult.getCode(), protocolResult.getMessage(),
                protocolResult.getData());
        }
        if (payload instanceof Map<?, ?> map) {
            Integer code = readInt(map.get("code"));
            Object message = map.containsKey("msg") ? map.get("msg") : map.get("message");
            return build(requestId, action, key, code == null ? RespCode.CODE_0.getValue() : code,
                message == null ? null : message.toString(), map.get("data"));
        }
        return build(requestId, action, key, RespCode.CODE_0.getValue(), null, payload);
    }

    /**
     * 组装回执，成功码统一补齐描述文案。
     *
     * @param requestId 受理凭据
     * @param action    业务动作标识
     * @param key       路由键
     * @param code      协议返回码
     * @param message   结果描述
     * @param data      业务数据
     * @return 回执内容
     */
    private VideoCommandResultVo build(String requestId, String action, String key, int code, String message, Object data) {
        boolean success = code == RespCode.CODE_0.getValue();
        String text = message == null || message.isBlank()
            ? (success ? "成功" : "指令执行失败")
            : message;
        return new VideoCommandResultVo(requestId, action, key, success, code, text, data);
    }

    /**
     * 宽松读取协议返回码。
     *
     * @param value 原始值
     * @return 返回码，无法解析时返回 {@code null}
     */
    private Integer readInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && text.matches("-?\\d+")) {
            return Integer.parseInt(text);
        }
        return null;
    }
}
