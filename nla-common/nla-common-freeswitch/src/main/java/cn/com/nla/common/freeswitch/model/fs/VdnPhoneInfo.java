package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 路由号码表
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class VdnPhoneInfo implements Serializable {

    private Long id;

    /**
     * 企业ID
     */
    private String companyId;

    /**
     * 路由码
     */
    private Long vdnId;

    /**
     * 特服号
     */
    private String phone;

    /**
     * 状态
     */
    private Integer status;

}