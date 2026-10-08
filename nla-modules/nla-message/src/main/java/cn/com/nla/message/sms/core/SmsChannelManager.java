package cn.com.nla.message.sms.core;

import cn.com.nla.message.domain.MobileMessageTemplate;
import cn.com.nla.message.domain.SmsConfig;
import cn.com.nla.message.mapper.MobileMessageTemplateMapper;
import cn.com.nla.message.mapper.SmsConfigMapper;
import cn.com.nla.message.sms.config.DbSmsReadConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 短信通道管理器：负责 sms4j {@code SmsBlend} 的表驱动生命周期与通道选择。
 * <p>
 * 应用就绪后按 {@code sms_sms_config} 中启用的账号动态创建并注册所有通道；配置增改删时由业务层
 * 调用 {@link #refresh}/{@link #remove}/{@link #refreshAll} 做定向刷新（禁止全量重建）。
 * 通道选择规则迁移自旧 {@code SmsConfigService.findList}：账号须「启用」且「配置了该消息类型的模板」，
 * 结果随机打散以做负载均衡，发送失败时由 {@link SmsSendManager} 遍历 failover。
 * </p>
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SmsChannelManager {

    /**
     * 启用状态
     */
    private static final int ACTIVE = 1;

    private final DbSmsReadConfig dbSmsReadConfig;
    private final SmsConfigMapper smsConfigMapper;
    private final MobileMessageTemplateMapper templateMapper;

    /**
     * 应用就绪后初始化所有启用通道。
     * <p>
     * 须晚于自定义供应商工厂注册（{@code SmsSupplierAutoConfiguration} 在 {@code @PostConstruct} 完成），
     * {@link ApplicationReadyEvent} 在所有 Bean 初始化后触发，天然满足顺序。
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initBlends() {
        try {
            SmsFactory.createSmsBlend(dbSmsReadConfig);
            log.info("表驱动短信通道初始化完成，当前已注册 {} 个通道", SmsFactory.getAll().size());
        } catch (Exception e) {
            log.error("表驱动短信通道初始化失败", e);
        }
    }

    /**
     * 全量重载所有通道（批量变更后使用）。
     */
    public void refreshAll() {
        SmsFactory.reloadAll(dbSmsReadConfig);
        log.info("表驱动短信通道已全量重载");
    }

    /**
     * 定向重载单个通道（新增或改为启用后使用）。
     *
     * @param configId 渠道配置主键
     */
    public void refresh(Long configId) {
        if (configId == null) {
            return;
        }
        SmsFactory.reload(String.valueOf(configId), dbSmsReadConfig);
        log.info("表驱动短信通道已重载: configId={}", configId);
    }

    /**
     * 注销单个通道（停用或删除后使用）。
     *
     * @param configId 渠道配置主键
     */
    public void remove(Long configId) {
        if (configId == null) {
            return;
        }
        SmsFactory.unregister(String.valueOf(configId));
        log.info("表驱动短信通道已注销: configId={}", configId);
    }

    /**
     * 取指定消息类型可用的启用账号：账号须启用，且配置了该消息类型的模板。
     * <p>
     * 结果已随机打散，调用方按序遍历即实现「随机负载均衡 + 失败 failover」。
     * </p>
     *
     * @param messageType 消息类型（1登录 2注册 3重置）
     * @return 可用账号列表；无则返回空列表
     */
    public List<SmsConfig> findActiveConfigs(Integer messageType) {
        if (messageType == null) {
            return Collections.emptyList();
        }
        // 1.先取配置了该消息类型模板的账号 id（逻辑删除由 @TableLogic 自动过滤）
        List<MobileMessageTemplate> templates = templateMapper.selectList(
            new LambdaQueryWrapper<MobileMessageTemplate>()
                .select(MobileMessageTemplate::getConfigId)
                .eq(MobileMessageTemplate::getType, messageType));
        Set<Long> configIds = templates.stream()
            .map(MobileMessageTemplate::getConfigId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (configIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 2.再取其中启用的账号
        List<SmsConfig> configs = smsConfigMapper.selectList(
            new LambdaQueryWrapper<SmsConfig>()
                .eq(SmsConfig::getIsActive, ACTIVE)
                .in(SmsConfig::getId, configIds));
        // 随机打散做负载均衡
        Collections.shuffle(configs);
        return configs;
    }

    /**
     * 取指定账号 + 消息类型的最新模板（id 最大者）。
     *
     * @param configId    渠道配置主键
     * @param messageType 消息类型
     * @return 最新模板；无则返回 {@code null}
     */
    public MobileMessageTemplate findLastTemplate(Long configId, Integer messageType) {
        if (configId == null || messageType == null) {
            return null;
        }
        List<MobileMessageTemplate> list = templateMapper.selectList(
            new LambdaQueryWrapper<MobileMessageTemplate>()
                .eq(MobileMessageTemplate::getConfigId, configId)
                .eq(MobileMessageTemplate::getType, messageType)
                .orderByDesc(MobileMessageTemplate::getId)
                .last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
    }
}
