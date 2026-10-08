package cn.com.nla.message.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.message.domain.SmsConfig;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 短信渠道配置业务对象 sms_sms_config
 *
 * @author TZY
 */
@Data
@AutoMapper(target = SmsConfig.class, reverseConvertGenerate = false)
public class SmsConfigBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 渠道类型（1短信网 3维纳多 4商务领航 5阿里云大于 6网易易盾 7云通讯 8腾讯云）
     */
    @NotNull(message = "渠道类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer smsType;

    /**
     * 配置名称
     */
    @NotBlank(message = "配置名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String configName;

    /**
     * 账号
     */
    @NotBlank(message = "账号不能为空", groups = {AddGroup.class, EditGroup.class})
    private String account;

    /**
     * 密码/密钥（只写不读；新增必填，编辑留空则保持原值）
     */
    @NotBlank(message = "密码不能为空", groups = {AddGroup.class})
    private String password;

    /**
     * 应用ID（腾讯云 sdkAppId / 容联云通讯 appId）
     */
    private String appId;

    /**
     * 余额
     */
    private String balance;

    /**
     * 是否启用（0停用 1启用）
     */
    @NotNull(message = "启用状态不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer isActive;

    /**
     * 签名
     */
    private String sign;

    /**
     * 签名位置（1左边 2右边）
     */
    private Integer signPlace;

    /**
     * 查询参数
     */
    private Map<String, Object> params = new HashMap<>();

}
