package cn.com.nla.callcenter.socket.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 坐席挂断入站事件 {@code AGENT:IN:HANG_UP_PHONE} 的消息体。
 *
 * <p>agentKey 字段沿用旧协议保留，当前挂断按会话对应的坐席处理，暂不使用。</p>
 *
 * @author TZY
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentInHangUpPhoneData extends Message {

    /**
     * 坐席账户（暂时无用）
     */
    private String agentKey;
}
