package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * 路由网关组
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RouteGroupInfo implements Serializable {

    /**
     * PK
     */
    private Long id;

    /**
     * 网关组
     */
    private String routeGroup;

    /**
     * 状态
     */
    private Integer status;
    /**
     * 网关列表
     */
    private List<RouteGateWayInfo> routeGateWayInfoList;
}
