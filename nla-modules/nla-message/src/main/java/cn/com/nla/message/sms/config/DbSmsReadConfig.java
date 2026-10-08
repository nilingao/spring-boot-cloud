package cn.com.nla.message.sms.config;

import cn.com.nla.common.sms.supplier.dxw.DxwConfig;
import cn.com.nla.common.sms.supplier.swlh.SwlhConfig;
import cn.com.nla.common.sms.supplier.wnd.WndConfig;
import cn.com.nla.common.sms.supplier.wyyd.WyydConfig;
import cn.com.nla.message.domain.SmsConfig;
import cn.com.nla.message.mapper.SmsConfigMapper;
import cn.com.nla.message.sms.SmsChannelEnum;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.aliyun.config.AlibabaConfig;
import org.dromara.sms4j.cloopen.config.CloopenConfig;
import org.dromara.sms4j.core.datainterface.SmsReadConfig;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.dromara.sms4j.tencent.config.TencentConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 表驱动配置读取器：从 {@code sms_sms_config} 读取启用账号，按 {@code smsType} 组装成 sms4j 的
 * {@link BaseConfig}，交由 {@code SmsFactory} 动态创建 {@code SmsBlend}。
 * <p>
 * 这是「不在 yml 配置、完全由表中配置驱动」的核心实现：改表即改短信通道行为。
 * configId 统一采用 {@code sms_sms_config.id} 的字符串形式，保证列表读取与单条读取一致。
 * </p>
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DbSmsReadConfig implements SmsReadConfig {

    /**
     * 启用状态
     */
    private static final int ACTIVE = 1;

    private final SmsConfigMapper smsConfigMapper;

    @Override
    public List<BaseConfig> getSupplierConfigList() {
        List<SmsConfig> rows = smsConfigMapper.selectList(
            new LambdaQueryWrapper<SmsConfig>().eq(SmsConfig::getIsActive, ACTIVE));
        List<BaseConfig> list = new ArrayList<>(rows.size());
        for (SmsConfig row : rows) {
            BaseConfig config = buildConfig(row);
            if (config != null) {
                list.add(config);
            }
        }
        return list;
    }

    @Override
    public BaseConfig getSupplierConfig(String configId) {
        if (StrUtil.isBlank(configId) || !StrUtil.isNumeric(configId)) {
            return null;
        }
        SmsConfig row = smsConfigMapper.selectById(Long.valueOf(configId));
        return row == null ? null : buildConfig(row);
    }

    /**
     * 将一行渠道配置组装为对应供应商的 {@link BaseConfig}。
     *
     * @param row 渠道配置
     * @return sms4j 配置对象；未知或已废弃渠道返回 {@code null}
     */
    private BaseConfig buildConfig(SmsConfig row) {
        SmsChannelEnum channel = SmsChannelEnum.of(row.getSmsType());
        if (channel == null) {
            log.warn("跳过未知或已废弃的短信渠道: configId={}, smsType={}", row.getId(), row.getSmsType());
            return null;
        }
        BaseConfig config = switch (channel) {
            case DXW -> {
                DxwConfig c = new DxwConfig();
                c.setSignPlace(row.getSignPlace());
                yield c;
            }
            case WND -> {
                WndConfig c = new WndConfig();
                c.setSignPlace(row.getSignPlace());
                yield c;
            }
            case SWLH -> {
                SwlhConfig c = new SwlhConfig();
                c.setSignPlace(row.getSignPlace());
                yield c;
            }
            case WYYD -> {
                WyydConfig c = new WyydConfig();
                if (StrUtil.isNotBlank(row.getAppId())) {
                    c.setBusinessId(row.getAppId());
                }
                yield c;
            }
            case ALIBABA -> new AlibabaConfig();
            case CLOOPEN -> {
                CloopenConfig c = new CloopenConfig();
                c.setSdkAppId(row.getAppId());
                yield c;
            }
            case TENCENT -> {
                TencentConfig c = new TencentConfig();
                c.setSdkAppId(row.getAppId());
                yield c;
            }
        };
        // 公共字段：account→accessKeyId，password→accessKeySecret，sign→signature
        config.setConfigId(String.valueOf(row.getId()));
        config.setAccessKeyId(row.getAccount());
        config.setAccessKeySecret(row.getPassword());
        config.setSignature(row.getSign());
        config.setWeight(1);
        return config;
    }
}
