package cn.com.nla.common.video.core.sip.listener.event.timeout.impl;

import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.core.model.EventResult;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeHandle;
import cn.com.nla.common.video.core.sip.listener.event.timeout.SipTimeoutEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.SerializationUtils;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.Dialog;
import javax.sip.TimeoutEvent;


/**
 * 相应超时处理
 */
@Slf4j
public class SipTimeoutEventImpl implements SipTimeoutEvent {

	@Autowired
	private SipSubscribeHandle sipSubscribeHandle;

	@Override
	public void process(TimeoutEvent event) {
		try {
			Dialog dialog = event.getClientTransaction().getDialog();
			if(dialog != null){
				String callId =dialog.getCallId().getCallId();
				String key = String.format("%s:%s", SipSubscribeHandle.VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER, callId);
				RedisUtils.publish(key, SerializationUtils.serialize(new EventResult(event)));
			}else {
				log.warn("[超时事件] ：dialog is null");
			}
			//清除订阅事件
		} catch (Exception e) {
			log.error("[超时事件失败]: ", e);
		}
	}
}
