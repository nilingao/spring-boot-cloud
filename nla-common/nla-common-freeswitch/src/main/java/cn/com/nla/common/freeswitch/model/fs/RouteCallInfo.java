package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RouteCallInfo {

    /**
     * PK
     */
    private Long id;

    /**
     * 创建时间
     */
    private Long cts;

    /**
     * 修改时间
     */
    private Long uts;

    /**
     * 所属企业
     */
    private Long companyId;

    /**
     * 所有路由组
     */
    private Long routeGroupId;

    /**
     * 字冠号码
     */
    private String routeNum;

    /**
     * 最长
     */
    private Integer numMax;

    /**
     * 最短
     */
    private Integer numMin;

    /**
     * 主叫替换规则
     */
    private Integer callerChange;

    /**
     * 替换号码
     */
    private String callerChangeNum;

    /**
     * 被叫替换规则
     */
    private Integer calledChange;

    /**
     * 替换号码
     */
    private String calledChangeNum;

    /**
     * 状态
     */
    private Integer status;
}
