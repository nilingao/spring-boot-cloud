package cn.com.nla.common.sms.supplier.dxw;

import cn.com.nla.common.sms.supplier.SmsSignUtils;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.provider.service.AbstractSmsBlend;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 短信网（dxw）发送实现，迁移自旧 {@code DxwHttpClient}。
 * <p>
 * 内容型渠道：表单 POST，响应为逗号前缀状态码（{@code 0,} 表示成功）。
 * </p>
 *
 * @author TZY
 */
@Slf4j
public class DxwSmsImpl extends AbstractSmsBlend<DxwConfig> {

    /**
     * 响应码 → 提示信息
     */
    private static final Map<String, String> RESP_CODE = Map.ofEntries(
        Map.entry("1,", "含有敏感词汇"),
        Map.entry("2,", "余额不足"),
        Map.entry("3,", "没有号码"),
        Map.entry("4,", "包含sql语句"),
        Map.entry("10,", "账号不存在"),
        Map.entry("11,", "账号注销"),
        Map.entry("12,", "账号停用"),
        Map.entry("13,", "IP鉴权失败"),
        Map.entry("14,", "格式错误"),
        Map.entry("-1,", "系统异常")
    );

    public DxwSmsImpl(DxwConfig config) {
        super(config);
    }

    @Override
    public String getSupplier() {
        return DxwConfig.SUPPLIER;
    }

    @Override
    public SmsResponse sendMessage(String phone, String message) {
        DxwConfig config = getConfig();
        String content = SmsSignUtils.handleSign(config.getSignature(), config.getSignPlace(), message);
        SmsResponse response = new SmsResponse();
        response.setConfigId(config.getConfigId());
        try (HttpResponse httpResponse = HttpRequest.post(config.getServerUrl())
            .form("name", config.getAccessKeyId())
            .form("pwd", config.getAccessKeySecret())
            .form("mobile", phone)
            .form("content", content)
            .form("type", "pt")
            .form("extno", "")
            .charset("UTF-8")
            .timeout(5000)
            .execute()) {
            String body = httpResponse.body();
            if (httpResponse.getStatus() == 200 && StrUtil.isNotEmpty(body)) {
                if (body.startsWith("0,")) {
                    response.setSuccess(true);
                    response.setData(body);
                } else {
                    response.setSuccess(false);
                    response.setData(RESP_CODE.entrySet().stream()
                        .filter(e -> body.startsWith(e.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(body));
                }
            } else {
                response.setSuccess(false);
                response.setData("httpCode " + httpResponse.getStatus());
            }
        } catch (Exception e) {
            log.error("短信网(dxw)发送失败", e);
            response.setSuccess(false);
            response.setData("与短信网关连接失败");
        }
        return response;
    }

    @Override
    public SmsResponse sendMessage(String phone, LinkedHashMap<String, String> messages) {
        return sendMessage(phone, joinValues(messages));
    }

    @Override
    public SmsResponse sendMessage(String phone, String templateId, LinkedHashMap<String, String> messages) {
        return sendMessage(phone, joinValues(messages));
    }

    @Override
    public SmsResponse massTexting(List<String> phones, String message) {
        SmsResponse last = null;
        for (String phone : phones) {
            last = sendMessage(phone, message);
        }
        return last;
    }

    @Override
    public SmsResponse massTexting(List<String> phones, String templateId, LinkedHashMap<String, String> messages) {
        SmsResponse last = null;
        for (String phone : phones) {
            last = sendMessage(phone, templateId, messages);
        }
        return last;
    }

    private String joinValues(LinkedHashMap<String, String> messages) {
        return (messages == null || messages.isEmpty()) ? "" : String.join("", messages.values());
    }
}
