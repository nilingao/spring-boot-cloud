package cn.com.nla.common.freeswitch.model.fs;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;


/**
 * 技能
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class SkillGroupInfo implements Serializable {
    /**
     * PK
     */
    private Long id;

    /**
     * 企业ID
     */
    private Long companyId;

    /**
     * 技能组优先级
     */
    private Integer levelValue;

    /**
     * 技能ID
     */
    private Long skillId;

    /**
     * 技能组ID
     */
    private Long groupId;

    /**
     * 等级类型(1:全部,2:等于,3:>,4:<,5:介于)
     */
    private Integer rankType;

    /**
     * 介于的开始值
     */
    private Integer rankValueStart;

    /**
     * 等级值
     */
    private Integer rankValue;

    /**
     * 匹配规则(1:低到高,2:高到低)
     */
    private Integer matchType;

    /**
     * 占用率
     */
    private Integer shareValue;

    /**
     * 状态
     */
    private Integer status;
}
