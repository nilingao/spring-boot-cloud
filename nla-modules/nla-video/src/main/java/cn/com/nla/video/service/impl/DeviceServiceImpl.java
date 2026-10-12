package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.video.domain.Device;
import cn.com.nla.video.domain.DeviceChannel;
import cn.com.nla.video.domain.bo.DeviceBo;
import cn.com.nla.video.domain.vo.DeviceVo;
import cn.com.nla.video.mapper.DeviceChannelMapper;
import cn.com.nla.video.mapper.DeviceMapper;
import cn.com.nla.video.service.IDeviceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 国标设备 Service 实现：管理侧 CRUD，删除前校验通道引用。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceServiceImpl implements IDeviceService {

    /** 在线标记值，与旧库 online 字段默认语义一致。 */
    private static final int ONLINE = 1;
    /** 启用标记值。 */
    private static final int ENABLED = 1;

    private final DeviceMapper deviceMapper;
    private final DeviceChannelMapper deviceChannelMapper;

    /**
     * 查询设备详情。
     *
     * @param id 主键
     * @return 设备视图对象
     */
    @Override
    public DeviceVo queryById(Long id) {
        return deviceMapper.selectVoById(id);
    }

    /**
     * 按设备国标编号查询设备详情。
     *
     * @param deviceId 设备国标编号
     * @return 设备视图对象，不存在返回 null
     */
    @Override
    public DeviceVo queryByDeviceId(String deviceId) {
        return deviceMapper.lambda().eq(Device::getDeviceId, deviceId).voOne();
    }

    /**
     * 分页查询设备列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 设备分页列表
     */
    @Override
    public PageResult<DeviceVo> queryPageList(DeviceBo bo, PageQuery pageQuery) {
        Page<DeviceVo> result = deviceMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的设备列表。
     *
     * @param bo 查询条件
     * @return 设备列表
     */
    @Override
    public List<DeviceVo> queryList(DeviceBo bo) {
        return deviceMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 查询在线设备列表。
     *
     * @return 在线设备列表
     */
    @Override
    public List<DeviceVo> queryOnlineList() {
        return deviceMapper.lambda().eq(Device::getOnline, ONLINE).voList();
    }

    /**
     * 查询在线且开启消息通道的设备列表。
     *
     * @return 设备列表
     */
    @Override
    public List<DeviceVo> queryOnlineMessageChannelList() {
        return deviceMapper.lambda()
            .eq(Device::getOnline, ONLINE)
            .eq(Device::getAsMessageChannel, ENABLED)
            .voList();
    }

    /**
     * 构造设备查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<Device> buildQueryWrapper(DeviceBo bo) {
        return QueryBuilder.lambda(Device.class)
            .eqIfText(Device::getDeviceId, bo.getDeviceId())
            .likeIfText(Device::getName, bo.getName())
            .eqIfText(Device::getIp, bo.getIp())
            .eqIfPresent(Device::getOnline, bo.getOnline())
            .eqIfText(Device::getMediaServerId, bo.getMediaServerId())
            .orderByDesc(Device::getId)
            .build();
    }

    /**
     * 新增设备。
     *
     * @param bo 设备业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DeviceBo bo) {
        Device add = MapstructUtils.convert(bo, Device.class);
        validEntityBeforeSave(add, null);
        boolean flag = deviceMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改设备。
     *
     * @param bo 设备业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(DeviceBo bo) {
        Device update = MapstructUtils.convert(bo, Device.class);
        validEntityBeforeSave(update, update.getId());
        return deviceMapper.updateById(update) > 0;
    }

    /**
     * 保存前校验：设备国标编号在有效设备内不允许重复（编辑排除自身）。
     *
     * @param entity    待保存实体
     * @param excludeId 编辑时排除的主键，新增传 null
     */
    private void validEntityBeforeSave(Device entity, Long excludeId) {
        var checker = deviceMapper.lambda().eq(Device::getDeviceId, entity.getDeviceId());
        if (excludeId != null) {
            checker.ne(Device::getId, excludeId);
        }
        if (checker.exists()) {
            throw new ServiceException("设备国标编号已存在，请勿重复录入");
        }
    }

    /**
     * 批量删除设备：先校验设备下是否仍有通道。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        if (Boolean.TRUE.equals(isValid)) {
            List<Device> rows = deviceMapper.lambda().select(Device::getDeviceId).in(Device::getId, ids).list();
            if (rows.isEmpty()) {
                return false;
            }
            List<String> deviceIds = rows.stream().map(Device::getDeviceId).toList();
            boolean referenced = deviceChannelMapper.lambda()
                .in(DeviceChannel::getDeviceId, deviceIds)
                .exists();
            if (referenced) {
                throw new ServiceException("设备下仍存在通道，请先删除相关通道");
            }
        }
        return deviceMapper.deleteByIds(ids) > 0;
    }
}
