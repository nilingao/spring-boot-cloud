package cn.com.nla.common.freeswitch.model.bean;

import cn.com.nla.common.freeswitch.model.BeanModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 路由信息
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RouteModel extends BeanModel {

    /** 匹配前缀 */
    private String reg;

    /** 路由类型 */
    private Long type;

    /** 呼入模式 */
    private Long inboundModel;

    /** 中继id */
    private String gatewayIds;

    /** 中继名称 */
    private String gatewayNames;

    /** 主叫前缀新增位数 */
    private String callerPre;

    /** 主叫前缀删除位数 */
    private Long callerPreRemove;

    /** 被叫前缀新增位数 */
    private String calleePre;

    /** 被叫前缀删除位数 */
    private Long calleePreRemove;

    /** DID */
    private String did;

    /** 被叫检测 */
    private String calleeCheck;

    /** 呼出主叫转换号码 */
    private String transform;

    /** 路由名称 */
    private String name;

}
