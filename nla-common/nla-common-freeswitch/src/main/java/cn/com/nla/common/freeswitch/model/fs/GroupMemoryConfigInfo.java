package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 坐席记忆配置
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class GroupMemoryConfigInfo implements Serializable {
    /**
     * PK
     */
    private Long id;

    /**
     * 企业ID
     */
    private Long companyId;

    /**
     * 技能组ID
     */
    private Long groupId;

    /**
     * 匹配成功策略 1:等待记忆坐席 2:超时转其他空闲坐席 3:忙碌转空闲坐席
     */
    private Integer successStrategy;

    /**
     * 匹配成功策略值
     */
    private Long successStrategyValue;

    /**
     * 匹配失败策略:
     * 1:其他空闲坐席
     * 2:其他技能组
     * 3:vdn
     * 4:ivr
     * 5:挂机
     */
    private Integer failStrategy;

    /**
     * 匹配失败策略值
     */
    private String failStrategyValue;

    /**
     * 记忆天数
     */
    private Integer memoryDay;

    /**
     * 呼入覆盖
     */
    private Integer inboundCover;

    /**
     * 外呼覆盖
     */
    private Integer outboundCover;

    /**
     *
     */
    private Integer status;

}
