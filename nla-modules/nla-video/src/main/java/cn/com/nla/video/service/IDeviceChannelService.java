package cn.com.nla.video.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.video.domain.bo.DeviceChannelBo;
import cn.com.nla.video.domain.vo.DeviceChannelVo;

import java.util.Collection;
import java.util.List;

/**
 * 国标设备通道 Service 接口。
 *
 * @author TZY
 */
public interface IDeviceChannelService {

    /**
     * 查询通道详情。
     *
     * @param id 主键
     * @return 通道视图对象
     */
    DeviceChannelVo queryById(Long id);

    /**
     * 按设备国标编号与通道国标编号查询通道详情。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 通道视图对象，不存在返回 null
     */
    DeviceChannelVo queryByDeviceIdAndChannelId(String deviceId, String channelId);

    /**
     * 分页查询通道列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 通道分页列表
     */
    PageResult<DeviceChannelVo> queryPageList(DeviceChannelBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的通道列表。
     *
     * @param bo 查询条件
     * @return 通道列表
     */
    List<DeviceChannelVo> queryList(DeviceChannelBo bo);

    /**
     * 新增通道，同一设备下通道编号不允许重复。
     *
     * @param bo 通道业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(DeviceChannelBo bo);

    /**
     * 修改通道，同一设备下通道编号不允许重复。
     *
     * @param bo 通道业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(DeviceChannelBo bo);

    /**
     * 按设备国标编号删除该设备的全部通道。
     *
     * @param deviceId 设备国标编号
     * @return 删除行数
     */
    int deleteByDeviceId(String deviceId);

    /**
     * 校验并批量删除通道，已被国标级联引用的通道不允许删除。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
