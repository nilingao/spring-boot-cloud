package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.video.domain.ParentPlatform;
import cn.com.nla.video.domain.PlatformCatalog;
import cn.com.nla.video.domain.PlatformGbChannel;
import cn.com.nla.video.domain.PlatformGbStream;
import cn.com.nla.video.domain.bo.ParentPlatformBo;
import cn.com.nla.video.domain.vo.ParentPlatformVo;
import cn.com.nla.video.mapper.ParentPlatformMapper;
import cn.com.nla.video.mapper.PlatformCatalogMapper;
import cn.com.nla.video.mapper.PlatformGbChannelMapper;
import cn.com.nla.video.mapper.PlatformGbStreamMapper;
import cn.com.nla.video.service.IParentPlatformService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 上级平台 Service 实现：国标级联配置维护，删除前校验目录与级联关系。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ParentPlatformServiceImpl implements IParentPlatformService {

    /** 启用标记值。 */
    private static final int ENABLED = 1;

    private final ParentPlatformMapper parentPlatformMapper;
    private final PlatformCatalogMapper platformCatalogMapper;
    private final PlatformGbChannelMapper platformGbChannelMapper;
    private final PlatformGbStreamMapper platformGbStreamMapper;

    /**
     * 查询上级平台详情。
     *
     * @param id 主键
     * @return 上级平台视图对象
     */
    @Override
    public ParentPlatformVo queryById(Long id) {
        return parentPlatformMapper.selectVoById(id);
    }

    /**
     * 按上级平台国标编号查询详情。
     *
     * @param serverGbId 上级平台国标编号
     * @return 上级平台视图对象，不存在返回 null
     */
    @Override
    public ParentPlatformVo queryByServerGbId(String serverGbId) {
        return parentPlatformMapper.lambda().eq(ParentPlatform::getServerGbId, serverGbId).voOne();
    }

    /**
     * 分页查询上级平台列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 上级平台分页列表
     */
    @Override
    public PageResult<ParentPlatformVo> queryPageList(ParentPlatformBo bo, PageQuery pageQuery) {
        Page<ParentPlatformVo> result = parentPlatformMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的上级平台列表。
     *
     * @param bo 查询条件
     * @return 上级平台列表
     */
    @Override
    public List<ParentPlatformVo> queryList(ParentPlatformBo bo) {
        return parentPlatformMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 查询已启用的上级平台列表。
     *
     * @return 上级平台列表
     */
    @Override
    public List<ParentPlatformVo> queryEnableList() {
        return parentPlatformMapper.lambda().eq(ParentPlatform::getEnable, ENABLED).voList();
    }

    /**
     * 构造上级平台查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<ParentPlatform> buildQueryWrapper(ParentPlatformBo bo) {
        return QueryBuilder.lambda(ParentPlatform.class)
            .eqIfPresent(ParentPlatform::getEnable, bo.getEnable())
            .likeIfText(ParentPlatform::getName, bo.getName())
            .eqIfText(ParentPlatform::getServerGbId, bo.getServerGbId())
            .eqIfText(ParentPlatform::getCatalogId, bo.getCatalogId())
            .eqIfPresent(ParentPlatform::getStatus, bo.getStatus())
            .orderByDesc(ParentPlatform::getId)
            .build();
    }

    /**
     * 新增上级平台。
     *
     * @param bo 上级平台业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(ParentPlatformBo bo) {
        ParentPlatform add = MapstructUtils.convert(bo, ParentPlatform.class);
        validEntityBeforeSave(add, null);
        boolean flag = parentPlatformMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改上级平台。
     *
     * @param bo 上级平台业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(ParentPlatformBo bo) {
        ParentPlatform update = MapstructUtils.convert(bo, ParentPlatform.class);
        validEntityBeforeSave(update, update.getId());
        return parentPlatformMapper.updateById(update) > 0;
    }

    /**
     * 保存前校验：上级平台国标编号在有效平台内不允许重复（编辑排除自身）。
     *
     * @param entity    待保存实体
     * @param excludeId 编辑时排除的主键，新增传 null
     */
    private void validEntityBeforeSave(ParentPlatform entity, Long excludeId) {
        var checker = parentPlatformMapper.lambda().eq(ParentPlatform::getServerGbId, entity.getServerGbId());
        if (excludeId != null) {
            checker.ne(ParentPlatform::getId, excludeId);
        }
        if (checker.exists()) {
            throw new ServiceException("上级平台国标编号已存在，请勿重复录入");
        }
    }

    /**
     * 批量删除上级平台：先校验目录、通道、直播流级联关系是否仍存在。
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
            List<ParentPlatform> rows = parentPlatformMapper.lambda()
                .select(ParentPlatform::getServerGbId)
                .in(ParentPlatform::getId, ids)
                .list();
            if (rows.isEmpty()) {
                return false;
            }
            List<String> serverGbIds = rows.stream().map(ParentPlatform::getServerGbId).toList();
            boolean catalogReferenced = platformCatalogMapper.lambda()
                .in(PlatformCatalog::getPlatformId, serverGbIds).exists();
            boolean channelReferenced = platformGbChannelMapper.lambda()
                .in(PlatformGbChannel::getPlatformId, serverGbIds).exists();
            boolean streamReferenced = platformGbStreamMapper.lambda()
                .in(PlatformGbStream::getPlatformId, serverGbIds).exists();
            if (catalogReferenced || channelReferenced || streamReferenced) {
                throw new ServiceException("上级平台仍存在目录或级联通道/直播流，请先删除相关级联数据");
            }
        }
        return parentPlatformMapper.deleteByIds(ids) > 0;
    }
}
