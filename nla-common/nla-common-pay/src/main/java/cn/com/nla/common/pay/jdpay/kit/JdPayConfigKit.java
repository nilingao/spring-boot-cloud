package cn.com.nla.common.pay.jdpay.kit;

import cn.com.nla.common.pay.jdpay.config.JdPayConfig;
import cn.hutool.core.util.StrUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class JdPayConfigKit {

	private static final ThreadLocal<String> TL = new ThreadLocal<String>();

	private static final Map<String, JdPayConfig> CFG_MAP = new ConcurrentHashMap<String, JdPayConfig>();
	private static final String DEFAULT_CFG_KEY = "_default_key_";

	/**
	 * 添加微信支付配置，每个appId只需添加一次，相同appId将被覆盖
	 *
	 * @param JdPayConfig 微信支付配置
	 * @return {WxPayApiConfig} 微信支付配置
	 */
	public static JdPayConfig putApiConfig(JdPayConfig JdPayConfig) {
		if (CFG_MAP.size() == 0) {
			CFG_MAP.put(DEFAULT_CFG_KEY, JdPayConfig);
		}
		return CFG_MAP.put(JdPayConfig.getAppId(), JdPayConfig);
	}

	public static JdPayConfig setThreadLocalJdPayConfig(JdPayConfig JdPayConfig) {
		if (StrUtil.isNotEmpty(JdPayConfig.getAppId())) {
			setThreadLocalAppId(JdPayConfig.getAppId());
		}
		return putApiConfig(JdPayConfig);
	}

	public static JdPayConfig removeApiConfig(JdPayConfig JdPayConfig) {
		return removeApiConfig(JdPayConfig.getAppId());
	}

	public static JdPayConfig removeApiConfig(String appId) {
		return CFG_MAP.remove(appId);
	}

	public static void setThreadLocalAppId(String appId) {
		if (StrUtil.isEmpty(appId)) {
			appId = CFG_MAP.get(DEFAULT_CFG_KEY).getAppId();
		}
		TL.set(appId);
	}

	public static void removeThreadLocalAppId() {
		TL.remove();
	}

	public static String getAppId() {
		String appId = TL.get();
		if (StrUtil.isEmpty(appId)) {
			appId = CFG_MAP.get(DEFAULT_CFG_KEY).getAppId();
		}
		return appId;
	}

	public static JdPayConfig getJdPayConfig() {
		String appId = getAppId();
		return getApiConfig(appId);
	}

	public static JdPayConfig getApiConfig(String appId) {
		JdPayConfig cfg = CFG_MAP.get(appId);
		if (cfg == null) {
			throw new IllegalStateException("需事先调用 JdPayConfigKit.putApiConfig(JdPayConfig) 将 appId 对应的 JdPayConfig 对象存入，才可以使用 JdPayConfigKit.getJdPayConfig() 的系列方法");
		}
		return cfg;
	}
}
