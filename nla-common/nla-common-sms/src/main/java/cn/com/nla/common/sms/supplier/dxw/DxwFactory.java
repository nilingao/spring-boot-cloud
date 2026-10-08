package cn.com.nla.common.sms.supplier.dxw;

import org.dromara.sms4j.provider.factory.AbstractProviderFactory;

/**
 * 短信网（dxw）供应商工厂。
 *
 * @author TZY
 */
public class DxwFactory extends AbstractProviderFactory<DxwSmsImpl, DxwConfig> {

    @Override
    public DxwSmsImpl createSms(DxwConfig config) {
        return new DxwSmsImpl(config);
    }

    @Override
    public String getSupplier() {
        return DxwConfig.SUPPLIER;
    }
}
