package cn.com.nla.common.freeswitch.vo.sip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 平台发送注册/注销消息时缓存此消息
 * @author lin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformRegisterInfo implements Serializable {
    /**
     * 平台Id
     */
    private String agentKey;

    /**
     * 是否时注册，false为注销
     */
    private boolean register;
}
