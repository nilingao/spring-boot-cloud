package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response;

import cn.com.nla.common.video.basic.enums.MessageType;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandlerAbstract;
import lombok.extern.slf4j.Slf4j;

/**
 * 命令类型： 请求动作的应答
 * 命令类型： 设备控制, 报警通知, 设备目录信息查询, 目录信息查询, 目录收到, 设备信息查询, 设备状态信息查询 ......
 */
@Slf4j
public class ResponseMessageHandler extends MessageHandlerAbstract {

    @Override
    public String getMessageType() {
        return MessageType.RESPONSE.getValue();
    }
}
