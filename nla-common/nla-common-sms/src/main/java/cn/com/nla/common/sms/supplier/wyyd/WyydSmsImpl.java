package cn.com.nla.common.sms.supplier.wyyd;

import cn.com.nla.common.sms.supplier.SmsSignUtils;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.provider.service.AbstractSmsBlend;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 网易易盾（wyyd）发送实现，迁移自旧 {@code WyydClient}。
 * <p>
 * 模板型渠道：表单 POST，签名规则为「参数名 ASCII 升序 → 拼接 key+value → 追加 secretKey → MD5(UTF-8) 十六进制小写」。
 * 响应 {@code code=200} 且 {@code data.result=200} 视为成功，{@code data.requestId} 为消息 ID。
 * </p>
 * <p>
 * 易盾仅支持模板短信，故 {@link #sendMessage(String, String)}（纯内容）直接返回失败响应。
 * </p>
 *
 * @author TZY
 */
@Slf4j
public class WyydSmsImpl extends AbstractSmsBlend<WyydConfig> {

    /**
     * 易盾短信业务对应的 smsType（用于多渠道模板编号解析）
     */
    private static final int SMS_TYPE = 6;

    public WyydSmsImpl(WyydConfig config) {
        super(config);
    }

    @Override
    public String getSupplier() {
        return WyydConfig.SUPPLIER;
    }

    @Override
    public SmsResponse sendMessage(String phone, String templateId, LinkedHashMap<String, String> messages) {
        WyydConfig config = getConfig();
        SmsResponse response = new SmsResponse();
        response.setConfigId(config.getConfigId());
        try {
            Map<String, String> params = new HashMap<>();
            // 1.公共参数
            params.put("secretId", config.getAccessKeyId());
            params.put("businessId", config.getBusinessId());
            params.put("version", "v2");
            params.put("timestamp", String.valueOf(System.currentTimeMillis()));
            params.put("nonce", String.valueOf(new Random().nextInt()));
            // 2.私有参数
            params.put("mobile", phone);
            params.put("templateId", SmsSignUtils.parseTemplateCode(SMS_TYPE, templateId));
            params.put("params", (messages == null || messages.isEmpty()) ? "{}" : JSONUtil.toJsonStr(messages));
            params.put("paramType", "json");
            // 3.签名（须在放入 signature 之前计算）
            params.put("signature", genSignature(config.getAccessKeySecret(), params));

            try (HttpResponse httpResponse = HttpRequest.post(config.getServerUrl())
                .form(new HashMap<String, Object>(params))
                .timeout(5000)
                .execute()) {
                String body = httpResponse.body();
                if (httpResponse.getStatus() == 200 && StrUtil.isNotEmpty(body)) {
                    JSONObject obj = JSONUtil.parseObj(body);
                    Integer code = obj.getInt("code");
                    JSONObject data = obj.getJSONObject("data");
                    if (code != null && code == 200 && data != null && Integer.valueOf(200).equals(data.getInt("result"))) {
                        response.setSuccess(true);
                        response.setData(data.getStr("requestId"));
                    } else {
                        response.setSuccess(false);
                        response.setData(obj.getStr("msg"));
                    }
                } else {
                    response.setSuccess(false);
                    response.setData("httpCode " + httpResponse.getStatus());
                }
            }
        } catch (Exception e) {
            log.error("网易易盾(wyyd)发送失败", e);
            response.setSuccess(false);
            response.setData("与短信网关连接失败");
        }
        return response;
    }

    @Override
    public SmsResponse sendMessage(String phone, String message) {
        return errorResp("网易易盾(wyyd)仅支持模板短信，请使用模板编号发送");
    }

    @Override
    public SmsResponse sendMessage(String phone, LinkedHashMap<String, String> messages) {
        return sendMessage(phone, getConfig().getTemplateId(), messages);
    }

    @Override
    public SmsResponse massTexting(List<String> phones, String message) {
        return errorResp("网易易盾(wyyd)仅支持模板短信，请使用模板编号发送");
    }

    @Override
    public SmsResponse massTexting(List<String> phones, String templateId, LinkedHashMap<String, String> messages) {
        SmsResponse last = null;
        for (String phone : phones) {
            last = sendMessage(phone, templateId, messages);
        }
        return last;
    }

    /**
     * 生成网易易盾签名：参数名 ASCII 升序 → 拼接 key+value → 追加 secretKey → MD5(UTF-8)。
     *
     * @param secretKey 产品私钥
     * @param params    请求参数（不含 signature）
     * @return 32 位十六进制小写签名
     */
    private static String genSignature(String secretKey, Map<String, String> params) {
        String[] keys = params.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        StringBuilder buffer = new StringBuilder();
        for (String key : keys) {
            buffer.append(key).append(params.get(key) == null ? "" : params.get(key));
        }
        buffer.append(secretKey);
        return DigestUtils.md5DigestAsHex(buffer.toString().getBytes(StandardCharsets.UTF_8));
    }
}
