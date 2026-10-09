package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 按键导航表
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class VdnDtmfInfo implements Serializable {
    /**
     * PK
     */
    private Long id;
    /**
     * 企业ID
     */
    private Long companyId;

    /**
     * 按键导航ID
     */
    private Long navigateId;

    /**
     * 按键
     */
    private String dtmf;

    /**
     * 路由类型(1:技能组,2:IVR,3:路由字码,4:坐席分机,5:挂机)
     */
    private Integer routeType;

    /**
     * 路由值
     */
    private Long routeValue;

    /**
     * 状态
     */
    private Integer status;
}