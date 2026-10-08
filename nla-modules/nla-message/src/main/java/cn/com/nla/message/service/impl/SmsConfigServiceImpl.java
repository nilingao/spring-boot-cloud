package cn.com.nla.message.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.message.domain.MobileMessageTemplate;
import cn.com.nla.message.domain.SmsConfig;
import cn.com.nla.message.domain.bo.SmsConfigBo;
import cn.com.nla.message.domain.vo.SmsConfigVo;
import cn.com.nla.message.mapper.MobileMessageTemplateMapper;
import cn.com.nla.message.mapper.SmsConfigMapper;
import cn.com.nla.message.service.ISmsConfigService;
import cn.com.nla.message.sms.core.SmsChannelManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 短信渠道配置Service业务层处理
 * <p>
 * 写操作成功后按启用状态定向刷新 sms4j 通道（启用重载、停用注销）；刷新须在 DB 写之后，
 * 因 {@code DbSmsReadConfig} 实时读库，可拿到最新配置。
 * </p>
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SmsConfigServiceImpl implements ISmsConfigService {

    /**
     * 启用状态
     */
    private static final Integer ACTIVE = 1;

    private final SmsConfigMapper configMapper;
    private final MobileMessageTemplateMapper templateMapper;
    private final SmsChannelManager smsChannelManager;

    /**
     * 查询短信渠道配置
     *
     * @param id 主键
     * @return 短信渠道配置
     */
    @Override
    public SmsConfigVo queryById(Long id) {
        return configMapper.selectVoById(id);
    }

    /**
     * 分页查询短信渠道配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 短信渠道配置分页列表
     */
    @Override
    public PageResult<SmsConfigVo> queryPageList(SmsConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SmsConfig> lqw = buildQueryWrapper(bo);
        Page<SmsConfigVo> result = configMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的短信渠道配置列表
     *
     * @param bo 查询条件
     * @return 短信渠道配置列表
     */
    @Override
    public List<SmsConfigVo> queryList(SmsConfigBo bo) {
        LambdaQueryWrapper<SmsConfig> lqw = buildQueryWrapper(bo);
        return configMapper.selectVoList(lqw);
    }

    /**
     * 构造短信渠道配置查询条件。
     *
     * @param bo 筛选条件
     * @return 包含渠道类型、名称、启用状态与创建时间区间的查询包装器
     */
    private LambdaQueryWrapper<SmsConfig> buildQueryWrapper(SmsConfigBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SmsConfig.class)
            .eqIfPresent(SmsConfig::getSmsType, bo.getSmsType())
            .likeIfText(SmsConfig::getConfigName, bo.getConfigName())
            .eqIfPresent(SmsConfig::getIsActive, bo.getIsActive())
            .betweenParams(SmsConfig::getCreateTime, params, "beginTime", "endTime")
            .orderByDesc(SmsConfig::getId)
            .build();
    }

    /**
     * 新增短信渠道配置
     *
     * @param bo 短信渠道配置
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(SmsConfigBo bo) {
        SmsConfig add = MapstructUtils.convert(bo, SmsConfig.class);
        validEntityBeforeSave(add);
        boolean flag = configMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
            refreshChannel(add);
        }
        return flag;
    }

    /**
     * 修改短信渠道配置
     *
     * @param bo 短信渠道配置
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(SmsConfigBo bo) {
        SmsConfig update = MapstructUtils.convert(bo, SmsConfig.class);
        // password 只写不读：编辑留空则置 null，依赖 MyBatis-Plus NOT_NULL 策略保持原值
        if (StringUtils.isBlank(update.getPassword())) {
            update.setPassword(null);
        }
        validEntityBeforeSave(update);
        boolean flag = configMapper.updateById(update) > 0;
        if (flag) {
            refreshChannel(update);
        }
        return flag;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 渠道配置实体
     */
    private void validEntityBeforeSave(SmsConfig entity) {
        // 可在此扩展通用业务校验
    }

    /**
     * 校验并批量删除短信渠道配置信息
     * <p>
     * 删除前做引用校验：被短信模板引用的渠道配置不允许删除；删除成功后注销对应通道。
     * </p>
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            boolean referenced = templateMapper.exists(
                new LambdaQueryWrapper<MobileMessageTemplate>()
                    .in(MobileMessageTemplate::getConfigId, ids));
            if (referenced) {
                throw new ServiceException("渠道配置已被短信模板引用，请先删除相关模板");
            }
        }
        boolean flag = configMapper.deleteByIds(ids) > 0;
        if (flag) {
            ids.forEach(smsChannelManager::remove);
        }
        return flag;
    }

    /**
     * 按启用状态定向刷新通道：启用则重载注册，停用则注销。
     * <p>
     * 须在 DB 写成功后调用，避免事务未提交即刷新读到旧值。
     * </p>
     *
     * @param config 渠道配置
     */
    private void refreshChannel(SmsConfig config) {
        if (config == null || config.getId() == null) {
            return;
        }
        if (ACTIVE.equals(config.getIsActive())) {
            smsChannelManager.refresh(config.getId());
        } else {
            smsChannelManager.remove(config.getId());
        }
    }

}
