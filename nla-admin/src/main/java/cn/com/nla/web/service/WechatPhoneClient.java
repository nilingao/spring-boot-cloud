package cn.com.nla.web.service;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.json.utils.JsonUtils;
import cn.com.nla.web.config.properties.XcxProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * 微信手机号快速验证：stable_token → getuserphonenumber。
 * <p>
 * 只允许固定 HTTPS 供应商地址；异常中不传播 secret 或原始响应。
 * </p>
 *
 * @author TZY
 */
@Component
public class WechatPhoneClient {

    private static final int MAX_BYTES = 64 * 1024;
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER).build();

    /**
     * 用小程序 phoneCode 换取用户手机号。
     *
     * @param appid     小程序 appid
     * @param app       服务端配置
     * @param phoneCode wx.getPhoneNumber() 返回的一次性 code
     * @return 纯电话号码（不含国家码前缀）
     * @throws ServiceException 供应商失败或返回无效数据
     */
    public String resolvePhoneNumber(String appid, XcxProperties.App app, String phoneCode) {
        try {
            // 1. 获取 access_token
            var tokenReply = post("/cgi-bin/stable_token", Map.of(
                "grant_type", "client_credential",
                "appid", appid,
                "secret", app.getSecret(),
                "force_refresh", false));
            var tokenMap = JsonUtils.parseMap(new String(tokenReply, StandardCharsets.UTF_8));
            Object accessToken = tokenMap == null ? null : tokenMap.get("access_token");
            if (tokenMap == null
                || (tokenMap.containsKey("errcode") && !"0".equals(String.valueOf(tokenMap.get("errcode"))))
                || !(accessToken instanceof String value) || !value.matches("[A-Za-z0-9_-]{1,4096}")) {
                throw new IllegalStateException("token failed");
            }
            // 2. 调用手机号获取接口
            byte[] phoneReply = post("/wxa/business/getuserphonenumber?access_token=" + value,
                Map.of("code", phoneCode));
            var phoneMap = JsonUtils.parseMap(new String(phoneReply, StandardCharsets.UTF_8));
            if (phoneMap == null
                || (phoneMap.containsKey("errcode") && !"0".equals(String.valueOf(phoneMap.get("errcode"))))) {
                throw new IllegalStateException("phone api failed");
            }
            Object phoneInfo = phoneMap.get("phone_info");
            if (!(phoneInfo instanceof Map<?, ?> info)) {
                throw new IllegalStateException("no phone_info");
            }
            Object purePhone = info.get("purePhoneNumber");
            if (!(purePhone instanceof String phone) || phone.isBlank() || phone.length() > 20) {
                throw new IllegalStateException("invalid phone");
            }
            return phone;
        } catch (RuntimeException e) {
            // 不传播含 app secret、access_token 或供应商原文的异常。
            throw new ServiceException("手机号获取失败，请重新授权");
        }
    }

    /**
     * 网络边界方法，测试替换此方法不发起真实外部请求。
     */
    protected byte[] post(String path, Map<String, Object> body) {
        var request = HttpRequest.newBuilder(URI.create("https://api.weixin.qq.com" + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJsonString(body)))
            .build();
        var pending = http.sendAsync(request, info -> new LimitedBodySubscriber());
        try {
            var response = pending.get(10, TimeUnit.SECONDS);
            if (response.statusCode() != 200) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException();
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException();
        } finally {
            pending.cancel(true);
        }
    }

    /** 拒绝超大响应体，与 WechatQrClient 同构。 */
    static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
        private Flow.Subscription subscription;
        private long received;

        @Override
        public java.util.concurrent.CompletionStage<byte[]> getBody() {
            return delegate.getBody();
        }

        @Override
        public void onSubscribe(Flow.Subscription value) {
            subscription = value;
            delegate.onSubscribe(value);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            for (var item : items) {
                received += item.remaining();
            }
            if (received > MAX_BYTES) {
                subscription.cancel();
                delegate.onError(new IOException("Response too large"));
            } else {
                delegate.onNext(items);
            }
        }

        @Override
        public void onError(Throwable error) {
            delegate.onError(error);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }
    }
}
