package cn.com.nla.callcenter.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.io.Serial;
import java.math.BigDecimal;

/** 企业信息表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_company")
public class Company extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 名称。 */
    @TableField(value = "`name`")
    private String name;

    /** 简称。 */
    @TableField(value = "`company_code`")
    private String companyCode;

    /** 联系人。 */
    @TableField(value = "`contact`")
    private String contact;

    /** 电话。 */
    @TableField(value = "`phone`")
    @ToString.Exclude
    private String phone;

    /** 金额，精确小数。 */
    @TableField(value = "`balance`")
    private BigDecimal balance;

    /** 1:呼出计费,2:呼入计费,3:双向计费,0:全免费。 */
    @TableField(value = "`bill_type`")
    private Integer billType;

    /** 0:预付费;1:后付费。 */
    @TableField(value = "`pay_type`")
    private Integer payType;

    /** 隐藏客户号码(0:不隐藏;1:隐藏)。 */
    @TableField(value = "`hidden_customer`")
    private Integer hiddenCustomer;

    /** 坐席密码等级。 */
    @TableField(value = "`secret_type`")
    private Integer secretType;

    /** 验证秘钥。 */
    @TableField(value = "`secret_key`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String secretKey;

    /** IVR通道数。 */
    @TableField(value = "`ivr_limit`")
    private Integer ivrLimit;

    /** 开通坐席。 */
    @TableField(value = "`agent_limit`")
    private Integer agentLimit;

    /** 开通技能组。 */
    @TableField(value = "`group_limit`")
    private Integer groupLimit;

    /** 单技能组中坐席上限。 */
    @TableField(value = "`group_agent_limit`")
    private Integer groupAgentLimit;

    /** 录音保留天数。 */
    @TableField(value = "`record_storage`")
    private Integer recordStorage;

    /** 话单回调通知。 */
    @TableField(value = "`notify_url`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String notifyUrl;

    /** 状态(0:禁用企业,1:免费企业;2:试用企业,3:付费企业)。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 开通会议室上限。 */
    @TableField(value = "`conference_limit`")
    private Integer conferenceLimit;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
