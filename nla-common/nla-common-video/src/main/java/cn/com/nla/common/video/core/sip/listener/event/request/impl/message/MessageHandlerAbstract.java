package cn.com.nla.common.video.core.sip.listener.event.request.impl.message;

import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.utils.XmlUtils;
import org.w3c.dom.Element;

import javax.sip.RequestEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MessageHandlerAbstract implements MessageHandler{

    private Map<String, MessageHandler> messageHandlerMap = new ConcurrentHashMap<>();

    public abstract String getMessageType();

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        String cmd = XmlUtils.getText(element, "CmdType");
        MessageHandler messageHandler = messageHandlerMap.get(cmd);
        if (messageHandler != null) {
            messageHandler.handForDevice(evt, deviceVo, element);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {
        String cmd = XmlUtils.getText(element, "CmdType");
        MessageHandler messageHandler = messageHandlerMap.get(cmd);
        if (messageHandler != null) {
            messageHandler.handForPlatform(evt, parentPlatformVo, element);
        }
    }

    public void setMessageHandler(String key,MessageHandler handler){
        messageHandlerMap.put(key,handler);
    }
    public MessageHandler getMessageHandler(String key){
       return messageHandlerMap.get(key);
    }
    public Map<String, MessageHandler> getMessageHandlerMap(){
        return messageHandlerMap;
    }

}
