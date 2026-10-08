package cn.com.nla.common.sms.supplier.wyyd;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.sms4j.provider.config.BaseConfig;

/**
 * 网易易盾（wyyd）渠道配置。
 * <p>
 * account→accessKeyId（secretId），password→accessKeySecret（secretKey，参与签名）。
 * 模板型渠道，不使用签名位置。注意：网易易盾与 sms4j 内置 netease（云信）不是同一产品。
 * </p>
 *
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WyydConfig extends BaseConfig {

    /**
     * 供应商标识
     */
    public static final String SUPPLIER = "wyyd";

    /**
     * 服务地址
     */
    private String serverUrl = "https://sms.dun.163.com/v2/sendsms";

    /**
     * 业务 ID（易盾按产品业务分配）
     */
    private String businessId = "682dadd1aa62494281fe97f3e2aedb00";

    @Override
    public String getSupplier() {
        return SUPPLIER;
    }
}
