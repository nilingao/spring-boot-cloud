package cn.com.nla.common.freeswitch.client.sip.listener.request;

import javax.sip.RequestEvent;

/**
 * 对SIP事件进行处理，包括request， response， timeout， ioException, transactionTerminated,dialogTerminated
 */
public interface SipRequestEvent {

    String getMethod();
	void process(RequestEvent event);

}
