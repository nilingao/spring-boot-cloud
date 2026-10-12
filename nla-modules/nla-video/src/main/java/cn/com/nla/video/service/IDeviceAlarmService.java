package cn.com.nla.video.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.video.domain.bo.DeviceAlarmBo;
import cn.com.nla.video.domain.vo.DeviceAlarmVo;

import java.util.Collection;
import java.util.List;

/**
 * 设备报警记录 Service 接口。报警由信令侧写入，管理端只做查询与清理。
 *
 * @author TZY
 */
public interface IDeviceAlarmService {

    /**
     * 查询报警详情。
     *
     * @param id 主键
     * @return 报警视图对象
     */
    DeviceAlarmVo queryById(Long id);

    /**
     * 分页查询报警列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 报警分页列表
     */
    PageResult<DeviceAlarmVo> queryPageList(DeviceAlarmBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的报警列表。
     *
     * @param bo 查询条件
     * @return 报警列表
     */
    List<DeviceAlarmVo> queryList(DeviceAlarmBo bo);

    /**
     * 新增报警记录，供信令侧报警上报落库。
     *
     * @param bo 报警业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(DeviceAlarmBo bo);

    /**
     * 批量删除报警记录。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验（报警记录无外部引用，保留扩展点）
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
