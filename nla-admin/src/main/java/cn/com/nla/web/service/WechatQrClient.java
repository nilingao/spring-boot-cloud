package cn.com.nla.web.service;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.json.utils.JsonUtils;
import cn.com.nla.web.config.properties.XcxProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/** 微信 stable_token + 不限次数小程序码；只允许固定 HTTPS 供应商地址。 */
@Component
public class WechatQrClient {
    private static final int MAX_BYTES = 1024 * 1024;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER).build();

    public String generate(String appid, XcxProperties.App app, String scene) {
        try {
            var tokenReply = post("/cgi-bin/stable_token", Map.of("grant_type", "client_credential",
                "appid", appid, "secret", app.getSecret(), "force_refresh", false));
            var token = JsonUtils.parseMap(new String(tokenReply, StandardCharsets.UTF_8));
            Object accessToken = token == null ? null : token.get("access_token");
            if (token == null || (token.containsKey("errcode") && !"0".equals(String.valueOf(token.get("errcode"))))
                || !(accessToken instanceof String value) || !value.matches("[A-Za-z0-9_-]{1,4096}")) {
                throw new IllegalStateException();
            }
            byte[] img = post("/wxa/getwxacodeunlimit?access_token=" + value,
                Map.of("scene", scene, "page", app.getQrPage(), "check_path", true,
                    "env_version", app.getQrEnvVersion(), "width", 430));
            // 微信错误响应也是 HTTP 200 JSON；不能作为图片传给网页。
            String mime;
            if (img.length >= 8 && img[0] == (byte) 0x89 && img[1] == 'P' && img[2] == 'N'
                && img[3] == 'G' && img[4] == 13 && img[5] == 10 && img[6] == 26 && img[7] == 10) {
                mime = "image/png";
            } else if (img.length >= 3 && img[0] == (byte) 0xff && img[1] == (byte) 0xd8 && img[2] == (byte) 0xff) {
                mime = "image/jpeg";
            } else { throw new IllegalStateException(); }
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(img);
        } catch (RuntimeException e) {
            // 不传播含 app secret、access_token 或供应商原文的异常。
            throw new ServiceException("微信二维码生成失败，请稍后重试");
        }
    }

    /** 测试仅替换此网络边界。响应大小/超时有上限，不记录请求或供应商原文。 */
    protected byte[] post(String path, Map<String, Object> body) {
        var request = HttpRequest.newBuilder(URI.create("https://api.weixin.qq.com" + path))
            .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJsonString(body))).build();
        var pending = http.sendAsync(request, info -> new LimitedBodySubscriber());
        try {
            var response = pending.get(10, TimeUnit.SECONDS);
            if (response.statusCode() != 200) { throw new IllegalStateException(); }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException();
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException();
        } finally { pending.cancel(true); }
    }

    /** 接收过程中拒绝超大响应，完整响应读取也受请求总超时约束。 */
    static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
        private Flow.Subscription subscription;
        private long received;
        @Override public CompletionStage<byte[]> getBody() { return delegate.getBody(); }
        @Override public void onSubscribe(Flow.Subscription value) {
            subscription = value;
            delegate.onSubscribe(value);
        }
        @Override public void onNext(List<ByteBuffer> items) {
            for (var item : items) { received += item.remaining(); }
            if (received > MAX_BYTES) {
                subscription.cancel();
                delegate.onError(new IOException("Response too large"));
            } else { delegate.onNext(items); }
        }
        @Override public void onError(Throwable error) { delegate.onError(error); }
        @Override public void onComplete() { delegate.onComplete(); }
    }
}
