package cn.com.nla.video.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.video.domain.bo.DeviceMobilePositionBo;
import cn.com.nla.video.domain.vo.DeviceMobilePositionVo;

import java.time.LocalDateTime;
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
     * 查询设备历史轨迹，起止时间同时传入时才按时间过滤。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号，可为空
     * @param startTime 起始时间，可为空
     * @param endTime   结束时间，可为空
     * @return 轨迹列表，按上报时间倒序
     */
    List<DeviceMobilePositionVo> queryHistory(String deviceId, String channelId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询设备最新一条位置记录。
     *
     * @param deviceId 设备国标编号
     * @return 最新位置记录，无记录返回 {@code null}
     */
    DeviceMobilePositionVo queryLatest(String deviceId);

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
