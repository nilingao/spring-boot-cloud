package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.video.domain.DeviceMobilePosition;
import cn.com.nla.video.domain.bo.DeviceMobilePositionBo;
import cn.com.nla.video.domain.vo.DeviceMobilePositionVo;
import cn.com.nla.video.mapper.DeviceMobilePositionMapper;
import cn.com.nla.video.service.IDeviceMobilePositionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 设备移动位置记录 Service 实现：只读查询与清理，写入留给信令侧订阅上报。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceMobilePositionServiceImpl implements IDeviceMobilePositionService {

    private final DeviceMobilePositionMapper mobilePositionMapper;

    /**
     * 查询位置记录详情。
     *
     * @param id 主键
     * @return 位置记录视图对象
     */
    @Override
    public DeviceMobilePositionVo queryById(Long id) {
        return mobilePositionMapper.selectVoById(id);
    }

    /**
     * 分页查询位置记录列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 位置记录分页列表
     */
    @Override
    public PageResult<DeviceMobilePositionVo> queryPageList(DeviceMobilePositionBo bo, PageQuery pageQuery) {
        Page<DeviceMobilePositionVo> result = mobilePositionMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的位置记录列表。
     *
     * @param bo 查询条件
     * @return 位置记录列表
     */
    @Override
    public List<DeviceMobilePositionVo> queryList(DeviceMobilePositionBo bo) {
        return mobilePositionMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构造位置记录查询条件，按上报时间倒序。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<DeviceMobilePosition> buildQueryWrapper(DeviceMobilePositionBo bo) {
        return QueryBuilder.lambda(DeviceMobilePosition.class)
            .eqIfText(DeviceMobilePosition::getDeviceId, bo.getDeviceId())
            .eqIfText(DeviceMobilePosition::getChannelId, bo.getChannelId())
            .orderByDesc(DeviceMobilePosition::getTime)
            .build();
    }

    /**
     * 查询设备历史轨迹，起止时间同时传入时才按时间过滤。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号，可为空
     * @param startTime 起始时间，可为空
     * @param endTime   结束时间，可为空
     * @return 轨迹列表，按上报时间倒序
     */
    @Override
    public List<DeviceMobilePositionVo> queryHistory(String deviceId, String channelId, LocalDateTime startTime,
                                                    LocalDateTime endTime) {
        LambdaQueryWrapper<DeviceMobilePosition> wrapper = QueryBuilder.lambda(DeviceMobilePosition.class)
            .eq(DeviceMobilePosition::getDeviceId, deviceId)
            .eqIfText(DeviceMobilePosition::getChannelId, channelId)
            .between(startTime != null && endTime != null, DeviceMobilePosition::getTime, startTime, endTime)
            .orderByDesc(DeviceMobilePosition::getTime)
            .build();
        return mobilePositionMapper.selectVoList(wrapper);
    }

    /**
     * 查询设备最新一条位置记录。
     *
     * @param deviceId 设备国标编号
     * @return 最新位置记录，无记录返回 {@code null}
     */
    @Override
    public DeviceMobilePositionVo queryLatest(String deviceId) {
        return mobilePositionMapper.selectVoOne(QueryBuilder.lambda(DeviceMobilePosition.class)
            .eq(DeviceMobilePosition::getDeviceId, deviceId)
            .orderByDesc(DeviceMobilePosition::getTime)
            .last("limit 1")
            .build());
    }

    /**
     * 新增位置记录。
     *
     * @param bo 位置记录业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DeviceMobilePositionBo bo) {
        DeviceMobilePosition add = MapstructUtils.convert(bo, DeviceMobilePosition.class);
        boolean flag = mobilePositionMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 批量删除位置记录。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验（位置记录无外部引用）
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return mobilePositionMapper.deleteByIds(ids) > 0;
    }
}
