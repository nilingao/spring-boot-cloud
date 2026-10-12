package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.video.domain.DeviceChannel;
import cn.com.nla.video.domain.PlatformGbChannel;
import cn.com.nla.video.domain.bo.DeviceChannelBo;
import cn.com.nla.video.domain.vo.DeviceChannelVo;
import cn.com.nla.video.mapper.DeviceChannelMapper;
import cn.com.nla.video.mapper.PlatformGbChannelMapper;
import cn.com.nla.video.service.IDeviceChannelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 国标设备通道 Service 实现：目录同步落库后的管理端维护，删除前校验级联引用。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceChannelServiceImpl implements IDeviceChannelService {

    private final DeviceChannelMapper deviceChannelMapper;
    private final PlatformGbChannelMapper platformGbChannelMapper;

    /**
     * 查询通道详情。
     *
     * @param id 主键
     * @return 通道视图对象
     */
    @Override
    public DeviceChannelVo queryById(Long id) {
        return deviceChannelMapper.selectVoById(id);
    }

    /**
     * 按设备国标编号与通道国标编号查询通道详情。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 通道视图对象，不存在返回 null
     */
    @Override
    public DeviceChannelVo queryByDeviceIdAndChannelId(String deviceId, String channelId) {
        return deviceChannelMapper.lambda()
            .eq(DeviceChannel::getDeviceId, deviceId)
            .eq(DeviceChannel::getChannelId, channelId)
            .voOne();
    }

    /**
     * 分页查询通道列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 通道分页列表
     */
    @Override
    public PageResult<DeviceChannelVo> queryPageList(DeviceChannelBo bo, PageQuery pageQuery) {
        Page<DeviceChannelVo> result = deviceChannelMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的通道列表。
     *
     * @param bo 查询条件
     * @return 通道列表
     */
    @Override
    public List<DeviceChannelVo> queryList(DeviceChannelBo bo) {
        return deviceChannelMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构造通道查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<DeviceChannel> buildQueryWrapper(DeviceChannelBo bo) {
        return QueryBuilder.lambda(DeviceChannel.class)
            .eqIfText(DeviceChannel::getChannelId, bo.getChannelId())
            .eqIfText(DeviceChannel::getDeviceId, bo.getDeviceId())
            .likeIfText(DeviceChannel::getName, bo.getName())
            .eqIfPresent(DeviceChannel::getStatus, bo.getStatus())
            .orderByDesc(DeviceChannel::getId)
            .build();
    }

    /**
     * 新增通道。
     *
     * @param bo 通道业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DeviceChannelBo bo) {
        DeviceChannel add = MapstructUtils.convert(bo, DeviceChannel.class);
        validEntityBeforeSave(add, null);
        boolean flag = deviceChannelMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改通道。
     *
     * @param bo 通道业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(DeviceChannelBo bo) {
        DeviceChannel update = MapstructUtils.convert(bo, DeviceChannel.class);
        validEntityBeforeSave(update, update.getId());
        return deviceChannelMapper.updateById(update) > 0;
    }

    /**
     * 保存前校验：同一设备下通道国标编号不允许重复（编辑排除自身）。
     *
     * @param entity    待保存实体
     * @param excludeId 编辑时排除的主键，新增传 null
     */
    private void validEntityBeforeSave(DeviceChannel entity, Long excludeId) {
        var checker = deviceChannelMapper.lambda()
            .eq(DeviceChannel::getDeviceId, entity.getDeviceId())
            .eq(DeviceChannel::getChannelId, entity.getChannelId());
        if (excludeId != null) {
            checker.ne(DeviceChannel::getId, excludeId);
        }
        if (checker.exists()) {
            throw new ServiceException("该设备下通道国标编号已存在，请勿重复录入");
        }
    }

    /**
     * 按设备国标编号删除该设备的全部通道。
     *
     * @param deviceId 设备国标编号
     * @return 删除行数
     */
    @Override
    public int deleteByDeviceId(String deviceId) {
        return deviceChannelMapper.lambda().eq(DeviceChannel::getDeviceId, deviceId).deleteCount();
    }

    /**
     * 批量删除通道：先校验通道是否已被国标级联目录引用。
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
            List<DeviceChannel> rows = deviceChannelMapper.lambda()
                .select(DeviceChannel::getChannelId)
                .in(DeviceChannel::getId, ids)
                .list();
            if (rows.isEmpty()) {
                return false;
            }
            List<String> channelIds = rows.stream().map(DeviceChannel::getChannelId).toList();
            boolean referenced = platformGbChannelMapper.lambda()
                .in(PlatformGbChannel::getDeviceChannelId, channelIds)
                .exists();
            if (referenced) {
                throw new ServiceException("通道已被上级平台目录级联引用，请先解除级联关系");
            }
        }
        return deviceChannelMapper.deleteByIds(ids) > 0;
    }
}
