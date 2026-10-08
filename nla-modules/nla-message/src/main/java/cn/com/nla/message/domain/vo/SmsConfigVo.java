package cn.com.nla.message.domain.vo;

import cn.com.nla.common.translation.annotation.Translation;
import cn.com.nla.common.translation.constant.TransConstant;
import cn.com.nla.message.domain.SmsConfig;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 短信渠道配置视图对象 sms_sms_config
 * <p>
 * 安全：password 只写不读，故不在视图对象中返回。
 * </p>
 *
 * @author TZY
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SmsConfig.class)
public class SmsConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 渠道类型
     */
    @ExcelProperty(value = "渠道类型")
    private Integer smsType;

    /**
     * 配置名称
     */
    @ExcelProperty(value = "配置名称")
    private String configName;

    /**
     * 账号
     */
    @ExcelProperty(value = "账号")
    private String account;

    /**
     * 应用ID（腾讯云 sdkAppId / 容联云通讯 appId）
     */
    @ExcelProperty(value = "应用ID")
    private String appId;

    /**
     * 余额
     */
    @ExcelProperty(value = "余额")
    private String balance;

    /**
     * 是否启用（0停用 1启用）
     */
    @ExcelProperty(value = "是否启用")
    private Integer isActive;

    /**
     * 签名
     */
    @ExcelProperty(value = "签名")
    private String sign;

    /**
     * 签名位置（1左边 2右边）
     */
    @ExcelProperty(value = "签名位置")
    private Integer signPlace;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 创建人账号
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "createBy")
    private String createByName;

}
