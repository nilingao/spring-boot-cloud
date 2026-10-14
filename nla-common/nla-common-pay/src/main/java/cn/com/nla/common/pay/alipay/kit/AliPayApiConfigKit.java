package cn.com.nla.common.pay.alipay.kit;

import cn.com.nla.common.pay.alipay.config.AliPayApiConfig;
import cn.hutool.core.util.StrUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AliPayApiConfigKit {
    private static final ThreadLocal<String> TL = new ThreadLocal<String>();

    private static final Map<String, AliPayApiConfig> CFG_MAP = new ConcurrentHashMap<String, AliPayApiConfig>();
    private static final String DEFAULT_CFG_KEY = "_default_key_";

    /**
     * <p>向缓存中设置 AliPayModel </p>
     * <p>每个 appId 只需添加一次，相同 appId 将被覆盖</p>
     *
     * @param AliPayApiConfig 支付宝支付配置
     * @return {@link AliPayApiConfig}
     */
    public static AliPayApiConfig putApiConfig(AliPayApiConfig AliPayApiConfig) {
        if (CFG_MAP.size() == 0) {
            CFG_MAP.put(DEFAULT_CFG_KEY, AliPayApiConfig);
        }
        CFG_MAP.put(AliPayApiConfig.getAppId(), AliPayApiConfig);
        return AliPayApiConfig;
    }

    /**
     * 向当前线程中设置 {@link AliPayApiConfig}
     *
     * @param AliPayApiConfig {@link AliPayApiConfig} 支付宝配置对象
     * @return {@link AliPayApiConfig}
     */
    public static AliPayApiConfig setThreadLocalAliPayModel(AliPayApiConfig AliPayApiConfig) {
        if (StrUtil.isNotEmpty(AliPayApiConfig.getAppId())) {
            setThreadLocalAppId(AliPayApiConfig.getAppId());
        }
        return putApiConfig(AliPayApiConfig);
    }

    /**
     * 通过 AliPayModel 移除支付配置
     *
     * @param AliPayApiConfig {@link AliPayApiConfig} 支付宝配置对象
     * @return {@link AliPayApiConfig}
     */
    public static AliPayApiConfig removeApiConfig(AliPayApiConfig AliPayApiConfig) {
        return removeApiConfig(AliPayApiConfig.getAppId());
    }

    /**
     * 通过 appId 移除支付配置
     *
     * @param appId 支付宝应用编号
     * @return {@link AliPayApiConfig}
     */
    public static AliPayApiConfig removeApiConfig(String appId) {
        return CFG_MAP.remove(appId);
    }

    /**
     * 向当前线程中设置 appId
     *
     * @param appId 支付宝应用编号
     */
    public static void setThreadLocalAppId(String appId) {
        if (StrUtil.isEmpty(appId)) {
            appId = CFG_MAP.get(DEFAULT_CFG_KEY).getAppId();
        }
        TL.set(appId);
    }

    /**
     * 移除当前线程中的 appId
     */
    public static void removeThreadLocalAppId() {
        TL.remove();
    }

    /**
     * 获取当前线程中的  appId
     *
     * @return 支付宝应用编号 appId
     */
    public static String getAppId() {
        String appId = TL.get();
        if (StrUtil.isEmpty(appId)) {
            appId = CFG_MAP.get(DEFAULT_CFG_KEY).getAppId();
        }
        return appId;
    }

    /**
     * 获取当前线程中的 AliPayModel
     *
     * @return {@link AliPayApiConfig}
     */
    public static AliPayApiConfig getAliPayModel() {
        String appId = getAppId();
        return getAliPayApiConfig(appId);
    }

    /**
     * 通过 appId 获取 AliPayModel
     *
     * @param appId 支付宝应用编号
     * @return {@link AliPayApiConfig}
     */
    public static AliPayApiConfig getAliPayApiConfig(String appId) {
        AliPayApiConfig cfg = CFG_MAP.get(appId);
        if (cfg == null) {
            throw new IllegalStateException("需事先调用 AliPayModelKit.putApiConfig(AliPayModel) 将 appId对应的 AliPayModel 对象存入，才可以使用 AliPayModelKit.getAliPayModel() 的系列方法");
        }
        return cfg;
    }
}
