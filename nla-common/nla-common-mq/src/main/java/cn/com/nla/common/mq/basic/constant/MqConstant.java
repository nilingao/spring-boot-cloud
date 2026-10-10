package cn.com.nla.common.mq.basic.constant;

/** 旧扫码业务使用的队列、交换机和 routing key，保持 broker 名称。 @author TZY */
public final class MqConstant {
    public static final String DEAD_LETTER_EXCHANGE = "dead_letter_exchange";
    public static final String DEAD_LETTER_ROUTING_KEY = "dead_letter_routing_key";
    public static final String DEAD_LETTER_QUEUE = "dead_letter_queue";
    public static final String QR_EXCHANGE = "qr_exchange";
    public static final String QR_ROUTING_KEY = "qr_routing_key";
    public static final String QR_QUEUE = "qr_queue";
    private MqConstant() { }
}
