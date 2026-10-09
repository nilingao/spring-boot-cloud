package cn.com.nla.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import cn.com.nla.common.core.constant.CacheNames;
import cn.com.nla.common.core.utils.SpringUtils;
import cn.com.nla.common.core.utils.StreamUtils;
import cn.com.nla.common.core.utils.TreeBuildUtils;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.system.domain.SysArea;
import cn.com.nla.system.domain.bo.SysAreaBo;
import cn.com.nla.system.domain.vo.SysAreaVo;
import cn.com.nla.system.mapper.SysAreaMapper;
import cn.com.nla.system.service.ISysAreaService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 行政区划 服务实现
 *
 * @author TZY
 */
@RequiredArgsConstructor
@Service
public class SysAreaServiceImpl implements ISysAreaService {

    private final SysAreaMapper areaMapper;

    /**
     * 查询行政区划列表
     *
     * @param bo 查询条件
     * @return 行政区划列表
     */
    @Override
    public List<SysAreaVo> selectAreaList(SysAreaBo bo) {
        return areaMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 查询行政区划树（供前端级联选择）
     *
     * @param bo 查询条件
     * @return 行政区划树列表
     */
    @Override
    public List<Tree<Long>> selectAreaTree(SysAreaBo bo) {
        List<SysAreaVo> areas = selectAreaList(bo);
        if (CollUtil.isEmpty(areas)) {
            return CollUtil.newArrayList();
        }
        return TreeBuildUtils.buildMultiRoot(areas,
            SysAreaVo::getAreaId,
            SysAreaVo::getParentId,
            (node, tree) -> tree.setId(node.getAreaId())
                .setParentId(node.getParentId())
                .setName(node.getAreaName())
                .setWeight(node.getLevel()));
    }

    /**
     * 根据地区Id查询行政区划详情
     *
     * @param areaId 地区Id
     * @return 行政区划详情
     */
    @Override
    public SysAreaVo selectAreaById(Long areaId) {
        return areaMapper.selectVoById(areaId);
    }

    /**
     * 查询全部行政区划的「地区Id -> 地区名称」映射（带缓存）
     *
     * @return 地区Id 到地区名称的映射
     */
    @Cacheable(cacheNames = CacheNames.SYS_AREA, key = "'nameMap'")
    @Override
    public Map<Long, String> selectAreaNameMap() {
        List<SysArea> list = areaMapper.lambda().list();
        return StreamUtils.toMap(list, SysArea::getAreaId, SysArea::getAreaName);
    }

    /**
     * 根据省/市/区地区Id拼接完整地址名称
     *
     * @param provinceId 省地区Id
     * @param cityId     市地区Id
     * @param areaId     区县地区Id
     * @return 拼接后的地址名称
     */
    @Override
    public String findAddress(Long provinceId, Long cityId, Long areaId) {
        // 通过 AOP 代理调用，确保命中 @Cacheable 缓存
        Map<Long, String> nameMap = SpringUtils.getAopProxy(this).selectAreaNameMap();
        StringBuilder address = new StringBuilder();
        if (provinceId != null) {
            address.append(nameMap.getOrDefault(provinceId, ""));
        }
        if (cityId != null) {
            address.append(nameMap.getOrDefault(cityId, ""));
        }
        if (areaId != null) {
            address.append(nameMap.getOrDefault(areaId, ""));
        }
        return address.toString();
    }

    /**
     * 构造行政区划查询条件
     *
     * @param bo 查询条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<SysArea> buildQueryWrapper(SysAreaBo bo) {
        return QueryBuilder.lambda(SysArea.class)
            .eqIfPresent(SysArea::getParentId, bo.getParentId())
            .eqIfPresent(SysArea::getLevel, bo.getLevel())
            .likeIfText(SysArea::getAreaName, bo.getAreaName())
            .likeIfText(SysArea::getAreaCode, bo.getAreaCode())
            .orderByAsc(SysArea::getLevel, SysArea::getAreaId)
            .build();
    }

}
