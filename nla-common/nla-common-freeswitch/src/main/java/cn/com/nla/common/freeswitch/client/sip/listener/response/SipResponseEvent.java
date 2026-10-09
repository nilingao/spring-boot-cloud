package cn.com.nla.common.freeswitch.client.sip.listener.response;

import javax.sip.ResponseEvent;

/**
 * 处理接收IPCamera发来的SIP协议响应消息
 */
public interface SipResponseEvent {

	String getMethod();

	void process(ResponseEvent event);

}
