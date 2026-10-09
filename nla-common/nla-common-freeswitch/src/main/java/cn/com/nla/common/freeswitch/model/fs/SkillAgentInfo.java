package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 坐席技能表
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class SkillAgentInfo implements Serializable {
    /**
     * PK
     */
    private Long id;

    /**
     * 企业id
     */
    private Long companyId;

    /**
     * 技能id
     */
    private Long skillId;

    /**
     * 坐席id
     */
    private Long agentId;

    /**
     * 范围
     */
    private Integer rankValue;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 坐席
     */
    private String agentKey;
}