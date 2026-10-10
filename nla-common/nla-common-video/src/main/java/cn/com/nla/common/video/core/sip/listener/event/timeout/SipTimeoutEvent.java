package cn.com.nla.common.video.core.sip.listener.event.timeout;

import javax.sip.TimeoutEvent;

/**
 * 相应超时处理
 */
public interface SipTimeoutEvent {

	void process(TimeoutEvent event);

}
