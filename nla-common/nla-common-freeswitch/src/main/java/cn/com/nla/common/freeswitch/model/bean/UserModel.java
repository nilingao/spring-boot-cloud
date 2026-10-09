package cn.com.nla.common.freeswitch.model.bean;

import cn.com.nla.common.freeswitch.model.BeanModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 呼叫账户管理对象
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class UserModel extends BeanModel {
    /**  */
    private String id;

    /**  */
    private String name;

    /**  */
    private String number;

    /**  */
    private String domain;

    /**  */
    private String password;

    /** 录音（1正常 0停用） */
    private Integer audio;

    /** 录像（1正常 0停用） */
    private Integer video;

    /**  */
    private Integer level;
}
