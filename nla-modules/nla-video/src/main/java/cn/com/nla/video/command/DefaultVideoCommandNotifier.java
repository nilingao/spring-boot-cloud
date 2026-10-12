package cn.com.nla.video.command;

import cn.com.nla.common.core.enums.PushSourceEnum;
import cn.com.nla.common.push.helper.PushHelper;
import cn.com.nla.system.api.domain.PushPayloadDTO;
import cn.com.nla.video.domain.vo.VideoCommandResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 默认回执通知器：经统一消息推送通道（SSE）投递给指令发起用户。
 *
 * <p>使用 Redis 主题发布，集群下由持有该用户连接的节点送出；推送失败只记录日志，
 * 不影响指令本身的下发与执行。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，与管理 CRUD 端点解耦。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class DefaultVideoCommandNotifier implements VideoCommandNotifier {

    /**
     * 视频指令回执的消息类型标识，前端按该类型区分业务推送。
     */
    public static final String PUSH_TYPE = "video-command";

    /**
     * 推送指令回执。
     *
     * @param userId 指令发起用户，为空表示协议侧/匿名触发，不投递
     * @param result 回执内容
     */
    @Override
    public void push(Long userId, VideoCommandResultVo result) {
        if (userId == null || result == null) {
            return;
        }
        try {
            PushPayloadDTO payload = PushPayloadDTO.of(PUSH_TYPE, PushSourceEnum.BACKEND.getSource(),
                result.getMessage(), result);
            PushHelper.publishMessage(List.of(userId), payload);
        } catch (Exception e) {
            log.warn("[视频指令回执] 推送失败，requestId：{}，原因：{}", result.getRequestId(), e.getMessage());
        }
    }
}
