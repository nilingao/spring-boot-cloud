package cn.com.nla.callcenter.service.impl;

import cn.com.nla.callcenter.config.fs.runtime.FsRuntimeConfigRefreshPublisher;
import cn.com.nla.callcenter.domain.RouteCall;
import cn.com.nla.callcenter.domain.RouteGateway;
import cn.com.nla.callcenter.domain.RouteGatewayGroup;
import cn.com.nla.callcenter.domain.RouteGroup;
import cn.com.nla.callcenter.domain.bo.RouteGroupBo;
import cn.com.nla.callcenter.domain.bo.RouteGroupGatewayEditBo;
import cn.com.nla.callcenter.domain.vo.FsOptionVo;
import cn.com.nla.callcenter.domain.vo.RouteGroupVo;
import cn.com.nla.callcenter.mapper.RouteCallMapper;
import cn.com.nla.callcenter.mapper.RouteGatewayGroupMapper;
import cn.com.nla.callcenter.mapper.RouteGatewayMapper;
import cn.com.nla.callcenter.mapper.RouteGroupMapper;
import cn.com.nla.callcenter.service.IRouteGroupService;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 路由组(网关组) Service 实现: 网关绑定以完整列表替换, 变更后发布受影响企业刷新事件。
 * <p>
 * 自旧工程 {@code RouteGroupManageServiceImpl} 迁移。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RouteGroupServiceImpl implements IRouteGroupService {

    /**
     * 下拉选项默认返回条数。
     */
    private static final int DEFAULT_OPTION_LIMIT = 20;

    private final RouteGroupMapper routeGroupMapper;
    private final RouteGatewayGroupMapper routeGatewayGroupMapper;
    private final RouteGatewayMapper routeGatewayMapper;
    private final RouteCallMapper routeCallMapper;
    private final FsRuntimeConfigRefreshPublisher refreshPublisher;

    /**
     * 查询路由组详情, 含已绑定的网关选项列表。
     *
     * @param id 路由组编号
     * @return 路由组视图对象(含 gateways)
     */
    @Override
    public RouteGroupVo queryById(Long id) {
        RouteGroupVo vo = routeGroupMapper.selectVoById(id);
        if (vo == null) {
            return null;
        }
        List<RouteGatewayGroup> assigned = routeGatewayGroupMapper.selectList(
            new LambdaQueryWrapper<RouteGatewayGroup>().eq(RouteGatewayGroup::getRouteGroupId, id));
        List<Long> gatewayIds = assigned.stream().map(RouteGatewayGroup::getGatewayId).toList();
        List<FsOptionVo> gateways = gatewayIds.isEmpty() ? Collections.emptyList()
            : routeGatewayMapper.selectByIds(gatewayIds).stream()
                .map(g -> FsOptionVo.builder()
                    .id(String.valueOf(g.getId()))
                    .name(g.getName())
                    .code(g.getMediaHost())
                    .status(g.getStatus())
                    .build())
                .toList();
        vo.setGateways(gateways);
        return vo;
    }

    /**
     * 分页查询路由组列表。
     *
     * @param bo        查询条件(名称模糊/状态)
     * @param pageQuery 分页参数
     * @return 路由组分页列表
     */
    @Override
    public List<RouteGroupVo> queryList(RouteGroupBo bo) {
        return routeGroupMapper.selectVoList(buildQueryWrapper(bo));
    }

    public PageResult<RouteGroupVo> queryPageList(RouteGroupBo bo, PageQuery pageQuery) {
        Page<RouteGroupVo> result = routeGroupMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 构造路由组查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<RouteGroup> buildQueryWrapper(RouteGroupBo bo) {
        return QueryBuilder.lambda(RouteGroup.class)
            .likeIfText(RouteGroup::getName, bo.getName())
            .eqIfPresent(RouteGroup::getStatus, bo.getStatus())
            .orderByAsc(RouteGroup::getId)
            .build();
    }

    /**
     * 新增路由组。
     *
     * @param bo 路由组业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(RouteGroupBo bo) {
        RouteGroup add = MapstructUtils.convert(bo, RouteGroup.class);
        boolean inserted = routeGroupMapper.insert(add) > 0;
        if (inserted) {
            publishAffectedCompanies(add.getId());
        }
        return inserted;
    }

    /**
     * 修改路由组。
     *
     * @param bo 路由组业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(RouteGroupBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改时必须指定路由组编号");
        }
        RouteGroup update = MapstructUtils.convert(bo, RouteGroup.class);
        boolean updated = routeGroupMapper.updateById(update) > 0;
        if (updated) {
            publishAffectedCompanies(update.getId());
        }
        return updated;
    }

    /**
     * 以完整列表替换路由组下的网关绑定, 事务内先删旧关系再批量插入。
     *
     * @param bo 路由组编号与网关编号列表
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateGateways(RouteGroupGatewayEditBo bo) {
        Long routeGroupId = bo.getRouteGroupId();
        if (routeGroupMapper.selectById(routeGroupId) == null) {
            throw new ServiceException("路由组不存在");
        }
        List<Long> gatewayIds = bo.getGatewayIds().stream().distinct().toList();
        for (Long gatewayId : gatewayIds) {
            if (routeGatewayMapper.selectById(gatewayId) == null) {
                throw new ServiceException("路由网关 " + gatewayId + " 不存在");
            }
        }
        // 完整替换: 先删除旧关系, 再批量插入新关系
        routeGatewayGroupMapper.delete(
            new LambdaQueryWrapper<RouteGatewayGroup>().eq(RouteGatewayGroup::getRouteGroupId, routeGroupId));
        if (!gatewayIds.isEmpty()) {
            List<RouteGatewayGroup> newRelations = gatewayIds.stream().map(gid -> {
                RouteGatewayGroup rel = new RouteGatewayGroup();
                rel.setRouteGroupId(routeGroupId);
                rel.setGatewayId(gid);
                return rel;
            }).toList();
            routeGatewayGroupMapper.insertBatch(newRelations);
        }
        publishAffectedCompanies(routeGroupId);
        return true;
    }

    /**
     * 切换路由组状态。
     *
     * @param bo 仅使用 id 与 status 字段
     * @return 是否更新成功
     */
    @Override
    public Boolean updateStatus(RouteGroupBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("必须指定路由组编号");
        }
        if (routeGroupMapper.selectById(bo.getId()) == null) {
            throw new ServiceException("路由组不存在");
        }
        RouteGroup update = new RouteGroup();
        update.setId(bo.getId());
        update.setStatus(bo.getStatus());
        boolean updated = routeGroupMapper.updateById(update) > 0;
        if (updated) {
            publishAffectedCompanies(bo.getId());
        }
        return updated;
    }

    /**
     * 批量删除路由组, 被外呼路由引用时拒绝删除并同步清理网关关系。
     *
     * @param ids     待删除的路由组编号集合
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
                if (routeGroupMapper.selectById(id) == null) {
                    throw new ServiceException("路由组不存在");
                }
                long refCount = routeCallMapper.selectCount(
                    new LambdaQueryWrapper<RouteCall>().eq(RouteCall::getRouteGroupId, id));
                if (refCount > 0) {
                    throw new ServiceException("该路由组已被 " + refCount + " 条外呼路由引用，无法删除");
                }
                // 删除关联网关关系
                routeGatewayGroupMapper.delete(
                    new LambdaQueryWrapper<RouteGatewayGroup>().eq(RouteGatewayGroup::getRouteGroupId, id));
            }
        }
        return routeGroupMapper.deleteByIds(ids) > 0;
    }

    /**
     * 查询路由组下拉选项。
     *
     * @param keyword 名称模糊关键词, 可为空
     * @param limit   返回条数上限, 为空时默认 20
     * @return 路由组选项列表
     */
    @Override
    public List<FsOptionVo> queryOptionList(String keyword, Integer limit) {
        int pageLimit = Objects.requireNonNullElse(limit, DEFAULT_OPTION_LIMIT);
        List<RouteGroup> list = routeGroupMapper.selectList(
            new LambdaQueryWrapper<RouteGroup>()
                .like(StringUtils.isNotBlank(keyword), RouteGroup::getName, keyword)
                .last("LIMIT " + pageLimit));
        return list.stream()
            .map(rg -> FsOptionVo.builder()
                .id(String.valueOf(rg.getId()))
                .name(rg.getName())
                .status(rg.getStatus())
                .build())
            .toList();
    }

    /**
     * 通过 fs_route_call 查找引用该路由组的企业并发布刷新事件。
     *
     * @param routeGroupId 路由组编号, 为空时忽略
     */
    private void publishAffectedCompanies(Long routeGroupId) {
        if (routeGroupId == null) {
            return;
        }
        List<Long> companyIds = routeCallMapper.selectList(
                new LambdaQueryWrapper<RouteCall>().eq(RouteCall::getRouteGroupId, routeGroupId))
            .stream().map(RouteCall::getCompanyId).distinct().toList();
        refreshPublisher.publishCompaniesChanged(companyIds);
    }

}
