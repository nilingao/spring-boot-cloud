package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.control;

import cn.com.nla.common.video.basic.enums.MessageType;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandlerAbstract;

/**
 * 命令类型： 控制命令
 * 命令类型： 设备控制： 远程启动, 录像控制（TODO）, 报警布防/撤防命令（TODO）, 报警复位命令（TODO）,
 *                   强制关键帧命令（TODO）, 拉框放大/缩小控制命令（TODO）, 看守位控制（TODO）, 报警复位（TODO）
 * 命令类型： 设备配置： SVAC编码配置（TODO）, 音频参数（TODO）, SVAC解码配置（TODO）
 */
public class ControlMessageHandler extends MessageHandlerAbstract {

    @Override
    public String getMessageType() {
        return MessageType.CONTROL.getValue();
    }

}
