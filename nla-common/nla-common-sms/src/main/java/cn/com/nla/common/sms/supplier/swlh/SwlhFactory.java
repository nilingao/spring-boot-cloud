package cn.com.nla.common.sms.supplier.swlh;

import org.dromara.sms4j.provider.factory.AbstractProviderFactory;

/**
 * 商务领航（swlh）供应商工厂。
 *
 * @author TZY
 */
public class SwlhFactory extends AbstractProviderFactory<SwlhSmsImpl, SwlhConfig> {

    @Override
    public SwlhSmsImpl createSms(SwlhConfig config) {
        return new SwlhSmsImpl(config);
    }

    @Override
    public String getSupplier() {
        return SwlhConfig.SUPPLIER;
    }
}
