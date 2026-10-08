package cn.com.nla.common.sms.supplier.wnd;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.sms4j.provider.config.BaseConfig;

/**
 * 维纳多（wnd）渠道配置。
 * <p>
 * account→accessKeyId，password→accessKeySecret（同时作为 AES 密钥），sign→signature，signPlace→signPlace。
 * </p>
 *
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WndConfig extends BaseConfig {

    /**
     * 供应商标识
     */
    public static final String SUPPLIER = "wnd";

    /**
     * 服务地址
     */
    private String serverUrl = "http://yl.mobsms.net/send/sendAnna.aspx";

    /**
     * 签名位置（1左边 2右边）
     */
    private Integer signPlace;

    @Override
    public String getSupplier() {
        return SUPPLIER;
    }
}
