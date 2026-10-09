package cn.com.nla.common.freeswitch.model.message;

import cn.com.nla.common.freeswitch.model.MessageModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 发起呼叫相关参数
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class MakeCallModel implements MessageModel {
    /**
     * 设备uuid
     */
    private String deviceId;
    /**
     * 设备uuid
     */
    private String callId;
    /**
     * sip中 sdp消息
     */
    private String sdp;
    /**
     * 主叫
     */
    private String display;

    /**
     * 被叫
     */
    private String called;
    /**
     * 被叫显号
     */
    private String calledDisplay;
    /**
     * 超时时长（秒）
     */
    private Integer originateTimeout = 15;
    /**
     * sip请求头
     */
    private List<String> sipHeaderList;
    /**
     * 网关信息
     */
    private RouteGatewayModel gatewayModel;

    @Data
    @SuperBuilder(toBuilder = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RouteGatewayModel implements MessageModel {
        /**
         * 媒体IP
         */
        private String mediaHost;
        /**
         * 媒体端口
         */
        private Integer mediaPort;
        /**
         * 主叫号码前缀
         */
        private String callerPrefix;
        /**
         * 被叫号码前缀
         */
        private String calledPrefix;
        /**
         * 媒体拨号计划文件
         */
        private String profile;
        /**
         * sip头
         */
        private List<String> sipHeaderList;

    }

}
