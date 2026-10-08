package cn.com.nla.message.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.message.domain.MobileMessageTemplate;
import cn.com.nla.message.domain.bo.MobileMessageTemplateBo;
import cn.com.nla.message.domain.vo.MobileMessageTemplateVo;
import cn.com.nla.message.mapper.MobileMessageTemplateMapper;
import cn.com.nla.message.service.IMobileMessageTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 短信模板Service业务层处理
 * <p>
 * 模板变更无需刷新 sms4j 通道：发送时由 {@code SmsChannelManager.findLastTemplate} 实时查库取最新模板。
 * </p>
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MobileMessageTemplateServiceImpl implements IMobileMessageTemplateService {

    private final MobileMessageTemplateMapper templateMapper;

    /**
     * 查询短信模板
     *
     * @param id 主键
     * @return 短信模板
     */
    @Override
    public MobileMessageTemplateVo queryById(Long id) {
        return templateMapper.selectVoById(id);
    }

    /**
     * 分页查询短信模板列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 短信模板分页列表
     */
    @Override
    public PageResult<MobileMessageTemplateVo> queryPageList(MobileMessageTemplateBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<MobileMessageTemplate> lqw = buildQueryWrapper(bo);
        Page<MobileMessageTemplateVo> result = templateMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的短信模板列表
     *
     * @param bo 查询条件
     * @return 短信模板列表
     */
    @Override
    public List<MobileMessageTemplateVo> queryList(MobileMessageTemplateBo bo) {
        LambdaQueryWrapper<MobileMessageTemplate> lqw = buildQueryWrapper(bo);
        return templateMapper.selectVoList(lqw);
    }

    /**
     * 构造短信模板查询条件。
     *
     * @param bo 筛选条件
     * @return 包含渠道配置、类型、编号、标题与创建时间区间的查询包装器
     */
    private LambdaQueryWrapper<MobileMessageTemplate> buildQueryWrapper(MobileMessageTemplateBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(MobileMessageTemplate.class)
            .eqIfPresent(MobileMessageTemplate::getConfigId, bo.getConfigId())
            .eqIfPresent(MobileMessageTemplate::getType, bo.getType())
            .likeIfText(MobileMessageTemplate::getCode, bo.getCode())
            .likeIfText(MobileMessageTemplate::getTitle, bo.getTitle())
            .betweenParams(MobileMessageTemplate::getCreateTime, params, "beginTime", "endTime")
            .orderByDesc(MobileMessageTemplate::getId)
            .build();
    }

    /**
     * 新增短信模板
     *
     * @param bo 短信模板
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(MobileMessageTemplateBo bo) {
        MobileMessageTemplate add = MapstructUtils.convert(bo, MobileMessageTemplate.class);
        validEntityBeforeSave(add);
        boolean flag = templateMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改短信模板
     *
     * @param bo 短信模板
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(MobileMessageTemplateBo bo) {
        MobileMessageTemplate update = MapstructUtils.convert(bo, MobileMessageTemplate.class);
        validEntityBeforeSave(update);
        return templateMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 短信模板实体
     */
    private void validEntityBeforeSave(MobileMessageTemplate entity) {
        // 可在此扩展通用业务校验
    }

    /**
     * 校验并批量删除短信模板信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 可在此扩展删除前业务校验
        }
        return templateMapper.deleteByIds(ids) > 0;
    }

}
