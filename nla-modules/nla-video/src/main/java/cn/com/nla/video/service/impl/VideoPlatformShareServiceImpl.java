package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.core.utils.TreeBuildUtils;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.common.video.basic.common.CatalogEventConstant;
import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.vo.sip.Address;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.video.domain.Device;
import cn.com.nla.video.domain.DeviceChannel;
import cn.com.nla.video.domain.GbStream;
import cn.com.nla.video.domain.PlatformCatalog;
import cn.com.nla.video.domain.PlatformGbChannel;
import cn.com.nla.video.domain.PlatformGbStream;
import cn.com.nla.video.domain.bo.GbStreamBo;
import cn.com.nla.video.domain.bo.VideoPlatformShareBo;
import cn.com.nla.video.domain.vo.DeviceChannelVo;
import cn.com.nla.video.domain.vo.GbStreamVo;
import cn.com.nla.video.domain.vo.VideoBindKeyVo;
import cn.com.nla.video.mapper.DeviceChannelMapper;
import cn.com.nla.video.mapper.DeviceMapper;
import cn.com.nla.video.mapper.GbStreamMapper;
import cn.com.nla.video.mapper.PlatformCatalogMapper;
import cn.com.nla.video.mapper.PlatformGbChannelMapper;
import cn.com.nla.video.mapper.PlatformGbStreamMapper;
import cn.com.nla.video.service.IGbStreamService;
import cn.com.nla.video.service.IVideoChannelCatalogService;
import cn.com.nla.video.service.IVideoPlatformShareService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 上级平台级联共享服务实现。
 *
 * <p>绑定关系落在关联表，目录树与已绑定回显按平台维度组装；每次绑定变更后按平台推送目录增量事件，
 * 与旧实现一致，非管理员只能共享未标记为管理员专属的设备通道。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，协议未开启时不暴露指令入口。</p>
 *
 * @author TZY
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class VideoPlatformShareServiceImpl implements IVideoPlatformShareService {

    /**
     * 树根父级编号
     */
    private static final String ROOT_PARENT_ID = "";

    /**
     * 解除类型：全部
     */
    private static final int RELATION_ALL = 0;

    /**
     * 解除类型：通道
     */
    private static final int RELATION_CHANNEL = 1;

    /**
     * 解除类型：直播流
     */
    private static final int RELATION_STREAM = 2;

    private final DeviceMapper deviceMapper;
    private final DeviceChannelMapper deviceChannelMapper;
    private final GbStreamMapper gbStreamMapper;
    private final PlatformCatalogMapper platformCatalogMapper;
    private final PlatformGbChannelMapper platformGbChannelMapper;
    private final PlatformGbStreamMapper platformGbStreamMapper;
    private final IGbStreamService gbStreamService;
    private final IVideoChannelCatalogService videoChannelCatalogService;

    @Override
    public List<Address> querySipList() {
        return RedisService.getRegisterServerManager().getSipList();
    }

    @Override
    public List<Tree<String>> queryChannelTree() {
        return videoChannelCatalogService.queryTree();
    }

    @Override
    public List<Tree<String>> queryCatalogTree(String platformId) {
        ParentPlatformVo platform = requirePlatform(platformId);
        List<PlatformCatalog> nodes = platformCatalogMapper.lambda()
            .eq(PlatformCatalog::getPlatformId, platform.getServerGbId())
            .list();
        PlatformCatalog root = new PlatformCatalog();
        root.setId(platform.getServerGbId());
        root.setParentId(ROOT_PARENT_ID);
        root.setPlatformId(platform.getServerGbId());
        root.setName(platform.getName());
        nodes.add(root);
        nodes.forEach(node -> node.setParentId(StrUtil.nullToEmpty(node.getParentId())));
        return TreeBuildUtils.build(nodes, ROOT_PARENT_ID, (PlatformCatalog node, Tree<String> tree) -> {
            tree.setId(node.getId());
            tree.setParentId(node.getParentId());
            tree.setName(node.getName());
            tree.putExtra("platformId", node.getPlatformId());
            tree.putExtra("civilCode", node.getCivilCode());
            tree.putExtra("businessGroupId", node.getBusinessGroupId());
        });
    }

    @Override
    public void deleteCatalogRelation(String catalogId, Integer type) {
        PlatformCatalog catalog = requireCatalogOrRoot(catalogId);
        int relationType = type == null ? RELATION_ALL : type;
        VideoPlatformShareBo bo = new VideoPlatformShareBo();
        bo.setPlatformId(catalog.getPlatformId());
        bo.setCatalogId(catalog.getId());
        bo.setIsAll(ConstEnum.Flag.YES.getValue());
        bo.setIsSub(ConstEnum.Flag.NO.getValue());
        if (relationType == RELATION_ALL || relationType == RELATION_CHANNEL) {
            // 解除目录关系属于清理语义，目录下没有对应类型的绑定时静默跳过
            doUnbindChannel(bo, false);
        }
        if (relationType == RELATION_ALL || relationType == RELATION_STREAM) {
            doUnbindStream(bo, false);
        }
    }

    @Override
    public VideoBindKeyVo queryChannelBindKey(VideoPlatformShareBo bo) {
        ParentPlatformVo platform = requirePlatform(bo.getPlatformId());
        List<String> catalogIdList = resolveCatalogIds(platform.getServerGbId(), bo.getCatalogId(), bo.getIsSub());
        Map<String, String> catalogByChannel = bindCatalogMap(
            platformGbChannelMapper.lambda().select(PlatformGbChannel::getCatalogId, PlatformGbChannel::getDeviceChannelId)
                .eq(PlatformGbChannel::getPlatformId, platform.getServerGbId()).list(),
            PlatformGbChannel::getDeviceChannelId, PlatformGbChannel::getCatalogId);
        List<DeviceChannel> visibleChannels = queryVisibleChannels(new ArrayList<>(catalogByChannel.keySet()));
        List<String> allGbIdList = channelBindKeys(visibleChannels);
        List<DeviceChannel> catalogChannels = visibleChannels.stream()
            .filter(channel -> catalogIdList.contains(catalogByChannel.get(channel.getChannelId())))
            .toList();
        return new VideoBindKeyVo(allGbIdList, channelBindKeys(catalogChannels));
    }

    @Override
    public void bindChannel(VideoPlatformShareBo bo) {
        ParentPlatformVo platform = requirePlatform(bo.getPlatformId());
        requireCatalog(platform.getServerGbId(), bo.getCatalogId());
        List<String> selectedIds = requireSelectedIds(bo);
        Set<String> boundIds = new HashSet<>(bindIds(
            platformGbChannelMapper.lambda().select(PlatformGbChannel::getDeviceChannelId)
                .eq(PlatformGbChannel::getPlatformId, platform.getServerGbId()).list(),
            PlatformGbChannel::getDeviceChannelId));
        List<DeviceChannel> candidates = queryVisibleChannels(selectedIds).stream()
            .filter(channel -> !boundIds.contains(channel.getChannelId()))
            .toList();
        if (candidates.isEmpty()) {
            throw new ServiceException("未获取添加通道信息");
        }
        List<PlatformGbChannel> relations = candidates.stream().map(channel -> {
            PlatformGbChannel relation = new PlatformGbChannel();
            relation.setPlatformId(platform.getServerGbId());
            relation.setCatalogId(bo.getCatalogId());
            relation.setDeviceChannelId(channel.getChannelId());
            return relation;
        }).toList();
        platformGbChannelMapper.insertBatch(relations);
        VideoService.getPlatformCatalogService()
            .handleCatalogEvent(CatalogEventConstant.ADD, platform.getServerGbId(), toProtocolChannels(candidates), null, null);
    }

    @Override
    public void unbindChannel(VideoPlatformShareBo bo) {
        doUnbindChannel(bo, true);
    }

    /**
     * 执行通道解除绑定并推送目录 DEL 事件。
     *
     * @param bo          绑定入参
     * @param failOnEmpty 目录下无可移除通道时是否中断
     */
    private void doUnbindChannel(VideoPlatformShareBo bo, boolean failOnEmpty) {
        ParentPlatformVo platform = requirePlatform(bo.getPlatformId());
        List<String> catalogIdList = resolveCatalogIds(platform.getServerGbId(), bo.getCatalogId(), bo.getIsSub());
        List<DeviceChannelVo> sharedChannels = platformGbChannelMapper
            .selectSharedChannels(platform.getServerGbId(), catalogIdList);
        List<String> selectedIds = requireSelectedIds(bo);
        if (selectedIds != null) {
            sharedChannels = sharedChannels.stream()
                .filter(channel -> selectedIds.contains(channel.getChannelId()))
                .toList();
        }
        if (sharedChannels.isEmpty()) {
            if (failOnEmpty) {
                throw new ServiceException("未获取移除通道信息");
            }
            return;
        }
        List<String> channelIds = sharedChannels.stream().map(DeviceChannelVo::getChannelId).toList();
        platformGbChannelMapper.lambda()
            .eq(PlatformGbChannel::getPlatformId, platform.getServerGbId())
            .in(PlatformGbChannel::getCatalogId, catalogIdList)
            .in(PlatformGbChannel::getDeviceChannelId, channelIds)
            .deleteCount();
        VideoService.getPlatformCatalogService()
            .handleCatalogEvent(CatalogEventConstant.DEL, platform.getServerGbId(), toProtocolChannels(sharedChannels), null, null);
    }

    @Override
    public List<GbStreamVo> queryStreamList() {
        return gbStreamService.queryList(new GbStreamBo());
    }

    @Override
    public VideoBindKeyVo queryStreamBindKey(VideoPlatformShareBo bo) {
        ParentPlatformVo platform = requirePlatform(bo.getPlatformId());
        List<String> catalogIdList = resolveCatalogIds(platform.getServerGbId(), bo.getCatalogId(), bo.getIsSub());
        Map<String, String> catalogByStream = bindCatalogMap(
            platformGbStreamMapper.lambda().select(PlatformGbStream::getCatalogId, PlatformGbStream::getGbStreamId)
                .eq(PlatformGbStream::getPlatformId, platform.getServerGbId()).list(),
            PlatformGbStream::getGbStreamId, PlatformGbStream::getCatalogId);
        List<String> allGbIdList = new ArrayList<>(catalogByStream.keySet());
        List<String> catalogGbIdList = catalogByStream.entrySet().stream()
            .filter(entry -> catalogIdList.contains(entry.getValue()))
            .map(Map.Entry::getKey)
            .toList();
        return new VideoBindKeyVo(allGbIdList, catalogGbIdList);
    }

    @Override
    public void bindStream(VideoPlatformShareBo bo) {
        ParentPlatformVo platform = requirePlatform(bo.getPlatformId());
        requireCatalog(platform.getServerGbId(), bo.getCatalogId());
        List<String> selectedIds = requireSelectedIds(bo);
        Set<String> boundIds = new HashSet<>(bindIds(
            platformGbStreamMapper.lambda().select(PlatformGbStream::getGbStreamId)
                .eq(PlatformGbStream::getPlatformId, platform.getServerGbId()).list(),
            PlatformGbStream::getGbStreamId));
        List<GbStream> candidates = selectedIds == null
            ? gbStreamMapper.lambda().list()
            : gbStreamMapper.lambda().in(GbStream::getGbId, selectedIds).list();
        candidates = candidates.stream().filter(stream -> !boundIds.contains(stream.getGbId())).toList();
        if (candidates.isEmpty()) {
            throw new ServiceException("未获取添加国标流信息");
        }
        List<PlatformGbStream> relations = candidates.stream().map(stream -> {
            PlatformGbStream relation = new PlatformGbStream();
            relation.setPlatformId(platform.getServerGbId());
            relation.setCatalogId(bo.getCatalogId());
            relation.setGbStreamId(stream.getGbId());
            return relation;
        }).toList();
        platformGbStreamMapper.insertBatch(relations);
        VideoService.getPlatformCatalogService()
            .handleCatalogEvent(CatalogEventConstant.ADD, platform.getServerGbId(), null, toProtocolStreams(candidates), null);
    }

    @Override
    public void unbindStream(VideoPlatformShareBo bo) {
        doUnbindStream(bo, true);
    }

    /**
     * 执行国标流解除绑定并推送目录 DEL 事件。
     *
     * @param bo          绑定入参
     * @param failOnEmpty 目录下无可移除直播流时是否中断
     */
    private void doUnbindStream(VideoPlatformShareBo bo, boolean failOnEmpty) {
        ParentPlatformVo platform = requirePlatform(bo.getPlatformId());
        List<String> catalogIdList = resolveCatalogIds(platform.getServerGbId(), bo.getCatalogId(), bo.getIsSub());
        List<GbStreamVo> sharedStreams = platformGbStreamMapper
            .selectSharedStreams(platform.getServerGbId(), catalogIdList);
        List<String> selectedIds = requireSelectedIds(bo);
        if (selectedIds != null) {
            sharedStreams = sharedStreams.stream()
                .filter(stream -> selectedIds.contains(stream.getGbId()))
                .toList();
        }
        if (sharedStreams.isEmpty()) {
            if (failOnEmpty) {
                throw new ServiceException("未获取移除国标流信息");
            }
            return;
        }
        List<String> gbIdList = sharedStreams.stream().map(GbStreamVo::getGbId).toList();
        platformGbStreamMapper.lambda()
            .eq(PlatformGbStream::getPlatformId, platform.getServerGbId())
            .in(PlatformGbStream::getCatalogId, catalogIdList)
            .in(PlatformGbStream::getGbStreamId, gbIdList)
            .deleteCount();
        VideoService.getPlatformCatalogService()
            .handleCatalogEvent(CatalogEventConstant.DEL, platform.getServerGbId(), null, toProtocolStreams(sharedStreams), null);
    }

    /**
     * 查询当前用户可见的通道，非超级管理员不可见管理员专属设备下的通道。
     *
     * @param channelIds 通道国标编号过滤条件，为空时不过滤
     * @return 可见通道列表
     */
    private List<DeviceChannel> queryVisibleChannels(List<String> channelIds) {
        List<Integer> flags = LoginHelper.isSuperAdmin() ? List.of(0, 1) : List.of(0);
        List<String> deviceIds = deviceMapper.lambda()
            .select(Device::getDeviceId)
            .in(Device::getHasAdministrator, flags)
            .list()
            .stream()
            .map(Device::getDeviceId)
            .toList();
        if (deviceIds.isEmpty()) {
            return List.of();
        }
        boolean filterChannel = channelIds != null && !channelIds.isEmpty();
        return deviceChannelMapper.lambda()
            .in(DeviceChannel::getDeviceId, deviceIds)
            .in(filterChannel, DeviceChannel::getChannelId, channelIds)
            .list();
    }

    /**
     * 组装绑定回显编号：通道国标编号与所属设备国标编号。
     *
     * @param channels 通道列表
     * @return 树形控件可用的编号集合
     */
    private static List<String> channelBindKeys(List<DeviceChannel> channels) {
        List<String> keys = channels.stream().map(DeviceChannel::getChannelId).collect(Collectors.toList());
        keys.addAll(channels.stream().map(DeviceChannel::getDeviceId).distinct().toList());
        return keys;
    }

    /**
     * 解析目录范围：包含子目录时展开全部后代目录编号，否则只取当前目录。
     *
     * @param platformId 上级平台国标编号
     * @param catalogId  目录国标编号
     * @param isSub      是否包含子目录
     * @return 目录编号列表
     */
    private List<String> resolveCatalogIds(String platformId, String catalogId, Integer isSub) {
        if (!isYes(isSub)) {
            return List.of(catalogId);
        }
        List<PlatformCatalog> catalogs = platformCatalogMapper.lambda()
            .select(PlatformCatalog::getId, PlatformCatalog::getParentId)
            .eq(PlatformCatalog::getPlatformId, platformId)
            .list();
        Map<String, List<String>> childrenMap = catalogs.stream()
            .collect(Collectors.groupingBy(catalog -> StrUtil.nullToEmpty(catalog.getParentId()),
                Collectors.mapping(PlatformCatalog::getId, Collectors.toList())));
        List<String> catalogIdList = new ArrayList<>();
        collectCatalogIds(catalogId, childrenMap, catalogIdList);
        if (catalogIdList.isEmpty()) {
            throw new ServiceException("当前目录不存在");
        }
        return catalogIdList;
    }

    /**
     * 递归收集目录及其后代编号。
     *
     * @param catalogId   当前目录编号
     * @param childrenMap 父级编号到子级编号集合
     * @param catalogIds  收集结果
     */
    private static void collectCatalogIds(String catalogId, Map<String, List<String>> childrenMap, List<String> catalogIds) {
        catalogIds.add(catalogId);
        for (String childId : childrenMap.getOrDefault(catalogId, List.of())) {
            collectCatalogIds(childId, childrenMap, catalogIds);
        }
    }

    /**
     * 查询上级平台，不存在时中断操作。
     *
     * @param platformId 上级平台国标编号
     * @return 上级平台视图
     */
    private ParentPlatformVo requirePlatform(String platformId) {
        ParentPlatformVo platform = VideoService.getParentPlatformService().getParentPlatformByServerGbId(platformId);
        if (platform == null) {
            throw new ServiceException("未获取上级平台信息");
        }
        return platform;
    }

    /**
     * 查询可承载共享关系的目录，平台根目录直接返回构造的虚拟节点。
     *
     * @param platformId 上级平台国标编号
     * @param catalogId  目录国标编号
     */
    private void requireCatalog(String platformId, String catalogId) {
        if (platformId.equals(catalogId)) {
            return;
        }
        PlatformCatalog catalog = platformCatalogMapper.selectById(catalogId);
        if (catalog == null || !platformId.equals(catalog.getPlatformId())) {
            throw new ServiceException("当前目录不存在");
        }
    }

    /**
     * 按编号解析目录，编号为上级平台国标编号时返回平台根目录节点。
     *
     * @param catalogId 目录或上级平台国标编号
     * @return 目录节点，其编号与所属平台编号保持一致
     */
    private PlatformCatalog requireCatalogOrRoot(String catalogId) {
        PlatformCatalog catalog = platformCatalogMapper.selectById(catalogId);
        if (catalog != null) {
            return catalog;
        }
        ParentPlatformVo platform = requirePlatform(catalogId);
        PlatformCatalog root = new PlatformCatalog();
        root.setId(platform.getServerGbId());
        root.setPlatformId(platform.getServerGbId());
        root.setParentId(platform.getServerGbId());
        return root;
    }

    /**
     * 校验非全量操作时必须携带编号集合。
     *
     * @param bo 绑定入参
     * @return 编号集合，全量操作返回 null
     */
    private static List<String> requireSelectedIds(VideoPlatformShareBo bo) {
        if (isYes(bo.getIsAll())) {
            return null;
        }
        if (bo.getGbIdList() == null || bo.getGbIdList().isEmpty()) {
            throw new ServiceException("请选择需要操作的数据");
        }
        return bo.getGbIdList();
    }

    /**
     * 提取关联记录的业务编号集合。
     *
     * @param relations 关联记录
     * @param keyGetter 业务编号取值
     * @param <T>       关联记录类型
     * @return 业务编号列表
     */
    private static <T> List<String> bindIds(List<T> relations, Function<T, String> keyGetter) {
        return relations.stream().map(keyGetter).toList();
    }

    /**
     * 组装关联记录中「业务编号 → 目录编号」的映射。
     *
     * @param relations   关联记录
     * @param keyGetter   业务编号取值
     * @param catalogGetter 目录编号取值
     * @param <T>         关联记录类型
     * @return 编号到目录的映射
     */
    private static <T> Map<String, String> bindCatalogMap(List<T> relations, Function<T, String> keyGetter,
                                                          Function<T, String> catalogGetter) {
        Map<String, String> catalogMap = new HashMap<>();
        for (T relation : relations) {
            catalogMap.put(keyGetter.apply(relation), catalogGetter.apply(relation));
        }
        return catalogMap;
    }

    /**
     * 判断标记是否为「是」。
     *
     * @param flag 标记值
     * @return 为空或非 1 时返回 false
     */
    private static boolean isYes(Integer flag) {
        return flag != null && flag == ConstEnum.Flag.YES.getValue();
    }

    /**
     * 将设备通道转换为协议侧视图，用于向上级平台推送目录事件。
     *
     * @param channels 通道实体或视图集合
     * @return 协议侧通道视图集合
     */
    private static List<cn.com.nla.common.video.basic.vo.video.DeviceChannelVo> toProtocolChannels(Collection<?> channels) {
        return BeanUtil.copyToList(channels, cn.com.nla.common.video.basic.vo.video.DeviceChannelVo.class);
    }

    /**
     * 将国标直播流转换为协议侧视图，用于向上级平台推送目录事件。
     *
     * @param streams 直播流实体或视图集合
     * @return 协议侧直播流视图集合
     */
    private static List<cn.com.nla.common.video.basic.vo.video.GbStreamVo> toProtocolStreams(Collection<?> streams) {
        return BeanUtil.copyToList(streams, cn.com.nla.common.video.basic.vo.video.GbStreamVo.class);
    }
}
