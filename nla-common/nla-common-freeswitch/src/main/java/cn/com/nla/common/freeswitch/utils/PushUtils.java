package cn.com.nla.common.freeswitch.utils;

import cn.com.nla.common.freeswitch.model.fs.CallLogPush;
import cn.com.nla.common.json.utils.JsonUtils;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 话单回调推送工具。
 * <p>
 * 迁移说明：旧实现基于 forest + springbootcomm RestResult（Boot2 产物、Web 响应包装），
 * 现改用 hutool HTTP，去 Web 化后以 HTTP 2xx 判定推送成功，仅返回布尔结果。
 *
 * @author TZY
 */
@Slf4j
public class PushUtils {

    private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
    private static final int READ_TIMEOUT = 10000;

    /**
     * 推送话单数据到企业回调地址。
     *
     * @param url   回调地址
     * @param param 话单数据
     * @return true=推送成功（HTTP 2xx），false=失败或异常
     */
    public static boolean request(String url, CallLogPush param) {
        try (HttpResponse response = HttpRequest.post(url)
                .contentType(CONTENT_TYPE_JSON)
                .body(JsonUtils.toJsonString(param))
                .timeout(READ_TIMEOUT)
                .execute()) {
            return response.isOk();
        } catch (Exception e) {
            log.error("推送数据时 发生错误 : ", e);
            return false;
        }
    }
}
