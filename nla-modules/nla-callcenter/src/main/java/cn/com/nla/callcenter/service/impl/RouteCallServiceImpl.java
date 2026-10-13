package cn.com.nla.callcenter.service.impl;

 import java.util.List;
import cn.com.nla.callcenter.config.fs.runtime.FsRuntimeConfigRefreshPublisher;
import cn.com.nla.callcenter.domain.RouteCall;
import cn.com.nla.callcenter.domain.RouteGroup;
import cn.com.nla.callcenter.domain.bo.RouteCallBo;
import cn.com.nla.callcenter.domain.vo.RouteCallVo;
import cn.com.nla.callcenter.mapper.RouteCallMapper;
import cn.com.nla.callcenter.mapper.RouteGroupMapper;
import cn.com.nla.callcenter.service.IRouteCallService;
import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * 外呼路由(字冠路由) Service 实现: 号码区间与同企业字冠唯一校验, 变更后发布企业刷新事件。
 * <p>
 * 自旧工程 {@code RouteCallManageServiceImpl} 迁移。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RouteCallServiceImpl implements IRouteCallService {

    private final RouteCallMapper routeCallMapper;
    private final RouteGroupMapper routeGroupMapper;
    private final FsRuntimeConfigRefreshPublisher refreshPublisher;

    /**
     * 查询外呼路由详情。
     *
     * @param id 外呼路由编号
     * @return 外呼路由视图对象
     */
    @Override
    public RouteCallVo queryById(Long id) {
        return routeCallMapper.selectVoById(id);
    }

    /**
     * 分页查询外呼路由列表。
     *
     * @param bo        查询条件(企业/路由组/状态)
     * @param pageQuery 分页参数
     * @return 外呼路由分页列表
     */
    @Override
    public List<RouteCallVo> queryList(RouteCallBo bo) {
        return routeCallMapper.selectVoList(buildQueryWrapper(bo));
    }

    public PageResult<RouteCallVo> queryPageList(RouteCallBo bo, PageQuery pageQuery) {
        Page<RouteCallVo> result = routeCallMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 构造外呼路由查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<RouteCall> buildQueryWrapper(RouteCallBo bo) {
        return QueryBuilder.lambda(RouteCall.class)
            .eqIfPresent(RouteCall::getCompanyId, bo.getCompanyId())
            .eqIfPresent(RouteCall::getRouteGroupId, bo.getRouteGroupId())
            .eqIfPresent(RouteCall::getStatus, bo.getStatus())
            .orderByAsc(RouteCall::getId)
            .build();
    }

    /**
     * 新增外呼路由, 校验号码区间与同企业字冠唯一。
     *
     * @param bo 外呼路由业务对象
     * @return 是否新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(RouteCallBo bo) {
        return saveOrUpdateValidated(bo, false);
    }

    /**
     * 修改外呼路由, 校验号码区间与同企业字冠唯一(排除自身)。
     *
     * @param bo 外呼路由业务对象
     * @return 是否修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(RouteCallBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改时必须指定外呼路由编号");
        }
        return saveOrUpdateValidated(bo, true);
    }

    /**
     * 新增/修改共用的保存前校验与落库。
     *
     * @param bo       外呼路由业务对象
     * @param isUpdate 是否为修改(修改时排除自身做唯一校验)
     * @return 是否保存成功
     */
    private Boolean saveOrUpdateValidated(RouteCallBo bo, boolean isUpdate) {
        // numMin 不能大于 numMax
        if (bo.getNumMin() != null && bo.getNumMax() != null && bo.getNumMin() > bo.getNumMax()) {
            throw new ServiceException("号码最小值不能大于最大值");
        }
        if (isUpdate && routeCallMapper.selectById(bo.getId()) == null) {
            throw new ServiceException("外呼路由不存在");
        }
        // 校验路由组存在
        if (routeGroupMapper.selectById(bo.getRouteGroupId()) == null) {
            throw new ServiceException("路由组不存在");
        }
        // 同一企业 routeNum 唯一
        boolean nameDuplicate = routeCallMapper.lambda()
            .eq(RouteCall::getCompanyId, bo.getCompanyId())
            .eq(RouteCall::getRouteNum, bo.getRouteNum())
            .ne(isUpdate, RouteCall::getId, bo.getId())
            .exists();
        if (nameDuplicate) {
            throw new ServiceException("该企业下路由号码已存在");
        }
        RouteCall entity = MapstructUtils.convert(bo, RouteCall.class);
        boolean saved = isUpdate ? routeCallMapper.updateById(entity) > 0 : routeCallMapper.insert(entity) > 0;
        if (saved) {
            refreshPublisher.publishCompanyChanged(entity.getCompanyId());
        }
        return saved;
    }

    /**
     * 切换外呼路由状态。
     *
     * @param bo 仅使用 id 与 status 字段
     * @return 是否更新成功
     */
    @Override
    public Boolean updateStatus(RouteCallBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("必须指定外呼路由编号");
        }
        RouteCall existing = routeCallMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException("外呼路由不存在");
        }
        RouteCall update = new RouteCall();
        update.setId(bo.getId());
        update.setStatus(bo.getStatus());
        boolean updated = routeCallMapper.updateById(update) > 0;
        if (updated) {
            refreshPublisher.publishCompanyChanged(existing.getCompanyId());
        }
        return updated;
    }

    /**
     * 批量删除外呼路由。
     *
     * @param ids     待删除的外呼路由编号集合
     * @param isValid 是否进行有效性校验(保留参数形态, 当前域无引用约束)
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            RouteCall existing = routeCallMapper.selectById(id);
            if (existing == null) {
                continue;
            }
            routeCallMapper.deleteById(id);
            refreshPublisher.publishCompanyChanged(existing.getCompanyId());
        }
        return true;
    }

}
