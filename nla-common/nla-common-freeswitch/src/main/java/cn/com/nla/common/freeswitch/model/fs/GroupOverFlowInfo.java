package cn.com.nla.common.freeswitch.model.fs;

import cn.com.nla.common.freeswitch.client.fs.LineupStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 技能组溢出策略
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class GroupOverFlowInfo  implements Serializable {

    /**
     * PK
     */
    private Long id;

    /**
     * 企业id
     */
    private Long companyId;

    /**
     * 名称
     */
    private String name;

    /**
     * 1:排队,2:溢出,3:挂机
     */
    private Integer handleType;

    /**
     * 排队方式(1:先进先出,2:vip,3:自定义)
     */
    private Integer busyType;

    /**
     * 排队超时时间
     */
    private Integer queueTimeout;

    /**
     * 排队超时(1:溢出,2:挂机)
     */
    private Integer busyTimeoutType;

    /**
     * 溢出(1:group,2:ivr,3:vdn)
     */
    private Integer overflowType;

    /**
     * 溢出值
     */
    private String overflowValue;

    /**
     * 自定义排队表达式
     */
    private String lineupExpression;

    /**
     * 前置条件
     */
    protected List<OverflowFrontInfo> overflowFronts;


    /**
     * 自定义策略
     */
    private List<OverflowExpInfo> overflowExps;

    /**
     * 技能组ID
     */
    private Long groupId;

    /**
     * 溢出策略id
     */
    private Long overflowId;

    /**
     * 优先级
     */
    private Integer levelValue;

    /**
     * 电话排队策略接口
     */
    private LineupStrategy lineupStrategy;

    /**
     * @param queueSize    队列长度
     * @param maxWaitTime  最大等待时长
     * @param callInAnswer 呼入应答数
     * @param callInTotal  呼入总数
     * @return
     */
    public boolean isAvailable(Integer queueSize, Integer maxWaitTime, Integer callInAnswer, Integer callInTotal) {
        boolean result = false;
        if (overflowFronts == null || overflowFronts.isEmpty()) {
            return result;
        }

        for (OverflowFrontInfo front : overflowFronts) {
            switch (front.getFrontType()) {
                case 1:
                    //1:队列长度;
                    result = compareCondition(queueSize, front);
                    break;

                case 2:
                    // 2:队列等待最大时长;
                    result = compareCondition(maxWaitTime, front);
                    break;

                case 3:
                    // 3:呼损率
                    int persent = 0;
                    if (callInAnswer > 0) {
                        persent = (int) (new BigDecimal(1).subtract(new BigDecimal(callInAnswer).divide(new BigDecimal(callInTotal), 2, RoundingMode.HALF_EVEN)).doubleValue() * 100);
                    }
                    result = compareCondition(persent, front);
                    break;

                default:
                    break;
            }
            if (result) {
                return result;
            }
            continue;
        }
        return result;
    }

    /**
     * 0:全部; 1:小于或等于; 2:等于; 3:大于或等于; 4:大于
     *
     * @param var
     * @param front
     * @return
     */
    private boolean compareCondition(Integer var, OverflowFrontInfo front) {
        boolean result = false;
        switch (front.getCompareCondition()) {
            case 0:
                return true;
            case 1:
                result = front.getRankValue().compareTo(var) >= 0;
                break;
            case 2:
                result = front.getRankValue().compareTo(var) == 0;
                break;
            case 3:
                result = front.getRankValue().compareTo(var) <= 0;
                break;
            case 4:
                result = front.getRankValue().compareTo(var) < 0;
                break;

            default:
                break;
        }
        return result;
    }

}
