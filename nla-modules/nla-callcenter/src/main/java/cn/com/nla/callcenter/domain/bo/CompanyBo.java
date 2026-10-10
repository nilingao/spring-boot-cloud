package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.Company;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** 企业信息表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = Company.class, reverseConvertGenerate = false)
public class CompanyBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 名称。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 简称。 */
    @Size(max = 255, message = "companyCode长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String companyCode;

    /** 联系人。 */
    @Size(max = 255, message = "contact长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String contact;

    /** 电话。 */
    @Size(max = 255, message = "phone长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String phone;

    /** 金额，精确小数。 */
    @Digits(integer = 15, fraction = 4, message = "金额最多15位整数及4位小数", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal balance;

    /** 1:呼出计费,2:呼入计费,3:双向计费,0:全免费。 */
    @Min(value = 0, message = "billType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer billType;

    /** 0:预付费;1:后付费。 */
    @Min(value = 0, message = "payType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer payType;

    /** 隐藏客户号码(0:不隐藏;1:隐藏)。 */
    @Min(value = 0, message = "hiddenCustomer不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer hiddenCustomer;

    /** 坐席密码等级。 */
    @Min(value = 0, message = "secretType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer secretType;

    /** 验证秘钥。 */
    @Size(max = 32, message = "secretKey长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String secretKey;

    /** IVR通道数。 */
    @Min(value = 0, message = "ivrLimit不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer ivrLimit;

    /** 开通坐席。 */
    @Min(value = 0, message = "agentLimit不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer agentLimit;

    /** 开通技能组。 */
    @Min(value = 0, message = "groupLimit不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer groupLimit;

    /** 单技能组中坐席上限。 */
    @Min(value = 0, message = "groupAgentLimit不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer groupAgentLimit;

    /** 录音保留天数。 */
    @Min(value = 0, message = "recordStorage不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer recordStorage;

    /** 话单回调通知。 */
    @Size(max = 255, message = "notifyUrl长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String notifyUrl;

    /** 状态(0:禁用企业,1:免费企业;2:试用企业,3:付费企业)。 */
    @Min(value = 0, message = "status不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer status;

    /** 开通会议室上限。 */
    @Min(value = 0, message = "conferenceLimit不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer conferenceLimit;
}
