package cn.com.nla.common.sms.supplier.swlh;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 商务领航（swlh）发送实现，迁移自旧 {@code SwlhHttpClient}。
 * <p>
 * 内容型渠道：表单 POST，响应为 XML {@code <string>00</string>}（00 表示成功）。
 * </p>
 *
 * @author TZY
 */
@Slf4j
public class SwlhSmsImpl extends AbstractSmsBlend<SwlhConfig> {

    /**
     * 提取 XML {@code <string>} 节点文本
     */
    private static final Pattern STRING_NODE = Pattern.compile("<string[^>]*>([^<]*)</string>");

    /**
     * 响应码 → 提示信息
     */
    private static final Map<String, String> RESP_CODE = Map.of(
        "01", "号码（超过上限50个）、内容等为空或内容长度超过210",
        "02", "用户鉴权失败",
        "03", "登录IP黑名单",
        "10", "余额不足",
        "99", "服务器接受失败"
    );

    public SwlhSmsImpl(SwlhConfig config) {
        super(config);
    }

    @Override
    public String getSupplier() {
        return SwlhConfig.SUPPLIER;
    }

    @Override
    public SmsResponse sendMessage(String phone, String message) {
        SwlhConfig config = getConfig();
        String content = SmsSignUtils.handleSign(config.getSignature(), config.getSignPlace(), message);
        String[] accounts = StrUtil.splitToArray(StrUtil.nullToEmpty(config.getAccessKeyId()), '|');
        String epid = accounts.length > 0 ? accounts[0] : "";
        String userName = accounts.length > 1 ? accounts[1] : "";

        SmsResponse response = new SmsResponse();
        response.setConfigId(config.getConfigId());
        try (HttpResponse httpResponse = HttpRequest.post(config.getServerUrl())
            .form("epid", epid)
            .form("User_Name", userName)
            .form("password", config.getAccessKeySecret())
            .form("phone", phone)
            .form("content", content)
            .form("ExtendCode", "")
            .charset("UTF-8")
            .timeout(5000)
            .execute()) {
            String body = httpResponse.body();
            if (httpResponse.getStatus() == 200 && StrUtil.isNotEmpty(body)) {
                Matcher matcher = STRING_NODE.matcher(body);
                String code = matcher.find() ? matcher.group(1).trim() : null;
                if ("00".equals(code)) {
                    response.setSuccess(true);
                    response.setData(body);
                } else {
                    response.setSuccess(false);
                    response.setData(code == null ? "解析XML失败" : RESP_CODE.getOrDefault(code, "未知代码: " + code));
                }
            } else {
                response.setSuccess(false);
                response.setData("httpCode " + httpResponse.getStatus());
            }
        } catch (Exception e) {
            log.error("商务领航(swlh)发送失败", e);
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
