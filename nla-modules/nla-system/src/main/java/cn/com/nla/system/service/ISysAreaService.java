package cn.com.nla.system.service;

import cn.hutool.core.lang.tree.Tree;
import cn.com.nla.system.domain.bo.SysAreaBo;
import cn.com.nla.system.domain.vo.SysAreaVo;

import java.util.List;
import java.util.Map;

/**
 * 行政区划 服务层
 * <p>
 * 行政区划为只读参考数据，仅提供查询能力（列表、树、详情、地址拼接），无写入接口；
 * 数据由批量导入维护。
 *
 * @author TZY
 */
public interface ISysAreaService {

    /**
     * 查询行政区划列表
     *
     * @param bo 查询条件
     * @return 行政区划列表
     */
    List<SysAreaVo> selectAreaList(SysAreaBo bo);

    /**
     * 查询行政区划树（供前端级联选择）
     *
     * @param bo 查询条件
     * @return 行政区划树列表
     */
    List<Tree<Long>> selectAreaTree(SysAreaBo bo);

    /**
     * 根据地区Id查询行政区划详情
     *
     * @param areaId 地区Id
     * @return 行政区划详情
     */
    SysAreaVo selectAreaById(Long areaId);

    /**
     * 查询全部行政区划的「地区Id -> 地区名称」映射（带缓存）
     *
     * @return 地区Id 到地区名称的映射
     */
    Map<Long, String> selectAreaNameMap();

    /**
     * 根据省/市/区地区Id拼接完整地址名称
     *
     * @param provinceId 省地区Id
     * @param cityId     市地区Id
     * @param areaId     区县地区Id
     * @return 拼接后的地址名称
     */
    String findAddress(Long provinceId, Long cityId, Long areaId);

}
