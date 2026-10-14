package cn.com.nla.common.pay;

import cn.com.nla.common.pay.alipay.config.AliPayApiConfig;
import cn.com.nla.common.pay.alipay.kit.AliPayApiConfigKit;
import cn.com.nla.common.pay.base.BaseModel;
import cn.com.nla.common.pay.jdpay.config.JdPayConfig;
import cn.com.nla.common.pay.jdpay.kit.JdPayConfigKit;
import cn.com.nla.common.pay.paypal.config.PayPalApiConfig;
import cn.com.nla.common.pay.paypal.kit.PayPalApiConfigKit;
import cn.com.nla.common.pay.qqpay.config.QqPayApiConfig;
import cn.com.nla.common.pay.qqpay.kit.QqPayApiConfigKit;
import com.ijpay.core.enums.SignType;
import com.ijpay.core.kit.HttpKit;
import com.ijpay.core.kit.PayKit;
import com.ijpay.core.kit.WxPayKit;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批次 6.7 pay 支付封装契约测试。
 *
 * <p>不启动 Spring 容器，验证三个契约面：
 * 1) 移植的四个渠道配置 Kit（alipay/qq/jd/paypal）的注册、线程绑定与缺配拒绝语义与旧工程一致；
 * 2) BaseModel 的 toMap/createSign 组包协议（空字段剔除 + MD5 签名）；
 * 3) 官方 IJPay 2.9.11 依赖在 JDK21 + 无 javax.servlet 环境下可加载可用（尖峰结论 5.3.2 落地验证）。</p>
 *
 * @author TZY
 */
class PaySdkContractTest {

    /**
     * BaseModel 组包用最小商户订单模型（需 public 顶层可见性，BaseModel 反射跨包 invoke getter）。
     */
    public static class DemoOrderModel extends BaseModel {
        private String outTradeNo = "T20261013000001";
        private String totalFee = "1";
        private String body;

        public String getOutTradeNo() {
            return outTradeNo;
        }

        public String getTotalFee() {
            return totalFee;
        }

        public String getBody() {
            return body;
        }
    }

    @Test
    void baseModelBuildsSignMapSkippingBlankFields() {
        DemoOrderModel model = new DemoOrderModel();
        Map<String, String> params = model.toMap();
        assertEquals("T20261013000001", params.get("outTradeNo"));
        assertEquals("1", params.get("totalFee"));
        // 空值字段不进入协议参数
        assertFalse(params.containsKey("body"));

        Map<String, String> signed = model.createSign("test-partner-key", SignType.MD5);
        assertNotNull(signed.get("sign"));
        assertFalse(signed.get("sign").isEmpty());
        // 签名后可通过官方 Kit 的验签回放
        assertTrue(WxPayKit.verifyNotify(signed, "test-partner-key", SignType.MD5));
    }

    @Test
    void aliPayKitBindsConfigAndRejectsUnknownAppId() {
        AliPayApiConfig config = AliPayApiConfig.builder()
            .setAppId("nla-pay-test-appid")
            .setPrivateKey("pid-key")
            .setAliPayPublicKey("pub-key")
            .setSignType("RSA2")
            .setFormat("json")
            .setCharset("UTF-8")
            .setServiceUrl("https://openapi.alipay.com/gateway.do");
        config.build();
        assertNotNull(config.getAliPayClient());

        AliPayApiConfigKit.putApiConfig(config);
        try {
            assertSame(config, AliPayApiConfigKit.getAliPayApiConfig("nla-pay-test-appid"));
            // 缺省线程上下文时回落默认配置
            AliPayApiConfigKit.removeThreadLocalAppId();
            assertSame(config, AliPayApiConfigKit.getAliPayModel());
            assertThrows(IllegalStateException.class,
                () -> AliPayApiConfigKit.getAliPayApiConfig("appid-not-registered"));
        } finally {
            AliPayApiConfigKit.removeApiConfig("nla-pay-test-appid");
            AliPayApiConfigKit.removeThreadLocalAppId();
        }
    }

    @Test
    void qqPayKitRoundTripKeepsLegacySemantics() {
        QqPayApiConfig config = QqPayApiConfig.builder().appId("qq-appid").mchId("qq-mch").partnerKey("k").build();
        QqPayApiConfigKit.putApiConfig(config);
        try {
            QqPayApiConfigKit.setThreadLocalAppId("qq-appid");
            assertSame(config, QqPayApiConfigKit.getQqPayApiConfig());
            assertThrows(IllegalStateException.class, () -> QqPayApiConfigKit.getApiConfig("qq-missing"));
        } finally {
            QqPayApiConfigKit.removeApiConfig("qq-appid");
            QqPayApiConfigKit.removeThreadLocalAppId();
        }
    }

    @Test
    void jdPayKitThreadLocalBindingUsesAppIdKey() {
        JdPayConfig config = JdPayConfig.builder().appId("jd-appid").mchId("jd-mch").build();
        JdPayConfigKit.setThreadLocalJdPayConfig(config);
        try {
            assertSame(config, JdPayConfigKit.getJdPayConfig());
            assertSame(config, JdPayConfigKit.getApiConfig("jd-appid"));
        } finally {
            JdPayConfigKit.removeApiConfig("jd-appid");
            JdPayConfigKit.removeThreadLocalAppId();
        }
        assertThrows(IllegalStateException.class, () -> JdPayConfigKit.getApiConfig("jd-appid"));
    }

    @Test
    void payPalKitUsesClientIdAsConfigKey() {
        PayPalApiConfig config = new PayPalApiConfig();
        config.setClientId("paypal-client");
        config.setSecret("s");
        config.setSandBox(true);
        PayPalApiConfigKit.putApiConfig(config);
        try {
            assertSame(config, PayPalApiConfigKit.getApiConfig("paypal-client"));
        } finally {
            PayPalApiConfigKit.removeApiConfig("paypal-client");
        }
        assertThrows(IllegalStateException.class, () -> PayPalApiConfigKit.getApiConfig("paypal-client"));
    }

    @Test
    void officialIJPayClassesLoadWithoutJavaxServlet() {
        // 尖峰 5.3.2 结论落地：javax.servlet 缺席时懒解析不阻塞核心 API 加载
        assertDoesNotThrow(() -> Class.forName("com.ijpay.wxpay.WxPayApi"));
        assertDoesNotThrow(() -> Class.forName("com.ijpay.wxpay.WxPayApiConfigKit"));
        assertThrows(ClassNotFoundException.class,
            () -> Class.forName("javax.servlet.http.HttpServletRequest"));
        assertNotNull(HttpKit.getDelegate());
    }

    @Test
    void wxPayKitXmlProtocolRoundTrip() {
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "T1");
        params.put("total_fee", "100");
        String xml = WxPayKit.toXml(params);
        assertTrue(xml.contains("out_trade_no"));
        assertTrue(xml.contains("T1"));
        Map<String, String> parsed = WxPayKit.xmlToMap(xml);
        assertEquals("T1", parsed.get("out_trade_no"));
        assertEquals("100", parsed.get("total_fee"));
    }

    @Test
    void payKitHmacMatchesJdkMacUnderBcJdk18onSwap() throws Exception {
        String data = "https://api.mch.weixin.qq.com/pay/micropay&body=nla";
        String key = "nla-hmac-key";
        String actual = PayKit.hmacSha256(data, key);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder expected = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            expected.append(String.format("%02x", b));
        }
        assertEquals(expected.toString(), actual);
    }
}
