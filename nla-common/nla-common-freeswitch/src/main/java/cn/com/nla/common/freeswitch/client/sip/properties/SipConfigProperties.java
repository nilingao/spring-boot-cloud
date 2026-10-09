package cn.com.nla.common.freeswitch.client.sip.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 信令服务参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "sip") // 配置文件的前缀
public class SipConfigProperties {
	/**
	 * 信令服务器名称注册naocs中用
	 */
	private String name = "fs-sip-server";
	/**
	 * 信令服务器国标编号
	 */
	private String id;
	/**
	 * 信令服务器ip //没有服务ip时取nacos 中ip
	 */
	private String ip;
	/**
	 * 信令服务器端口
	 */
	private Integer port;
	/**
	 * 连接时长
	 */
	private int expires = 300;
	/**
	 * 信令区域编号
	 */
	private String domain;

	/**
	 * 信令服务器链接密码
	 */
	private String password="";

}