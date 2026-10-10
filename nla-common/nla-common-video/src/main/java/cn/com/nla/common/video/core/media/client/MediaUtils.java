package cn.com.nla.common.video.core.media.client;

import cn.com.nla.common.video.basic.vo.media.MediaRestResult;
import cn.com.nla.common.video.basic.vo.media.OnStreamChangedResult;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.basic.common.ZLMediaKitConstant;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import java.util.LinkedHashMap;
import java.util.Map;

/** ZLM HTTP query transport; connections and responses are bounded and closed.
 * @author TZY
 */
@Slf4j
public final class MediaUtils {
    private MediaUtils() { }
    public static MediaRestResult request(MediaServerVo server, String path, Object... params) {
        try { return request(server, path, MediaRestResult.class, params); }
        catch (Exception failure) {
            log.warn("ZLM request failed: {}", path, failure);
            return MediaRestResult.result(2, "请求流媒体错误");
        }
    }
    public static OnStreamChangedResult requestStreamChanged(MediaServerVo server, String path, Object... params) {
        try { return request(server, path, OnStreamChangedResult.class, params); }
        catch (Exception failure) {
            log.warn("ZLM stream query failed: {}", path, failure);
            return OnStreamChangedResult.result(2);
        }
    }
    private static <T> T request(MediaServerVo server, String path, Class<T> type, Object... params) {
        boolean ssl = Integer.valueOf(1).equals(server.getSslStatus());
        String host = server.getIp();
        if (host.contains(":") && !host.startsWith("[")) host = "[" + host + "]";
        String prefix = server.getVideoHttpPrefix();
        prefix = prefix == null || prefix.isBlank() ? "" : "/" + prefix.replaceAll("^/+|/+$", "");
        String url = (ssl ? "https" : "http") + "://" + host + ":" +
            (ssl ? server.getHttpSslPort() : server.getHttpPort()) + prefix + (path.startsWith("/") ? path : "/" + path);
        Map<String, Object> query = new LinkedHashMap<>();
        for (Object param : params) {
            if (param instanceof Map<?, ?> map) map.forEach((key, value) -> query.put(key.toString(), value));
            else if (param != null) query.putAll(BeanUtil.beanToMap(param));
        }
        query.put(ZLMediaKitConstant.MEDIA_SECRET, server.getSecret());
        try (HttpResponse response = HttpRequest.get(url).timeout(10000).form(query).execute()) {
            if (!response.isOk()) throw new IllegalStateException("ZLM HTTP status " + response.getStatus());
            return JSONUtil.toBean(response.body(), type);
        }
    }
}
