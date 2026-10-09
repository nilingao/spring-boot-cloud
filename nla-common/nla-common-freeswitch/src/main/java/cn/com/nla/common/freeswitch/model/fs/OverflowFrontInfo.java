package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 溢出策略前置条件
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class OverflowFrontInfo  implements Serializable {

    /**
     * PK
     */
    private Long id;

    /**
     * 企业ID
     */
    private Long companyId;

    /**
     * 策略ID
     */
    private Long overflowId;

    /**
     * frontType 1:队列长度; 2:队列等待最大时长; 3:呼损率
     */
    private Integer frontType;

    /**
     * 5种条件:
     * 0:全部; 1:小于或等于; 2:等于; 3:大于或等于; 4:大于
     */
    private Integer compareCondition;

    /**
     *
     */
    private Integer rankValueStart;

    /**
     * 符号条件值
     */
    private Integer rankValue;

    /**
     * 状态
     */
    private Integer status;
}
