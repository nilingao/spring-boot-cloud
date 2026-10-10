package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.notify;

import cn.com.nla.common.video.basic.enums.MessageType;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandlerAbstract;
import lombok.extern.slf4j.Slf4j;

/**
 * 命令类型： 通知命令， 参看 A.2.5 通知命令
 * 命令类型： 状态信息(心跳)报送, 报警通知, 媒体通知, 移动设备位置数据，语音广播通知(TODO), 设备预置位(TODO)
 * @author TZY
 */
@Slf4j
public class NotifyMessageHandler extends MessageHandlerAbstract {
    @Override
    public String getMessageType() {
        return MessageType.NOTIFY.getValue();
    }
}
