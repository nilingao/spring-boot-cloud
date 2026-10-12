package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.TreeBuildUtils;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.common.video.basic.vo.sip.SyncStatus;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.video.domain.Device;
import cn.com.nla.video.domain.DeviceChannel;
import cn.com.nla.video.mapper.DeviceChannelMapper;
import cn.com.nla.video.mapper.DeviceMapper;
import cn.com.nla.video.service.IVideoChannelCatalogService;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备通道目录服务实现。
 *
 * <p>树形目录由设备与通道两张表在内存中拼装，父级指向目录节点的孤儿通道不参与建树，与旧实现一致；
 * 目录同步是异步信令过程，只发起指令，进度由协议侧的目录缓存提供。</p>
 *
 * @author TZY
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class VideoChannelCatalogServiceImpl implements IVideoChannelCatalogService {

    /**
     * 树根父级编号，设备节点固定挂根，父级为空的通道同样挂根
     */
    private static final String ROOT_PARENT_ID = "";

    /**
     * 节点类型：设备
     */
    private static final int NODE_TYPE_DEVICE = 1;

    /**
     * 节点类型：通道
     */
    private static final int NODE_TYPE_CHANNEL = 2;

    private final DeviceMapper deviceMapper;
    private final DeviceChannelMapper deviceChannelMapper;
    private final SipServer sipServer;
    private final SIPCommander sipCommander;

    @Override
    public List<Tree<String>> queryTree() {
        List<Integer> flags = LoginHelper.isSuperAdmin() ? List.of(0, 1) : List.of(0);
        List<Device> devices = deviceMapper.lambda().in(Device::getHasAdministrator, flags).list();
        if (devices.isEmpty()) {
            return List.of();
        }
        List<String> deviceIds = devices.stream().map(Device::getDeviceId).toList();
        List<DeviceChannel> channels = deviceChannelMapper.lambda()
            .in(DeviceChannel::getDeviceId, deviceIds)
            .list();
        List<ChannelNode> nodes = new ArrayList<>(devices.size() + channels.size());
        for (Device device : devices) {
            nodes.add(new ChannelNode(ROOT_PARENT_ID, device.getDeviceId(), device.getDeviceId(), NODE_TYPE_DEVICE,
                device.getOnline()));
        }
        for (DeviceChannel channel : channels) {
            nodes.add(new ChannelNode(StrUtil.nullToEmpty(channel.getParentId()), channel.getChannelId(),
                channel.getDeviceId(), NODE_TYPE_CHANNEL, channel.getStatus()));
        }
        return TreeBuildUtils.build(nodes, ROOT_PARENT_ID, (ChannelNode node, Tree<String> tree) -> {
            tree.setId(node.id());
            tree.setParentId(node.parentId());
            tree.setName(node.id());
            tree.putExtra("deviceId", node.deviceId());
            tree.putExtra("type", node.type());
            tree.putExtra("status", node.status());
        });
    }

    @Override
    public void sync(String deviceId) {
        DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceId);
        if (deviceVo == null) {
            throw new ServiceException("未获取设备信息");
        }
        if (!VideoService.getDeviceService().sync(sipServer, sipCommander, deviceVo)) {
            throw new ServiceException("设备同步失败");
        }
    }

    @Override
    public SyncStatus querySyncStatus(String deviceId) {
        SyncStatus syncStatus = VideoService.getDeviceService().getChannelSyncStatus(deviceId);
        if (syncStatus == null) {
            throw new ServiceException("设备同步失败");
        }
        return syncStatus;
    }

    /**
     * 建树用的中间节点。
     *
     * @param parentId 父级编号，设备与无父级的通道为空串
     * @param id       节点编号，设备国标编号或通道国标编号
     * @param deviceId 所属设备国标编号
     * @param type     节点类型：1 设备，2 通道
     * @param status   在线状态，设备取注册状态，通道取通道状态
     */
    private record ChannelNode(String parentId, String id, String deviceId, int type, Integer status) {
    }
}
