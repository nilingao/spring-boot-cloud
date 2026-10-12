package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.video.domain.ParentPlatform;
import cn.com.nla.video.domain.PlatformCatalog;
import cn.com.nla.video.domain.PlatformGbChannel;
import cn.com.nla.video.domain.PlatformGbStream;
import cn.com.nla.video.domain.bo.PlatformCatalogBo;
import cn.com.nla.video.domain.vo.PlatformCatalogVo;
import cn.com.nla.video.mapper.ParentPlatformMapper;
import cn.com.nla.video.mapper.PlatformCatalogMapper;
import cn.com.nla.video.mapper.PlatformGbChannelMapper;
import cn.com.nla.video.mapper.PlatformGbStreamMapper;
import cn.com.nla.video.service.IPlatformCatalogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 上级平台目录 Service 实现：目录树维护，删除前校验子目录与级联引用。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PlatformCatalogServiceImpl implements IPlatformCatalogService {

    private final PlatformCatalogMapper platformCatalogMapper;
    private final ParentPlatformMapper parentPlatformMapper;
    private final PlatformGbChannelMapper platformGbChannelMapper;
    private final PlatformGbStreamMapper platformGbStreamMapper;

    /**
     * 查询目录详情。
     *
     * @param id 目录国标编号
     * @return 目录视图对象
     */
    @Override
    public PlatformCatalogVo queryById(String id) {
        return platformCatalogMapper.selectVoById(id);
    }

    /**
     * 分页查询目录列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 目录分页列表
     */
    @Override
    public PageResult<PlatformCatalogVo> queryPageList(PlatformCatalogBo bo, PageQuery pageQuery) {
        Page<PlatformCatalogVo> result = platformCatalogMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的目录列表。
     *
     * @param bo 查询条件
     * @return 目录列表
     */
    @Override
    public List<PlatformCatalogVo> queryList(PlatformCatalogBo bo) {
        return platformCatalogMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 按上级平台国标编号查询目录列表。
     *
     * @param platformId 上级平台国标编号
     * @return 目录列表
     */
    @Override
    public List<PlatformCatalogVo> queryByPlatformId(String platformId) {
        return platformCatalogMapper.lambda().eq(PlatformCatalog::getPlatformId, platformId).voList();
    }

    /**
     * 构造目录查询条件，按目录编号升序以贴合目录树顺序。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<PlatformCatalog> buildQueryWrapper(PlatformCatalogBo bo) {
        return QueryBuilder.lambda(PlatformCatalog.class)
            .eqIfText(PlatformCatalog::getParentId, bo.getParentId())
            .likeIfText(PlatformCatalog::getName, bo.getName())
            .eqIfText(PlatformCatalog::getPlatformId, bo.getPlatformId())
            .eqIfText(PlatformCatalog::getBusinessGroupId, bo.getBusinessGroupId())
            .orderByAsc(PlatformCatalog::getId)
            .build();
    }

    /**
     * 新增目录：主键为录入的目录国标编号，需校验非空且未占用。
     *
     * @param bo 目录业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(PlatformCatalogBo bo) {
        PlatformCatalog add = MapstructUtils.convert(bo, PlatformCatalog.class);
        if (StringUtils.isBlank(add.getId())) {
            throw new ServiceException("目录国标编号不能为空");
        }
        if (platformCatalogMapper.lambda().eq(PlatformCatalog::getId, add.getId()).exists()) {
            throw new ServiceException("目录国标编号已存在，请勿重复录入");
        }
        return platformCatalogMapper.insert(add) > 0;
    }

    /**
     * 修改目录。
     *
     * @param bo 目录业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(PlatformCatalogBo bo) {
        PlatformCatalog update = MapstructUtils.convert(bo, PlatformCatalog.class);
        if (StringUtils.isBlank(update.getId())) {
            throw new ServiceException("修改时必须指定目录国标编号");
        }
        return platformCatalogMapper.updateById(update) > 0;
    }

    /**
     * 批量删除目录：先校验子目录、上级平台默认目录以及通道/直播流级联引用。
     *
     * @param ids     待删除的目录编号集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        if (Boolean.TRUE.equals(isValid)) {
            boolean hasChild = platformCatalogMapper.lambda().in(PlatformCatalog::getParentId, ids).exists();
            boolean channelReferenced = platformGbChannelMapper.lambda()
                .in(PlatformGbChannel::getCatalogId, ids).exists();
            boolean streamReferenced = platformGbStreamMapper.lambda()
                .in(PlatformGbStream::getCatalogId, ids).exists();
            boolean platformReferenced = parentPlatformMapper.lambda()
                .in(ParentPlatform::getCatalogId, ids).exists();
            if (hasChild) {
                throw new ServiceException("目录下存在子目录，请先删除子目录");
            }
            if (channelReferenced || streamReferenced) {
                throw new ServiceException("目录已绑定级联通道或直播流，请先解除引用关系");
            }
            if (platformReferenced) {
                throw new ServiceException("目录已被上级平台设为共享根目录，请先修改平台配置");
            }
        }
        return platformCatalogMapper.deleteByIds(ids) > 0;
    }
}
