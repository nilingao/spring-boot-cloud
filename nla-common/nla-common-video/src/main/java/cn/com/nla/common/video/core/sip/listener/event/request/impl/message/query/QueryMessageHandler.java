package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.query;

import cn.com.nla.common.video.basic.enums.MessageType;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandlerAbstract;
import lombok.extern.slf4j.Slf4j;

/**
 * 命令类型： 查询指令
 * 命令类型： 设备状态, 设备目录信息, 设备信息, 文件目录检索(TODO), 报警(TODO), 设备配置(TODO), 设备预置位(TODO), 移动设备位置数据(TODO)
 */
@Slf4j
public class QueryMessageHandler extends MessageHandlerAbstract {
    @Override
    public String getMessageType() {
        return MessageType.QUERY.getValue();
    }
}
