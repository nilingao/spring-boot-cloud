package cn.com.nla.common.sms.supplier.swlh;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.sms4j.provider.config.BaseConfig;

/**
 * 商务领航（swlh）渠道配置。
 * <p>
 * account 以 {@code |} 分割为 epid 与 User_Name；表字段映射同 dxw。
 * </p>
 *
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SwlhConfig extends BaseConfig {

    /**
     * 供应商标识
     */
    public static final String SUPPLIER = "swlh";

    /**
     * 服务地址
     */
    private String serverUrl = "http://access.xx95.net:8886/Connect_Service.asmx/SendSmsEx";

    /**
     * 签名位置（1左边 2右边）
     */
    private Integer signPlace;

    @Override
    public String getSupplier() {
        return SUPPLIER;
    }
}
