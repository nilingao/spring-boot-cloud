package cn.com.nla.callcenter.socket.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 坐席来电操作入站事件 {@code AGENT:IN:PHONE_NOTIFICATION} 的消息体。
 *
 * @author TZY
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentInPhoneNotificationData extends Message {

    /**
     * 用户来电操作：1 挂断 2 接听
     */
    private Integer type;

    /**
     * 来电通话编号
     */
    private String callId;

    /**
     * 接听方式：是否视频接听（1 是 0 否）
     */
    private Integer onVideo;
}
