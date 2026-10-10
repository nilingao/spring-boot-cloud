package cn.com.nla.common.video.basic.common;

/**
 * 信令服务相关参数
 */
public class SipConstant {

    /** 十六进制转换器 */
    public static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static Integer maxForwardsHeader = 70;
    /**
     * 信令默认密码认证方式
     */
    public static final String DEFAULT_ALGORITHM = "MD5";
    /**
     * 信令默认认证头部
     */
    public static final String DEFAULT_SCHEME = "Digest";

}
