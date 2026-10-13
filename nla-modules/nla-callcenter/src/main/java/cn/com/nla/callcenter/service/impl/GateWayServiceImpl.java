package cn.com.nla.callcenter.service.impl;

import cn.com.nla.callcenter.domain.GateWay;
import cn.com.nla.callcenter.domain.bo.GateWayBo;
import cn.com.nla.callcenter.domain.vo.FsOptionVo;
import cn.com.nla.callcenter.domain.vo.GateWayVo;
import cn.com.nla.callcenter.mapper.GateWayMapper;
import cn.com.nla.callcenter.service.IGateWayService;
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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * FS 注册网关 Service 实现: 单表 CRUD, 密码仅入库不回显, 留空修改时保持原值。
 * <p>
 * 自旧工程 {@code FsGatewayManageServiceImpl} 迁移, 无运行时缓存联动。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GateWayServiceImpl implements IGateWayService {

    /**
     * 下拉选项默认返回条数。
     */
    private static final int DEFAULT_OPTION_LIMIT = 20;

    private final GateWayMapper gateWayMapper;

    /**
     * 查询网关详情, 密码字段不回显。
     *
     * @param id 网关编号
     * @return 网关视图对象
     */
    @Override
    public GateWayVo queryById(Long id) {
        return gateWayMapper.selectVoById(id);
    }

    /**
     * 分页查询网关列表。
     *
     * @param bo        查询条件(名称模糊/选中标记)
     * @param pageQuery 分页参数
     * @return 网关分页列表
     */
    @Override
    public List<GateWayVo> queryList(GateWayBo bo) {
        return gateWayMapper.selectVoList(buildQueryWrapper(bo));
    }

    public PageResult<GateWayVo> queryPageList(GateWayBo bo, PageQuery pageQuery) {
        Page<GateWayVo> result = gateWayMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 构造网关查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<GateWay> buildQueryWrapper(GateWayBo bo) {
        return QueryBuilder.lambda(GateWay.class)
            .likeIfText(GateWay::getName, bo.getName())
            .eqIfText(GateWay::getSelected, bo.getSelected())
            .orderByAsc(GateWay::getId)
            .build();
    }

    /**
     * 新增网关。
     *
     * @param bo 网关业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(GateWayBo bo) {
        GateWay add = MapstructUtils.convert(bo, GateWay.class);
        return gateWayMapper.insert(add) > 0;
    }

    /**
     * 修改网关, 密码留空时保持原值。
     *
     * @param bo 网关业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(GateWayBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改时必须指定网关编号");
        }
        GateWay update = MapstructUtils.convert(bo, GateWay.class);
        if (StringUtils.isBlank(update.getPassword())) {
            GateWay existing = gateWayMapper.selectById(update.getId());
            if (existing == null) {
                throw new ServiceException("FS注册网关不存在");
            }
            update.setPassword(existing.getPassword());
        }
        return gateWayMapper.updateById(update) > 0;
    }

    /**
     * 切换网关选中标记(0 未选中 / 1 已选中)。
     *
     * @param bo 仅使用 id 与 selected 字段
     * @return 是否更新成功
     */
    @Override
    public Boolean updateSelected(GateWayBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("必须指定网关编号");
        }
        if (!gateWayMapper.lambda().eq(GateWay::getId, bo.getId()).exists()) {
            throw new ServiceException("FS注册网关不存在");
        }
        GateWay update = new GateWay();
        update.setId(bo.getId());
        update.setSelected(bo.getSelected());
        return gateWayMapper.updateById(update) > 0;
    }

    /**
     * 批量删除网关, 无外部引用校验。
     *
     * @param ids     待删除的网关编号集合
     * @param isValid 是否进行有效性校验(保留参数形态, 当前域无引用约束)
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return gateWayMapper.deleteByIds(ids) > 0;
    }

    /**
     * 查询网关下拉选项。
     *
     * @param keyword 名称模糊关键词, 可为空
     * @param limit   返回条数上限, 为空时默认 20
     * @return 网关选项列表
     */
    @Override
    public List<FsOptionVo> queryOptionList(String keyword, Integer limit) {
        int pageLimit = Objects.requireNonNullElse(limit, DEFAULT_OPTION_LIMIT);
        List<GateWay> list = gateWayMapper.selectList(
            new LambdaQueryWrapper<GateWay>()
                .like(StringUtils.isNotBlank(keyword), GateWay::getName, keyword)
                .last("LIMIT " + pageLimit));
        return list.stream()
            .map(g -> FsOptionVo.builder()
                .id(String.valueOf(g.getId()))
                .name(g.getName())
                .code(g.getRouteId())
                .build())
            .toList();
    }

}
