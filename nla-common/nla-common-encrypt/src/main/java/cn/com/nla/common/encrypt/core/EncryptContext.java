package cn.com.nla.common.encrypt.core;

import lombok.Data;
import cn.com.nla.common.encrypt.enums.AlgorithmType;
import cn.com.nla.common.encrypt.enums.EncodeType;

/**
 * 加密上下文 用于encryptor传递必要的参数。
 *
 * @author TZY
 * @version 4.6.0
 */
@Data
public class EncryptContext {

    /**
     * 默认算法
     */
    private AlgorithmType algorithm;

    /**
     * 安全秘钥
     */
    private String password;

    /**
     * 公钥
     */
    private String publicKey;

    /**
     * 私钥
     */
    private String privateKey;

    /**
     * 编码方式，base64/hex
     */
    private EncodeType encode;

}
