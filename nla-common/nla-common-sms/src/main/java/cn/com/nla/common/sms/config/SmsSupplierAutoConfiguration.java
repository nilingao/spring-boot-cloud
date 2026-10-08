package cn.com.nla.common.sms.config;

import cn.com.nla.common.sms.supplier.dxw.DxwFactory;
import cn.com.nla.common.sms.supplier.swlh.SwlhFactory;
import cn.com.nla.common.sms.supplier.wnd.WndFactory;
import cn.com.nla.common.sms.supplier.wyyd.WyydFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.provider.factory.ProviderFactoryHolder;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * 自定义短信供应商注册配置。
 * <p>
 * 将 4 个 SPI 自定义渠道工厂（dxw/wnd/swlh/wyyd）注册进 sms4j 的 {@link ProviderFactoryHolder}，
 * 供表驱动动态创建 {@code SmsBlend} 时按 supplier 查找。内置渠道（aliyun/tencent/cloopen 等）
 * 由 sms4j 自身注册，无需在此处理。注册须在 {@code SmsFactory.createSmsBlend(...)} 之前完成。
 * </p>
 *
 * @author TZY
 */
@Slf4j
@AutoConfiguration
public class SmsSupplierAutoConfiguration {

    /**
     * 注册自定义供应商工厂。
     */
    @PostConstruct
    public void registerCustomSuppliers() {
        ProviderFactoryHolder.registerFactory(new DxwFactory());
        ProviderFactoryHolder.registerFactory(new WndFactory());
        ProviderFactoryHolder.registerFactory(new SwlhFactory());
        ProviderFactoryHolder.registerFactory(new WyydFactory());
        log.info("已注册自定义短信供应商工厂: dxw, wnd, swlh, wyyd");
    }
}
