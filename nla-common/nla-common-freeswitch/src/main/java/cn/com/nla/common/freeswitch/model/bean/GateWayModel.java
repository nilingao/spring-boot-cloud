package cn.com.nla.common.freeswitch.model.bean;

import cn.com.nla.common.freeswitch.model.BeanModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 网关中继信息
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class GateWayModel extends BeanModel {

    /** 服务器地址 */
    private String name;

    /**关联路由id**/
    private String routeId;

    /** 服务器地址 */
    private String realm;

    /** 是否注册 */
    private Integer register;

    /** 传输类型 */
    private String transport;

    /** 重连间隔（秒） */
    private Long retrySeconds;

    /** 账户 */
    private String username;

    /** 密码 */
    private String password;

    /**是否选中0:未选中，1：已选中**/
    private int selected;


}