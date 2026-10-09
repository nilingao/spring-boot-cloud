package cn.com.nla.common.freeswitch.redis.subscribe.notify;

import cn.com.nla.common.freeswitch.utils.DynamicTask;
import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class AgentLoginSubscribe {

    private final static String FS_AGENT_LOGIN_SUBSCRIBE_MANAGER = SipConstant.FS_AGENT_LOGIN_SUBSCRIBE_MANAGER;
    public final static String FS_AGENT_LOGIN_EVENT_SUBSCRIBE_MANAGER = SipConstant.FS_AGENT_LOGIN_EVENT_SUBSCRIBE_MANAGER;
    private final Integer millis = 8;

    private final Map<String, Integer> loginMessageListener = new ConcurrentHashMap<>();

    private final Map<String, Instant> loginTimeSubscribes = new ConcurrentHashMap<>();

    private final Map<String, SipLoginEvent> loginSubscribes = new ConcurrentHashMap<>();



    public AgentLoginSubscribe(DynamicTask dynamicTask){
        dynamicTask.startCron(FS_AGENT_LOGIN_SUBSCRIBE_MANAGER,millis, this::execute);
    }

    public void execute(){
        Instant instant = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(millis));
        for (String key : loginTimeSubscribes.keySet()) {
            if (loginTimeSubscribes.get(key).isBefore(instant)){
                removeOkSubscribe(key);
            }
        }
        log.info("[定时任务] 清理过期的SIP订阅信息, loginSubscribes : {},loginSubscribes : {}",loginSubscribes.size(),loginSubscribes.size());
    }
    
    public void addLoginSubscribe(String key, SipLoginEvent event) {
        loginTimeSubscribes.put(key, Instant.now());
        loginSubscribes.put(key,event);
        String redisListenerKey = String.format("%s%s", FS_AGENT_LOGIN_EVENT_SUBSCRIBE_MANAGER, key);
        //创建监听
        loginMessageListener.computeIfAbsent(key, o -> RedisUtils.subscribeAndGetListenerId(redisListenerKey, byte[].class, body -> {
            SipLoginEvent okSubscribe = loginSubscribes.get(key);
            if(okSubscribe != null){
                okSubscribe.run();
                return;
            }
            //移除
            removeAllSubscribe(key);
        }));
    }

    public  void removeAllSubscribe(String key){
        removeOkSubscribe(key);
    }

    public void removeOkSubscribe(String key) {
        if(key == null){
            return;
        }
        loginSubscribes.remove(key);
        loginTimeSubscribes.remove(key);
        Integer listenerId = loginMessageListener.remove(key);
        if(listenerId != null){
            RedisUtils.unsubscribe(String.format("%s%s", FS_AGENT_LOGIN_EVENT_SUBSCRIBE_MANAGER, key), listenerId);
        }
    }
}
