package cn.com.nla.message.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 短信渠道配置对象 sms_sms_config
 * <p>
 * 表驱动核心：每行是一个短信通道账号，运行时由 DbSmsReadConfig 读取 isActive=1 的记录，
 * 按 smsType 组装成 sms4j 的 BaseConfig 注册到 SmsFactory；改表即改行为，不依赖 yml。
 * </p>
 *
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sms_sms_config")
public class SmsConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 渠道类型（1短信网 3维纳多 4商务领航 5阿里云大于 6网易易盾 7云通讯 8腾讯云；2创蓝网已废弃）
     */
    private Integer smsType;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 账号
     */
    private String account;

    /**
     * 密码/密钥（只写不读，不返回前端）
     */
    private String password;

    /**
     * 应用ID（腾讯云 sdkAppId / 容联云通讯 appId；表驱动，替代旧硬编码常量）
     */
    private String appId;

    /**
     * 余额
     */
    private String balance;

    /**
     * 是否启用（0停用 1启用）
     */
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
     * 删除标志（0存在 1删除）
     */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;

}
