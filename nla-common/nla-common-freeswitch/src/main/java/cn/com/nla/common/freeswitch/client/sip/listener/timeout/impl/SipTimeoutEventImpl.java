package cn.com.nla.common.freeswitch.client.sip.listener.timeout.impl;

import cn.com.nla.common.freeswitch.vo.sip.EventResult;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.SipSubscribeHandle;
import cn.com.nla.common.freeswitch.client.sip.listener.timeout.SipTimeoutEvent;
import cn.com.nla.common.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import jakarta.annotation.Resource;
import javax.sip.Dialog;
import javax.sip.TimeoutEvent;


/**
 * 相应超时处理
 */
@Slf4j
@Component
public class SipTimeoutEventImpl implements SipTimeoutEvent {

	@Resource
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
