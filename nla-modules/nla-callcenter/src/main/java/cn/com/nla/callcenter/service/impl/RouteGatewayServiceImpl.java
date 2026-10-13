package cn.com.nla.callcenter.service.impl;

import cn.com.nla.callcenter.config.fs.runtime.FsRuntimeConfigRefreshPublisher;
import cn.com.nla.callcenter.domain.RouteCall;
import cn.com.nla.callcenter.domain.RouteGateway;
import cn.com.nla.callcenter.domain.RouteGatewayGroup;
import cn.com.nla.callcenter.domain.bo.RouteGatewayBo;
import cn.com.nla.callcenter.domain.vo.FsOptionVo;
import cn.com.nla.callcenter.domain.vo.RouteGatewayVo;
import cn.com.nla.callcenter.mapper.RouteCallMapper;
import cn.com.nla.callcenter.mapper.RouteGatewayGroupMapper;
import cn.com.nla.callcenter.mapper.RouteGatewayMapper;
import cn.com.nla.callcenter.service.IRouteGatewayService;
import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 路由网关(媒体网关) Service 实现: 名称唯一校验, 被路由组引用时拒绝删除,
 * 变更后经 fs_route_gateway_group -> fs_route_call 双跳发布受影响企业刷新事件。
 * <p>
 * 自旧工程 {@code RouteGatewayManageServiceImpl} 迁移。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RouteGatewayServiceImpl implements IRouteGatewayService {

    /**
     * 下拉选项默认返回条数。
     */
    private static final int DEFAULT_OPTION_LIMIT = 20;

    private final RouteGatewayMapper routeGatewayMapper;
    private final RouteGatewayGroupMapper routeGatewayGroupMapper;
    private final RouteCallMapper routeCallMapper;
    private final FsRuntimeConfigRefreshPublisher refreshPublisher;

    /**
     * 查询路由网关详情。
     *
     * @param id 路由网关编号
     * @return 路由网关视图对象
     */
    @Override
    public RouteGatewayVo queryById(Long id) {
        return routeGatewayMapper.selectVoById(id);
    }

    /**
     * 分页查询路由网关列表。
     *
     * @param bo        查询条件(名称模糊/状态)
     * @param pageQuery 分页参数
     * @return 路由网关分页列表
     */
    @Override
    public PageResult<RouteGatewayVo> queryPageList(RouteGatewayBo bo, PageQuery pageQuery) {
        Page<RouteGatewayVo> result = routeGatewayMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 构造路由网关查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<RouteGateway> buildQueryWrapper(RouteGatewayBo bo) {
        return QueryBuilder.lambda(RouteGateway.class)
            .likeIfText(RouteGateway::getName, bo.getName())
            .eqIfPresent(RouteGateway::getStatus, bo.getStatus())
            .orderByAsc(RouteGateway::getId)
            .build();
    }

    /**
     * 新增路由网关, 校验名称唯一。
     *
     * @param bo 路由网关业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(RouteGatewayBo bo) {
        return saveOrUpdateValidated(bo, false);
    }

    /**
     * 修改路由网关, 校验名称唯一(排除自身)。
     *
     * @param bo 路由网关业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(RouteGatewayBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改时必须指定路由网关编号");
        }
        return saveOrUpdateValidated(bo, true);
    }

    /**
     * 新增/修改共用的保存前校验与落库。
     *
     * @param bo       路由网关业务对象
     * @param isUpdate 是否为修改(修改时排除自身做唯一校验)
     * @return 是否保存成功
     */
    private Boolean saveOrUpdateValidated(RouteGatewayBo bo, boolean isUpdate) {
        if (isUpdate && routeGatewayMapper.selectById(bo.getId()) == null) {
            throw new ServiceException("路由网关不存在");
        }
        boolean nameDuplicate = routeGatewayMapper.lambda()
            .eq(RouteGateway::getName, bo.getName())
            .ne(isUpdate, RouteGateway::getId, bo.getId())
            .exists();
        if (nameDuplicate) {
            throw new ServiceException("路由网关名称已存在");
        }
        RouteGateway entity = MapstructUtils.convert(bo, RouteGateway.class);
        boolean saved = isUpdate ? routeGatewayMapper.updateById(entity) > 0 : routeGatewayMapper.insert(entity) > 0;
        if (saved) {
            publishAffectedCompanies(entity.getId());
        }
        return saved;
    }

    /**
     * 切换路由网关状态。
     *
     * @param bo 仅使用 id 与 status 字段
     * @return 是否更新成功
     */
    @Override
    public Boolean updateStatus(RouteGatewayBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("必须指定路由网关编号");
        }
        if (routeGatewayMapper.selectById(bo.getId()) == null) {
            throw new ServiceException("路由网关不存在");
        }
        RouteGateway update = new RouteGateway();
        update.setId(bo.getId());
        update.setStatus(bo.getStatus());
        boolean updated = routeGatewayMapper.updateById(update) > 0;
        if (updated) {
            publishAffectedCompanies(bo.getId());
        }
        return updated;
    }

    /**
     * 批量删除路由网关, 被路由组引用时拒绝删除。
     *
     * @param ids     待删除的路由网关编号集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        if (Boolean.TRUE.equals(isValid)) {
            for (Long id : ids) {
                if (routeGatewayMapper.selectById(id) == null) {
                    throw new ServiceException("路由网关不存在");
                }
                long refCount = routeGatewayGroupMapper.selectCount(
                    new LambdaQueryWrapper<RouteGatewayGroup>().eq(RouteGatewayGroup::getGatewayId, id));
                if (refCount > 0) {
                    throw new ServiceException("该路由网关已被 " + refCount + " 个路由组引用，无法删除");
                }
            }
        }
        return routeGatewayMapper.deleteByIds(ids) > 0;
    }

    /**
     * 查询路由网关下拉选项。
     *
     * @param keyword 名称模糊关键词, 可为空
     * @param limit   返回条数上限, 为空时默认 20
     * @return 路由网关选项列表
     */
    @Override
    public List<FsOptionVo> queryOptionList(String keyword, Integer limit) {
        int pageLimit = Objects.requireNonNullElse(limit, DEFAULT_OPTION_LIMIT);
        List<RouteGateway> list = routeGatewayMapper.selectList(
            new LambdaQueryWrapper<RouteGateway>()
                .like(StringUtils.isNotBlank(keyword), RouteGateway::getName, keyword)
                .last("LIMIT " + pageLimit));
        return list.stream()
            .map(rg -> FsOptionVo.builder()
                .id(String.valueOf(rg.getId()))
                .name(rg.getName())
                .code(rg.getMediaHost())
                .status(rg.getStatus())
                .build())
            .toList();
    }

    /**
     * 通过 fs_route_gateway_group -> fs_route_call 查找受影响的企业 ID 并发布刷新事件。
     *
     * @param gatewayId 路由网关编号, 为空时忽略
     */
    private void publishAffectedCompanies(Long gatewayId) {
        if (gatewayId == null) {
            return;
        }
        List<RouteGatewayGroup> groups = routeGatewayGroupMapper.selectList(
            new LambdaQueryWrapper<RouteGatewayGroup>().eq(RouteGatewayGroup::getGatewayId, gatewayId));
        if (groups.isEmpty()) {
            return;
        }
        List<Long> groupIds = groups.stream().map(RouteGatewayGroup::getRouteGroupId).distinct().toList();
        List<Long> companyIds = routeCallMapper.selectList(
                new LambdaQueryWrapper<RouteCall>().in(RouteCall::getRouteGroupId, groupIds))
            .stream().map(RouteCall::getCompanyId).distinct().toList();
        refreshPublisher.publishCompaniesChanged(companyIds);
    }

}
