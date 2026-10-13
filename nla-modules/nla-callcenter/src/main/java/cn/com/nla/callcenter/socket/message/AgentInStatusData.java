package cn.com.nla.callcenter.socket.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 坐席状态变更入站事件 {@code AGENT:IN:STATUS} 的消息体。
 *
 * @author TZY
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentInStatusData extends Message {

    /**
     * 是否更新状态：空或 0 时仅查询当前坐席信息，1 时切换繁忙/空闲
     */
    private Integer updateStatus;
}
