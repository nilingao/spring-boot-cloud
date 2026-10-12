package cn.com.nla.video.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.video.domain.bo.DeviceBo;
import cn.com.nla.video.domain.vo.DeviceVo;

import java.util.Collection;
import java.util.List;

/**
 * 国标设备 Service 接口。
 *
 * @author TZY
 */
public interface IDeviceService {

    /**
     * 查询设备详情。
     *
     * @param id 主键
     * @return 设备视图对象
     */
    DeviceVo queryById(Long id);

    /**
     * 按设备国标编号查询设备详情。
     *
     * @param deviceId 设备国标编号
     * @return 设备视图对象，不存在返回 null
     */
    DeviceVo queryByDeviceId(String deviceId);

    /**
     * 分页查询设备列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 设备分页列表
     */
    PageResult<DeviceVo> queryPageList(DeviceBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的设备列表。
     *
     * @param bo 查询条件
     * @return 设备列表
     */
    List<DeviceVo> queryList(DeviceBo bo);

    /**
     * 查询在线设备列表，供信令侧广播使用。
     *
     * @return 在线设备列表
     */
    List<DeviceVo> queryOnlineList();

    /**
     * 查询在线且开启消息通道的设备列表。
     *
     * @return 设备列表
     */
    List<DeviceVo> queryOnlineMessageChannelList();

    /**
     * 新增设备，设备国标编号不允许重复。
     *
     * @param bo 设备业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(DeviceBo bo);

    /**
     * 修改设备，设备国标编号不允许与其他设备重复。
     *
     * @param bo 设备业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(DeviceBo bo);

    /**
     * 校验并批量删除设备，存在关联通道时拒绝删除。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
