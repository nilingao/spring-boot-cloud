package cn.com.nla.common.freeswitch.redis.impl.result;

import cn.com.nla.common.freeswitch.enums.ConstEnum;
import cn.com.nla.common.freeswitch.utils.DynamicTask;
import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.freeswitch.vo.result.DeferredResultVo;
import cn.com.nla.common.freeswitch.vo.result.FsRestResult;
import cn.com.nla.common.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步请求轮训类
 * @param 
 */
@Slf4j
@Component
public class DeferredResultHolder {
    private static final Integer millis = 10;
    private static final String VIDEO_DEFERRED_RESULT_HOLDER = SipConstant.VIDEO_DEFERRED_RESULT_HOLDER;


    /**
     * login 登陆 key
     */
    public static final String AGENT_LOGIN = "agent_login_";
    /**
     * 请求拨打电话
     */
    public static final String CALL_PHONE = "call_phone_";
    /**
     * 停止点播key
     */
    public static final String CALLBACK_CMD_STOP = "callback_stop_";
    /**
     * 获取移动位置信息
     */
    public static final String CALLBACK_STREAM_NONE_READER = "callback_stream_none_reader_";


    private Map<String, Map<String,LocalDateTime>> resultDateMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, CompletableFuture>> resultMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> resultListenerMap = new ConcurrentHashMap<>();

    public DeferredResultHolder(DynamicTask dynamicTask){
        dynamicTask.startCron(VIDEO_DEFERRED_RESULT_HOLDER,millis, this::execute);
    }
    /**
     * 对订阅数据进行过期清理
     */
    public void execute(){
        LocalDateTime date = LocalDateTime.now();
        for (Map.Entry<String, Map<String, LocalDateTime>> entry : resultDateMap.entrySet()) {
            for (Map.Entry<String, LocalDateTime> dateEntry : entry.getValue().entrySet()) {
                if(date.compareTo(dateEntry.getValue()) > 0){
                    del(entry.getKey(),dateEntry.getKey());
                }
            }
        }
        log.info("[定时任务] 清理查询异步回调, resultDateMap : {},resultMap : {}",resultDateMap.size(),resultMap.size());
    }

    public boolean exist(String key, String id){
        if (key == null) {
            return false;
        }
        Map<String, CompletableFuture> deferredResultMap = resultMap.get(key);
        if (id == null) {
            return deferredResultMap != null;
        }else {
            return deferredResultMap != null && deferredResultMap.get(id) != null;
        }
    }

    /**
     * 释放单个请求
     */
    public void invokeResult(String key,String id,Object data) {
        RedisUtils.publish(VIDEO_DEFERRED_RESULT_HOLDER, SerializationUtils.serialize(DeferredResultVo.builder().onAll(ConstEnum.Flag.NO.getValue()).key(key).id(id).data(data).build()));
    }

    /**
     * 释放所有的请求
     */
    public void invokeAllResult(String key,Object data) {
        RedisUtils.publish(VIDEO_DEFERRED_RESULT_HOLDER,SerializationUtils.serialize(DeferredResultVo.builder().onAll(ConstEnum.Flag.YES.getValue()).key(key).id(null).data(data).build()));
    }

    public void put(String key, String id, FsRestResult result) {
        Map<String, CompletableFuture> deferredResultMap = resultMap.computeIfAbsent(key, o -> new ConcurrentHashMap<>());
        deferredResultMap.put(id, result);
        Map<String, LocalDateTime> stringDateMap = resultDateMap.computeIfAbsent(key, o -> new ConcurrentHashMap<>());
        stringDateMap.put(id, LocalDateTime.now().plusSeconds(((int) (result.getTimeoutValue()/1000))));
        //添加订阅
        int listenerId = RedisUtils.subscribeAndGetListenerId(VIDEO_DEFERRED_RESULT_HOLDER, byte[].class, body -> {
            DeferredResultVo vo = (DeferredResultVo) SerializationUtils.deserialize(body);
            if(vo == null){
                return;
            }
            Map<String, CompletableFuture> currentResultMap = resultMap.get(key);
            if (currentResultMap == null) {
                return;
            }
            if(vo.getOnAll() == ConstEnum.Flag.YES.getValue()){
                for (Map.Entry<String, CompletableFuture> entry : currentResultMap.entrySet()) {
                    entry.getValue().complete(vo.getData());
                    del(key,entry.getKey());
                }
            }else {
                CompletableFuture deferredResult = currentResultMap.get(vo.getId());
                if(deferredResult != null){
                    deferredResult.complete(vo.getData());
                    del(key,vo.getId());
                }
            }
        });
        String k = String.format("%s%s:%s",VIDEO_DEFERRED_RESULT_HOLDER,key,id);
        resultListenerMap.put(k,listenerId);
    }

    public void del(String key, String id) {
        if(StringUtils.isEmpty(id)){
            log.error("[请求回调] : 未获取删除回调 ID");
            return;
        }
        Map<String, CompletableFuture> deferredResultMap = resultMap.get(key);
        if (deferredResultMap != null && !deferredResultMap.isEmpty()) {
            deferredResultMap.remove(id);
        }
        if(deferredResultMap == null || deferredResultMap.isEmpty()){
            resultMap.remove(key);
        }
        Map<String, LocalDateTime> stringDateMap = resultDateMap.get(key);
        if (stringDateMap != null && !stringDateMap.isEmpty()) {
            stringDateMap.remove(id);
        }
        if(stringDateMap == null || stringDateMap.isEmpty()){
            resultDateMap.remove(key);
        }
        String k = String.format("%s%s:%s",VIDEO_DEFERRED_RESULT_HOLDER,key,id);
        Integer listenerId = resultListenerMap.remove(k);
        if(listenerId != null){
            RedisUtils.unsubscribe(VIDEO_DEFERRED_RESULT_HOLDER, listenerId);
        }
    }
}
