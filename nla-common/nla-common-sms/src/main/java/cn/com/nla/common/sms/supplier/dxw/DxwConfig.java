package cn.com.nla.common.sms.supplier.dxw;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.sms4j.provider.config.BaseConfig;

/**
 * 短信网（dxw）渠道配置。
 * <p>
 * 表字段映射：account→accessKeyId，password→accessKeySecret，sign→signature，signPlace→signPlace。
 * </p>
 *
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DxwConfig extends BaseConfig {

    /**
     * 供应商标识
     */
    public static final String SUPPLIER = "dxw";

    /**
     * 服务地址
     */
    private String serverUrl = "http://web.duanxinwang.cc/asmx/smsservice.aspx";

    /**
     * 签名位置（1左边 2右边）
     */
    private Integer signPlace;

    @Override
    public String getSupplier() {
        return SUPPLIER;
    }
}
