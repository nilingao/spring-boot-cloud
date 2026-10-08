package cn.com.nla.common.sms.supplier.wnd;

import cn.com.nla.common.sms.supplier.SmsSignUtils;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.provider.service.AbstractSmsBlend;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维纳多（wnd）发送实现，迁移自旧 {@code WndHttpClient}。
 * <p>
 * 内容型渠道：请求体 JSON 经 AES（密钥取自 password，SHA1PRNG 确定性种子）加密为十六进制大写串，
 * 以表单 {@code u/p/c/m} 提交；请求 UTF-8、响应 GBK。{@code errid=0} 且 success 段含手机号视为成功。
 * </p>
 * <p>
 * 注意：旧实现依赖 {@code com.sun.crypto.provider.SunJCE}，JDK 9+ 已不可访问，此处移除；
 * SunJCE 为 JDK 默认注册 Provider，行为不变。
 * </p>
 *
 * @author TZY
 */
@Slf4j
public class WndSmsImpl extends AbstractSmsBlend<WndConfig> {

    /**
     * 响应字符集（维纳多网关返回 GBK）
     */
    private static final Charset RESP_CHARSET = Charset.forName("GBK");

    /**
     * 响应码 → 提示信息
     */
    private static final Map<String, String> RESP_CODE = Map.ofEntries(
        Map.entry("6002", "用户帐号不正确"),
        Map.entry("6008", "无效的手机号码"),
        Map.entry("6009", "手机号码是黑名单"),
        Map.entry("6010", "用户密码不正确"),
        Map.entry("6011", "短信内容超过了最大长度限制"),
        Map.entry("6012", "该企业用户设置了 ip 限制"),
        Map.entry("6013", "该企业用户余额不足"),
        Map.entry("6014", "发送短信内容不能为空"),
        Map.entry("6015", "发送内容中含非法字符"),
        Map.entry("6019", "账户已停机，请联系客服"),
        Map.entry("6021", "扩展号码未备案"),
        Map.entry("6023", "发送手机号码超过太长，已超过 300 个号码"),
        Map.entry("6024", "定制时间不正确"),
        Map.entry("6025", "扩展号码太长（总长度超过 20 位）"),
        Map.entry("6080", "提交异常，请联系服务商解决"),
        Map.entry("6085", "短信内容为空")
    );

    public WndSmsImpl(WndConfig config) {
        super(config);
    }

    @Override
    public String getSupplier() {
        return WndConfig.SUPPLIER;
    }

    @Override
    public SmsResponse sendMessage(String phone, String message) {
        WndConfig config = getConfig();
        String content = SmsSignUtils.handleSign(config.getSignature(), config.getSignPlace(), message);
        SmsResponse response = new SmsResponse();
        response.setConfigId(config.getConfigId());
        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("name", config.getAccessKeyId());
            req.put("psw", config.getAccessKeySecret());
            req.put("dst", phone);
            req.put("msg", URLEncoder.encode(content, StandardCharsets.UTF_8));
            req.put("time", "");
            req.put("sender", "");
            req.put("sequeid", String.valueOf(System.currentTimeMillis()));
            String m = aesEncryptHex(JSONUtil.toJsonStr(req), config.getAccessKeySecret());

            try (HttpResponse httpResponse = HttpRequest.post(config.getServerUrl())
                .form("u", config.getAccessKeyId())
                .form("p", "1")
                .form("c", "3")
                .form("m", m)
                .charset("UTF-8")
                .timeout(5000)
                .execute()) {
                String body = new String(httpResponse.bodyBytes(), RESP_CHARSET);
                if (httpResponse.getStatus() == 200 && StrUtil.isNotEmpty(body)) {
                    JSONObject obj = JSONUtil.parseObj(body);
                    String errid = obj.getStr("errid");
                    if ("0".equals(errid)) {
                        String success = obj.getStr("success");
                        if (StrUtil.isNotEmpty(success) && success.contains(phone)) {
                            response.setSuccess(true);
                            response.setData(body);
                        } else {
                            response.setSuccess(false);
                            response.setData(obj.getStr("err"));
                        }
                    } else {
                        response.setSuccess(false);
                        response.setData(RESP_CODE.getOrDefault(errid, body));
                    }
                } else {
                    response.setSuccess(false);
                    response.setData("httpCode " + httpResponse.getStatus());
                }
            }
        } catch (Exception e) {
            log.error("维纳多(wnd)发送失败", e);
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

    /**
     * AES 加密并转十六进制大写串（密钥由 password 经 SHA1PRNG 确定性种子派生，兼容旧网关）。
     *
     * @param data     明文
     * @param password 密钥原文
     * @return 十六进制大写密文
     */
    private static String aesEncryptHex(String data, String password) {
        try {
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(password.getBytes(StandardCharsets.UTF_8));
            keygen.init(secureRandom);
            SecretKey key = keygen.generateKey();
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cipherBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexUtil.encodeHexStr(cipherBytes).toUpperCase();
        } catch (Exception e) {
            throw new IllegalStateException("维纳多(wnd)AES加密失败", e);
        }
    }

    private String joinValues(LinkedHashMap<String, String> messages) {
        return (messages == null || messages.isEmpty()) ? "" : String.join("", messages.values());
    }
}
