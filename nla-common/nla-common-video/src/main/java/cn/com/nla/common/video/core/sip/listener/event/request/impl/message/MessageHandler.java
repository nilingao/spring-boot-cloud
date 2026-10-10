package cn.com.nla.common.video.core.sip.listener.event.request.impl.message;

import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import org.w3c.dom.Element;

import javax.sip.RequestEvent;

/**
 * 不同消息类型接口
 */
public interface MessageHandler {
    /**
     * 处理来自设备的信息
     * @param evt
     * @param deviceVo
     */
    void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element);

    /**
     * 处理来自平台的信息
     * @param evt
     * @param parentPlatformVo
     */
    void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element);
}
