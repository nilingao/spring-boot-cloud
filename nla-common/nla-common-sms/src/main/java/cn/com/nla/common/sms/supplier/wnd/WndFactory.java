package cn.com.nla.common.sms.supplier.wnd;

import org.dromara.sms4j.provider.factory.AbstractProviderFactory;

/**
 * 维纳多（wnd）供应商工厂。
 *
 * @author TZY
 */
public class WndFactory extends AbstractProviderFactory<WndSmsImpl, WndConfig> {

    @Override
    public WndSmsImpl createSms(WndConfig config) {
        return new WndSmsImpl(config);
    }

    @Override
    public String getSupplier() {
        return WndConfig.SUPPLIER;
    }
}
