package cn.com.nla.common.video.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 信令服务参数
 */
@Data
@ConfigurationProperties(prefix = "video.sip") // 配置文件的前缀
public class SipConfigProperties {
	/**
	 * 信令服务器名称
	 */
	private String name = "video-sip-server";
	/**
	 * SIP local listening address. In Docker this can be 0.0.0.0.
	 */
	private String bindIp;
	/**
	 * SIP address announced in Via/Contact/Call-ID. In Docker this should be the host/public reachable IP.
	 */
	private String advertisedIp;
	/**
	 * 信令服务器端口
	 */
	private Integer port;
	/**
	 * 信令区域编号
	 */
	private String domain;
	/**
	 * 信令服务器国标编号
	 */
	private String id;
	/**
	 * 信令服务器链接密码
	 */
	private String password="123456";

}
