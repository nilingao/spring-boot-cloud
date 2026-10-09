package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompanyConferenceInfo  implements Serializable {

    /**
     * 企业id
     */
    private String companyId;

    /**
     * 会议室名
     */
    private String name;

    /**
     * 会议室号码
     */
    private String code;

    /**
     * 会议室号码
     */
    private String password;

    /**
     * 使用状态(1.未使用 1.使用中)
     */
    private Integer status;

    /**
     * 启用状态(0.禁用 1.启用)
     */
    private Integer enable;
}
