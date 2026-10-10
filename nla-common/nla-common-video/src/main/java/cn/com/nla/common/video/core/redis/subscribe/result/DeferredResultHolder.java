package cn.com.nla.common.video.core.redis.subscribe.result;

import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.core.demo.DeferredResultVo;
import cn.com.nla.common.video.core.demo.VideoRestResult;
import cn.com.nla.common.video.core.redis.VideoMessageListener;
import cn.com.nla.common.video.core.utils.DynamicTask;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.SerializationUtils;

/** Routes asynchronous callbacks by request key and id across video nodes.
 * @author TZY
 */
public class DeferredResultHolder extends VideoMessageListener {
    public static final String PLAY_CALLBACK = "play_callback_";
    /**
     * 报警回复
     */
    public static final String CALLBACK_CMD_ALARM = "callback_alarm_";
    /**
     * 广播回复
     */
    public static final String CALLBACK_CMD_BROADCAST = "callback_broadcast_";
    /**
     * 设备下载回复
     */
    public static final String CALLBACK_CMD_CONFIGDOWNLOAD = "callback_configdownload_";
    /**
     * 设备控制命令
     */
    public static final String CALLBACK_CMD_DEVICECONTROL = "callback_devicecontrol_";

    /**
     * 设备配置命令
     */
    public static final String CALLBACK_CMD_DEVICECONFIG = "callback_deviceconfig_";
    public static final String CALLBACK_CMD_DEVICEINFO = "callback_deviceinfo_";
    public static final String CALLBACK_CMD_DEVICESTATUS = "callback_devicestatus_";
    /**
     * 点播信息key
     */
    public static final String CALLBACK_CMD_PLAY = "callback_play_";
    /**
     * 停止点播key
     */
    public static final String CALLBACK_CMD_STOP = "callback_stop_";
    /**
     * 预置位查询
     */
    public static final String CALLBACK_CMD_PRESETQUERY = "callback_presetquery_";
    /**
     * 历史录像播放信息key
     */
    public static final String CALLBACK_CMD_PLAYBACK = "callback_playback_";
    /**
     * 历史录像信息key
     */
    public static final String CALLBACK_CMD_RECORDINFO = "callback_recordinfo_";
    /**
     * 历史录像下载信息key
     */
    public static final String CALLBACK_CMD_DOWNLOAD = "callback_download_";
    /**
     * 获取移动位置信息
     */
    public static final String CALLBACK_CMD_MOBILEPOSITION = "callback_mobileposition_";
    /**
     * 获取移动位置信息
     */
    public static final String CALLBACK_STREAM_NONE_READER = "callback_stream_none_reader_";


    private final Map<String, Map<String, VideoRestResult<?>>> resultMap = new ConcurrentHashMap<>();

    public DeferredResultHolder(DynamicTask dynamicTask) { super(VideoConstant.VIDEO_DEFERRED_RESULT_HOLDER); }
    public boolean exist(String key, String id) {
        if (key == null) return false;
        Map<String, VideoRestResult<?>> requests = resultMap.get(key);
        return requests != null && (id == null ? !requests.isEmpty() : requests.containsKey(id));
    }
    public void invokeResult(String key, String id, Object data) { publish(key, id, data, 0); }
    public void invokeAllResult(String key, Object data) { publish(key, null, data, 1); }
    private void publish(String key, String id, Object data, int all) {
        RedisUtils.publish(VideoConstant.VIDEO_DEFERRED_RESULT_HOLDER,
            SerializationUtils.serialize(DeferredResultVo.builder().onAll(all).key(key).id(id).data(data).build()));
    }
    public synchronized void put(String key, String id, VideoRestResult<?> result) {
        Objects.requireNonNull(key); Objects.requireNonNull(id); Objects.requireNonNull(result);
        resultMap.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>()).put(id, result);
        result.whenComplete((value, error) -> remove(key, id, result));
    }
    private synchronized void remove(String key, String id, VideoRestResult<?> expected) {
        Map<String, VideoRestResult<?>> requests = resultMap.get(key);
        if (requests != null) {
            requests.remove(id, expected);
            if (requests.isEmpty()) resultMap.remove(key, requests);
        }
    }
    public synchronized void del(String key, String id) {
        if (key == null || id == null) return;
        Map<String, VideoRestResult<?>> requests = resultMap.get(key);
        if (requests != null) {
            requests.remove(id);
            if (requests.isEmpty()) resultMap.remove(key, requests);
        }
    }
    @Override
    public synchronized void onMessage(byte[] payload) {
        DeferredResultVo callback = (DeferredResultVo) SerializationUtils.deserialize(payload);
        if (callback == null || callback.getKey() == null) return;
        Map<String, VideoRestResult<?>> requests = resultMap.get(callback.getKey());
        if (requests == null) return;
        if (callback.getOnAll() == 1) {
            for (VideoRestResult<?> result : new ArrayList<>(requests.values())) complete(result, callback.getData());
        } else if (callback.getId() != null) {
            VideoRestResult<?> result = requests.get(callback.getId());
            if (result != null) complete(result, callback.getData());
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void complete(VideoRestResult result, Object value) { result.complete(value); }
    /** Kept for callers that explicitly request cleanup; completed futures remove themselves. */
    public void execute() { }
    @Override
    public synchronized void close() {
        super.close();
        var pending = resultMap.values().stream().flatMap(map -> map.values().stream()).toList();
        resultMap.clear();
        pending.forEach(result -> result.cancel(false));
    }
}
