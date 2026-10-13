package cn.com.nla.callcenter.socket.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 坐席外呼入站事件 {@code AGENT:IN:CALL_PHONE} 的消息体。
 *
 * @author TZY
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentInCallPhoneData extends Message {

    /**
     * 被叫号码
     */
    private String caller;

    /**
     * 呼叫类型，缺省按音频电话处理（见 VideoStreamType.CALL_AUDIO_PHONE）
     */
    private String type;
}
