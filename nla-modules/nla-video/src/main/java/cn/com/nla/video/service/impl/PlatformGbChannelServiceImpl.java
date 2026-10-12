package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.video.domain.PlatformGbChannel;
import cn.com.nla.video.domain.bo.PlatformGbChannelBo;
import cn.com.nla.video.domain.vo.PlatformGbChannelVo;
import cn.com.nla.video.mapper.PlatformGbChannelMapper;
import cn.com.nla.video.service.IPlatformGbChannelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 平台目录与通道级联关系 Service 实现。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PlatformGbChannelServiceImpl implements IPlatformGbChannelService {

    private final PlatformGbChannelMapper platformGbChannelMapper;

    /**
     * 查询级联关系详情。
     *
     * @param id 主键
     * @return 级联关系视图对象
     */
    @Override
    public PlatformGbChannelVo queryById(Long id) {
        return platformGbChannelMapper.selectVoById(id);
    }

    /**
     * 分页查询级联关系列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 级联关系分页列表
     */
    @Override
    public PageResult<PlatformGbChannelVo> queryPageList(PlatformGbChannelBo bo, PageQuery pageQuery) {
        Page<PlatformGbChannelVo> result = platformGbChannelMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的级联关系列表。
     *
     * @param bo 查询条件
     * @return 级联关系列表
     */
    @Override
    public List<PlatformGbChannelVo> queryList(PlatformGbChannelBo bo) {
        return platformGbChannelMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构造级联关系查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<PlatformGbChannel> buildQueryWrapper(PlatformGbChannelBo bo) {
        return QueryBuilder.lambda(PlatformGbChannel.class)
            .eqIfText(PlatformGbChannel::getPlatformId, bo.getPlatformId())
            .eqIfText(PlatformGbChannel::getCatalogId, bo.getCatalogId())
            .eqIfText(PlatformGbChannel::getDeviceChannelId, bo.getDeviceChannelId())
            .orderByDesc(PlatformGbChannel::getId)
            .build();
    }

    /**
     * 新增级联关系。
     *
     * @param bo 级联关系业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(PlatformGbChannelBo bo) {
        PlatformGbChannel add = MapstructUtils.convert(bo, PlatformGbChannel.class);
        validEntityBeforeSave(add, null);
        boolean flag = platformGbChannelMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改级联关系。
     *
     * @param bo 级联关系业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(PlatformGbChannelBo bo) {
        PlatformGbChannel update = MapstructUtils.convert(bo, PlatformGbChannel.class);
        validEntityBeforeSave(update, update.getId());
        return platformGbChannelMapper.updateById(update) > 0;
    }

    /**
     * 保存前校验：同一平台目录下通道国标编号不允许重复（编辑排除自身）。
     *
     * @param entity    待保存实体
     * @param excludeId 编辑时排除的主键，新增传 null
     */
    private void validEntityBeforeSave(PlatformGbChannel entity, Long excludeId) {
        var checker = platformGbChannelMapper.lambda()
            .eq(PlatformGbChannel::getPlatformId, entity.getPlatformId())
            .eq(PlatformGbChannel::getCatalogId, entity.getCatalogId())
            .eq(PlatformGbChannel::getDeviceChannelId, entity.getDeviceChannelId());
        if (excludeId != null) {
            checker.ne(PlatformGbChannel::getId, excludeId);
        }
        if (checker.exists()) {
            throw new ServiceException("该平台目录下已存在此通道的级联关系，请勿重复添加");
        }
    }

    /**
     * 批量删除级联关系。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验（关系表无下游引用）
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return platformGbChannelMapper.deleteByIds(ids) > 0;
    }
}
