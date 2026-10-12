package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.video.domain.GbStream;
import cn.com.nla.video.domain.PlatformGbStream;
import cn.com.nla.video.domain.bo.GbStreamBo;
import cn.com.nla.video.domain.vo.GbStreamVo;
import cn.com.nla.video.mapper.GbStreamMapper;
import cn.com.nla.video.mapper.PlatformGbStreamMapper;
import cn.com.nla.video.service.IGbStreamService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 国标直播流 Service 实现：流与国标编号映射维护，删除前校验平台级联引用。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GbStreamServiceImpl implements IGbStreamService {

    private final GbStreamMapper gbStreamMapper;
    private final PlatformGbStreamMapper platformGbStreamMapper;

    /**
     * 查询直播流详情。
     *
     * @param gbStreamId 主键
     * @return 直播流视图对象
     */
    @Override
    public GbStreamVo queryById(Long gbStreamId) {
        return gbStreamMapper.selectVoById(gbStreamId);
    }

    /**
     * 按流国标编号查询直播流详情。
     *
     * @param gbId 流国标编号
     * @return 直播流视图对象，不存在返回 null
     */
    @Override
    public GbStreamVo queryByGbId(String gbId) {
        return gbStreamMapper.lambda().eq(GbStream::getGbId, gbId).voOne();
    }

    /**
     * 分页查询直播流列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 直播流分页列表
     */
    @Override
    public PageResult<GbStreamVo> queryPageList(GbStreamBo bo, PageQuery pageQuery) {
        Page<GbStreamVo> result = gbStreamMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的直播流列表。
     *
     * @param bo 查询条件
     * @return 直播流列表
     */
    @Override
    public List<GbStreamVo> queryList(GbStreamBo bo) {
        return gbStreamMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构造直播流查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<GbStream> buildQueryWrapper(GbStreamBo bo) {
        return QueryBuilder.lambda(GbStream.class)
            .eqIfText(GbStream::getApp, bo.getApp())
            .eqIfText(GbStream::getStream, bo.getStream())
            .eqIfText(GbStream::getGbId, bo.getGbId())
            .likeIfText(GbStream::getName, bo.getName())
            .eqIfPresent(GbStream::getStreamType, bo.getStreamType())
            .eqIfText(GbStream::getMediaServerId, bo.getMediaServerId())
            .orderByDesc(GbStream::getGbStreamId)
            .build();
    }

    /**
     * 新增直播流。
     *
     * @param bo 直播流业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(GbStreamBo bo) {
        GbStream add = MapstructUtils.convert(bo, GbStream.class);
        validEntityBeforeSave(add, null);
        boolean flag = gbStreamMapper.insert(add) > 0;
        if (flag) {
            bo.setGbStreamId(add.getGbStreamId());
        }
        return flag;
    }

    /**
     * 修改直播流。
     *
     * @param bo 直播流业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(GbStreamBo bo) {
        GbStream update = MapstructUtils.convert(bo, GbStream.class);
        validEntityBeforeSave(update, update.getGbStreamId());
        return gbStreamMapper.updateById(update) > 0;
    }

    /**
     * 保存前校验：流国标编号在有效流内不允许重复（编辑排除自身）。
     *
     * @param entity    待保存实体
     * @param excludeId 编辑时排除的主键，新增传 null
     */
    private void validEntityBeforeSave(GbStream entity, Long excludeId) {
        var checker = gbStreamMapper.lambda().eq(GbStream::getGbId, entity.getGbId());
        if (excludeId != null) {
            checker.ne(GbStream::getGbStreamId, excludeId);
        }
        if (checker.exists()) {
            throw new ServiceException("直播流国标编号已存在，请勿重复录入");
        }
    }

    /**
     * 批量删除直播流：先校验流是否已被上级平台级联。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        if (Boolean.TRUE.equals(isValid)) {
            List<GbStream> rows = gbStreamMapper.lambda()
                .select(GbStream::getGbId)
                .in(GbStream::getGbStreamId, ids)
                .list();
            if (rows.isEmpty()) {
                return false;
            }
            List<String> gbIds = rows.stream().map(GbStream::getGbId).toList();
            boolean referenced = platformGbStreamMapper.lambda()
                .in(PlatformGbStream::getGbStreamId, gbIds)
                .exists();
            if (referenced) {
                throw new ServiceException("直播流已被上级平台级联引用，请先解除级联关系");
            }
        }
        return gbStreamMapper.deleteByIds(ids) > 0;
    }
}
