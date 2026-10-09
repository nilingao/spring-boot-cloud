package cn.com.nla.common.freeswitch.model.fs;

import cn.com.nla.common.freeswitch.client.fs.AgentStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class GroupAgentStrategyInfo implements Serializable {

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
     * 1:内置策略,2:自定义
     */
    private Integer strategyType;

    /**
     * (1最长空闲时间、2最长平均空闲、3最少应答次数、4最少通话时长、5最长话后时长、6轮选、7随机)
     */
    private Integer strategyValue;

    /**
     * 自定义表达式
     */
    private String customExpression;

    /**
     *
     */
    private Integer status;

    /**
     * 坐席策略接口
     */
    private AgentStrategy agentStrategy;

    /**
     * 坐席自定义策略
     */
    private List<GroupStrategyExpInfo> strategyExpList;
}
