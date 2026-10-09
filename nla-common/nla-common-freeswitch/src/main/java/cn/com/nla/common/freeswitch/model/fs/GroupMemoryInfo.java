package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 坐席与客户记忆表
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class GroupMemoryInfo implements Serializable {

    /**
     * PK
     */
    private Long id;

    /**
     * 企业ID
     */
    private Long companyId;

    /**
     * 坐席
     */
    private String agentKey;

    /**
     * 技能组ID
     */
    private Long groupId;

    /**
     * 客户电话
     */
    private String phone;

    /**
     * 状态
     */
    private Integer status;


}
