package cn.com.nla.common.pay.jdpay.config;

import lombok.*;

import java.io.Serializable;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JdPayConfig implements Serializable {
	private static final long serialVersionUID = -9044503427692786302L;

	private String appId;
	private String mchId;
	private String rsaPrivateKey;
	private String rsaPublicKey;
	private String desKey;
	private String domain;
	private String certPath;
}
