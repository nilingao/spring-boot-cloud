package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 坐席自定义策略表
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class GroupStrategyExpInfo implements Serializable {
    /**
     * PK
     */
    private Long id;

    /**
     * 企业ID
     */
    private Long companyId;

    /**
     * 技能组id
     */
    private Long groupId;

    /**
     * 自定义值
     */
    private String strategyKey;

    /**
     * 百分百
     */
    private Integer strategyPresent;

    /**
     * 类型
     */
    private Integer strategyType;

    /**
     * 状态
     */
    private Integer status;
}