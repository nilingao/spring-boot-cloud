package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 自定义溢出策略优先级
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class OverflowExpInfo implements Serializable {

    /**
     * PK
     */
    private Long id;

    /**
     * 企业ID
     */
    private Long companyId;

    /**
     * 溢出策略ID
     */
    private Long overflowId;

    /**
     * 自定义值
     */
    private String expKey;

    /**
     * 权重
     */
    private Integer rate;

    /**
     * 状态
     */
    private Integer status;
}
