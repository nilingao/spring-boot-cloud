package cn.com.nla.common.video.core.sip.properties;

import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

/**
 * 信令默认配置
 */
@Slf4j
public class DefaultSipProperties {
    /**
     * 创建信令默认配置
     * @param ip
     * @param sipLog 是否开启日志
     * @return
     */
    public static Properties getProperties(String ip, boolean sipLog) {
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "NLA_VIDEO_GB28181_SIP");
        properties.setProperty("javax.sip.IP_ADDRESS", ip);
        // 关闭自动会话
        properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "off");
        /**
         * 完整配置参考 gov.nist.javax.sip.SipStackImpl，需要下载源码
         * gov/nist/javax/sip/SipStackImpl.class
         * sip消息的解析在 gov.nist.javax.sip.stack.UDPMessageChannel的processIncomingDataPacket方法
         */
        // 接收所有notify请求，即使没有订阅
        properties.setProperty("gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY", "true");
        properties.setProperty("gov.nist.javax.sip.AUTOMATIC_DIALOG_ERROR_HANDLING", "false");
        properties.setProperty("gov.nist.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true");
        // 为_NULL _对话框传递_终止的_事件
        properties.setProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", "true");
        // 是否自动计算content length的实际长度，默认不计算
        properties.setProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "true");
        // 会话清理策略
        properties.setProperty("gov.nist.javax.sip.RELEASE_REFERENCES_STRATEGY", "Normal");
        // 处理由该服务器处理的基于底层TCP的保持生存超时
        properties.setProperty("gov.nist.javax.sip.RELIABLE_CONNECTION_KEEP_ALIVE_TIMEOUT", "60");
        // 获取实际内容长度，不使用header中的长度信息
        properties.setProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "true");
        // 线程可重入
        properties.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER", "true");
        // 定义应用程序打算多久审计一次 SIP 堆栈，了解其内部线程的健康状况（该属性指定连续审计之间的时间（以毫秒为单位））
        properties.setProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS", "30000");
        //开启重试机制
        properties.setProperty("javax.sip.RETRANSMISSION_FILTER", "true");
        /**
         * sip_server_log.log 和 sip_debug_log.log ERROR, INFO, WARNING, OFF, DEBUG, TRACE
         */
        if (sipLog) {
            properties.setProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT", "false");
            log.info("[SIP日志]已开启");
        }else {
            log.info("[SIP日志]已关闭");
        }
        return properties;
    }
}
