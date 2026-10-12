package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.video.domain.DeviceAlarm;
import cn.com.nla.video.domain.bo.DeviceAlarmBo;
import cn.com.nla.video.domain.vo.DeviceAlarmVo;
import cn.com.nla.video.mapper.DeviceAlarmMapper;
import cn.com.nla.video.service.IDeviceAlarmService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 设备报警记录 Service 实现：只读查询与清理，写入留给信令侧上报。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceAlarmServiceImpl implements IDeviceAlarmService {

    private final DeviceAlarmMapper deviceAlarmMapper;

    /**
     * 查询报警详情。
     *
     * @param id 主键
     * @return 报警视图对象
     */
    @Override
    public DeviceAlarmVo queryById(Long id) {
        return deviceAlarmMapper.selectVoById(id);
    }

    /**
     * 分页查询报警列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 报警分页列表
     */
    @Override
    public PageResult<DeviceAlarmVo> queryPageList(DeviceAlarmBo bo, PageQuery pageQuery) {
        Page<DeviceAlarmVo> result = deviceAlarmMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的报警列表。
     *
     * @param bo 查询条件
     * @return 报警列表
     */
    @Override
    public List<DeviceAlarmVo> queryList(DeviceAlarmBo bo) {
        return deviceAlarmMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构造报警查询条件，按报警时间倒序。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<DeviceAlarm> buildQueryWrapper(DeviceAlarmBo bo) {
        return QueryBuilder.lambda(DeviceAlarm.class)
            .eqIfText(DeviceAlarm::getDeviceId, bo.getDeviceId())
            .eqIfText(DeviceAlarm::getChannelId, bo.getChannelId())
            .eqIfPresent(DeviceAlarm::getAlarmType, bo.getAlarmType())
            .orderByDesc(DeviceAlarm::getAlarmTime)
            .build();
    }

    /**
     * 新增报警记录。
     *
     * @param bo 报警业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DeviceAlarmBo bo) {
        DeviceAlarm add = MapstructUtils.convert(bo, DeviceAlarm.class);
        boolean flag = deviceAlarmMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 批量删除报警记录。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验（报警记录无外部引用）
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return deviceAlarmMapper.deleteByIds(ids) > 0;
    }
}
