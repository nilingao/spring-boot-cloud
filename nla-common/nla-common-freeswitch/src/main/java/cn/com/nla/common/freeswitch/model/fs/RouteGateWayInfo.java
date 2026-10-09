package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RouteGateWayInfo  implements Serializable {
    /**
     * PK
     */
    private Long id;

    /**
     * 号码
     */
    private String name;

    /**
     * 媒体地址
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
     * sip头1
     */
    private String sipHeader1;

    /**
     * sip头2
     */
    private String sipHeader2;

    /**
     * sip头3
     */
    private String sipHeader3;

    /**
     * 状态
     */
    private Integer status;
}
