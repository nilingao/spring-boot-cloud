package cn.com.nla.common.sms.supplier.wyyd;

import org.dromara.sms4j.provider.factory.AbstractProviderFactory;

/**
 * 网易易盾（wyyd）供应商工厂。
 *
 * @author TZY
 */
public class WyydFactory extends AbstractProviderFactory<WyydSmsImpl, WyydConfig> {

    @Override
    public WyydSmsImpl createSms(WyydConfig config) {
        return new WyydSmsImpl(config);
    }

    @Override
    public String getSupplier() {
        return WyydConfig.SUPPLIER;
    }
}
