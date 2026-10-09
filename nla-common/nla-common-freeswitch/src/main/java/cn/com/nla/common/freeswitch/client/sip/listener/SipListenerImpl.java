package cn.com.nla.common.freeswitch.client.sip.listener;

import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.listener.request.SipRequestEvent;
import cn.com.nla.common.freeswitch.client.sip.listener.response.SipResponseEvent;
import cn.com.nla.common.freeswitch.client.sip.listener.timeout.SipTimeoutEvent;
import cn.com.nla.common.freeswitch.client.sip.utils.SipLogUtils;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.SipSubscribeHandle;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import javax.sip.*;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
public class SipListenerImpl implements SipListener {

    private Map<String, SipRequestEvent> sipRequestEventMap ;
    private Map<String, SipResponseEvent> sipResponseEventMap;
    /**
     * SIP超时事件
     */
    private SipTimeoutEvent sipTimeoutEvent;

    private final SipServer sipServer;

    public SipListenerImpl(SipServer sipServer){
        this.sipServer = sipServer;
    }



    public void init(SipTimeoutEvent sipTimeoutEvent, ConcurrentHashMap<String, SipRequestEvent> sipRequestEventMap, ConcurrentHashMap<String, SipResponseEvent> sipResponseEventMap){
        this.sipTimeoutEvent = sipTimeoutEvent;
        this.sipRequestEventMap = sipRequestEventMap;
        this.sipResponseEventMap = sipResponseEventMap;
    }

    /**
     * 分发RequestEvent事件
     * 除去注册消息 其他则分发给mq
     */
    @Override
    public void processRequest(RequestEvent requestEvent) {
        ThreadUtil.execute(()->{
            try {
                SipLogUtils.receiveMessage(sipServer,requestEvent);
                String method = requestEvent.getRequest().getMethod();
                SipRequestEvent sipRequestProcessor = sipRequestEventMap.get(method);
                if (sipRequestProcessor == null) {
                    log.warn("不支持方法{}的request", method);
                    // TODO 回复错误玛
                    return;
                }
                sipRequestEventMap.get(method).process(requestEvent);
            }catch (Exception e){
                log.error("RequestEvent事件消息处理异常",e);
            }
        });
    }

    /**
     * 分发ResponseEvent事件
     *  除去注册消息 其他则分发给mq
     */
    @Override
    public void processResponse(ResponseEvent responseEvent) {
        ThreadUtil.execute(()->{
            try {
                SipLogUtils.receiveMessage(sipServer,responseEvent);
                Response response = responseEvent.getResponse();
                int status = response.getStatusCode();
                // Success
                if (((status >= Response.OK) && (status < Response.MULTIPLE_CHOICES)) || status == Response.UNAUTHORIZED || status == Response.PROXY_AUTHENTICATION_REQUIRED) {
                    CSeqHeader cseqHeader = (CSeqHeader) responseEvent.getResponse().getHeader(CSeqHeader.NAME);
                    String method = cseqHeader.getMethod();
                    SipResponseEvent sipRequestProcessor = sipResponseEventMap.get(method);
                    if (sipRequestProcessor != null) {
                        sipRequestProcessor.process(responseEvent);
                    }
                    if (status != Response.UNAUTHORIZED && status != Response.PROXY_AUTHENTICATION_REQUIRED &&  responseEvent.getResponse() != null) {
                        CallIdHeader callIdHeader = (CallIdHeader)responseEvent.getResponse().getHeader(CallIdHeader.NAME);
                        if (callIdHeader != null) {
                            String key = String.format("%s%s", SipSubscribeHandle.VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER, callIdHeader.getCallId());
                            RedisUtils.publish(key, SerializationUtils.serialize(responseEvent));
                        }
                    }
                } else if ((status >= Response.TRYING) && (status < Response.OK)) {
                    // 增加其它无需回复的响应，如101、180等
                } else {
                    log.warn("接收到失败的response响应！status：" + status + ",message:" + response.getReasonPhrase());
                    if (responseEvent.getResponse() != null) {
                        CallIdHeader callIdHeader = (CallIdHeader)responseEvent.getResponse().getHeader(CallIdHeader.NAME);
                        if (callIdHeader != null) {
                            String key = String.format("%s%s", SipSubscribeHandle.VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER, callIdHeader.getCallId());
                            RedisUtils.publish(key,SerializationUtils.serialize(responseEvent));
                        }
                    }
                    if (responseEvent.getDialog() != null) {
                        responseEvent.getDialog().delete();
                    }
                }
            }catch (Exception e){
                log.error("ResponseEvent事件消息处理异常",e);
            }
        });
    }

    /**
     * 向超时订阅发送消息
     * 分发给mq
     */
    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        ThreadUtil.execute(()->{
            try {
                SipLogUtils.receiveMessage(sipServer,timeoutEvent);
                if(sipTimeoutEvent != null){
                    sipTimeoutEvent.process(timeoutEvent);
                }
            }catch (Exception e){
                log.error("超时订阅事件消息处理异常",e);
            }
        });
    }

    @Override
    public void processIOException(IOExceptionEvent e) {
        log.error("processIOException : {}", e);
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {

    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent event) {
        CallIdHeader callId = event.getDialog().getCallId();
        log.info("processDialogTerminated :{}",callId);
    }

}
