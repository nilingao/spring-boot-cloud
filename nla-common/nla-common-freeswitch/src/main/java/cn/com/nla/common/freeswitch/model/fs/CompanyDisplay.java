package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 号码池表
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompanyDisplay implements Serializable {
    /**
     * PK
     */
    private Long id;

    /**
     * 企业id
     */
    private Long companyId;

    /**
     * 号码池
     */
    private String name;

    /**
     * 1:呼入号码,2:主叫显号,3:被叫显号
     */
    private Integer type;

    /**
     * 状态
     */
    private Integer status;
}