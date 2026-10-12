package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.video.domain.PlatformGbStream;
import cn.com.nla.video.domain.bo.PlatformGbStreamBo;
import cn.com.nla.video.domain.vo.PlatformGbStreamVo;
import cn.com.nla.video.mapper.PlatformGbStreamMapper;
import cn.com.nla.video.service.IPlatformGbStreamService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 平台目录与直播流级联关系 Service 实现。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PlatformGbStreamServiceImpl implements IPlatformGbStreamService {

    private final PlatformGbStreamMapper platformGbStreamMapper;

    /**
     * 查询级联关系详情。
     *
     * @param id 主键
     * @return 级联关系视图对象
     */
    @Override
    public PlatformGbStreamVo queryById(Long id) {
        return platformGbStreamMapper.selectVoById(id);
    }

    /**
     * 分页查询级联关系列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 级联关系分页列表
     */
    @Override
    public PageResult<PlatformGbStreamVo> queryPageList(PlatformGbStreamBo bo, PageQuery pageQuery) {
        Page<PlatformGbStreamVo> result = platformGbStreamMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的级联关系列表。
     *
     * @param bo 查询条件
     * @return 级联关系列表
     */
    @Override
    public List<PlatformGbStreamVo> queryList(PlatformGbStreamBo bo) {
        return platformGbStreamMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构造级联关系查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<PlatformGbStream> buildQueryWrapper(PlatformGbStreamBo bo) {
        return QueryBuilder.lambda(PlatformGbStream.class)
            .eqIfText(PlatformGbStream::getPlatformId, bo.getPlatformId())
            .eqIfText(PlatformGbStream::getCatalogId, bo.getCatalogId())
            .eqIfText(PlatformGbStream::getGbStreamId, bo.getGbStreamId())
            .orderByDesc(PlatformGbStream::getId)
            .build();
    }

    /**
     * 新增级联关系。
     *
     * @param bo 级联关系业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(PlatformGbStreamBo bo) {
        PlatformGbStream add = MapstructUtils.convert(bo, PlatformGbStream.class);
        validEntityBeforeSave(add, null);
        boolean flag = platformGbStreamMapper.insert(add) > 0;
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
    public Boolean updateByBo(PlatformGbStreamBo bo) {
        PlatformGbStream update = MapstructUtils.convert(bo, PlatformGbStream.class);
        validEntityBeforeSave(update, update.getId());
        return platformGbStreamMapper.updateById(update) > 0;
    }

    /**
     * 保存前校验：同一平台目录下直播流编号不允许重复（编辑排除自身）。
     *
     * @param entity    待保存实体
     * @param excludeId 编辑时排除的主键，新增传 null
     */
    private void validEntityBeforeSave(PlatformGbStream entity, Long excludeId) {
        var checker = platformGbStreamMapper.lambda()
            .eq(PlatformGbStream::getPlatformId, entity.getPlatformId())
            .eq(PlatformGbStream::getCatalogId, entity.getCatalogId())
            .eq(PlatformGbStream::getGbStreamId, entity.getGbStreamId());
        if (excludeId != null) {
            checker.ne(PlatformGbStream::getId, excludeId);
        }
        if (checker.exists()) {
            throw new ServiceException("该平台目录下已存在此直播流的级联关系，请勿重复添加");
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
        return platformGbStreamMapper.deleteByIds(ids) > 0;
    }
}
