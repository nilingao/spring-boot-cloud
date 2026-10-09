package cn.com.nla.common.freeswitch.model.bean;

import cn.com.nla.common.freeswitch.model.BeanModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 注册信息
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RegisterModel extends BeanModel {
    private String userAccount;
    private String domain;
    private String token;
    private String contact;
    private Float expireTime;
    private String ip;
    private String port;
    private String protocol;
    private String hostName;
}
