package cn.com.nla.video.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.video.domain.bo.DeviceMobilePositionBo;
import cn.com.nla.video.domain.vo.DeviceMobilePositionVo;

import java.util.Collection;
import java.util.List;

/**
 * 设备移动位置记录 Service 接口。位置由信令侧订阅上报，管理端只做查询与清理。
 *
 * @author TZY
 */
public interface IDeviceMobilePositionService {

    /**
     * 查询位置记录详情。
     *
     * @param id 主键
     * @return 位置记录视图对象
     */
    DeviceMobilePositionVo queryById(Long id);

    /**
     * 分页查询位置记录列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 位置记录分页列表
     */
    PageResult<DeviceMobilePositionVo> queryPageList(DeviceMobilePositionBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的位置记录列表。
     *
     * @param bo 查询条件
     * @return 位置记录列表
     */
    List<DeviceMobilePositionVo> queryList(DeviceMobilePositionBo bo);

    /**
     * 新增位置记录。
     *
     * @param bo 位置记录业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(DeviceMobilePositionBo bo);

    /**
     * 批量删除位置记录。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验（位置记录无外部引用，保留扩展点）
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
