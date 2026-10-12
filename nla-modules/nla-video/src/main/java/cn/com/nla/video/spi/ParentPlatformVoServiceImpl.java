package cn.com.nla.video.spi;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.vo.sip.ChannelSourceInfo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.service.video.ParentPlatformVoService;
import cn.com.nla.video.domain.ParentPlatform;
import cn.com.nla.video.domain.PlatformGbChannel;
import cn.com.nla.video.mapper.ParentPlatformMapper;
import cn.com.nla.video.mapper.PlatformGbChannelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 上级平台协议侧实现：维护国标级联平台的查询、状态与共享关系。
 *
 * <p>仅在 {@code video.enabled=true} 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class ParentPlatformVoServiceImpl extends ParentPlatformVoService {

    private final ParentPlatformMapper parentPlatformMapper;
    private final PlatformGbChannelMapper platformGbChannelMapper;
    private final PlatformShareHelper platformShareHelper;

    /**
     * 按上级平台国标编号查询平台。
     *
     * @param platformGbId 上级平台国标编号
     * @return 平台视图，未获取到返回 null
     */
    @Override
    public ParentPlatformVo getParentPlatformByServerGbId(String platformGbId) {
        if (StringUtils.isEmpty(platformGbId)) {
            log.info("[上级平台] ： 未获取上级平台编号");
            return null;
        }
        ParentPlatform platform = parentPlatformMapper.lambda().eq(ParentPlatform::getServerGbId, platformGbId).one();
        if (platform == null) {
            return null;
        }
        return VideoSpiConvert.to(platform, ParentPlatformVo.class);
    }

    /**
     * 按本级平台国标编号查询级联到它的上级平台（仅启用项）。
     *
     * @param deviceGbId 本级平台国标编号
     * @return 平台列表，未获取到返回 null
     */
    @Override
    public List<ParentPlatformVo> getParentPlatformByDeviceGbId(String deviceGbId) {
        if (StringUtils.isEmpty(deviceGbId)) {
            log.info("[上级平台] ： 未获取注册平台编号");
            return null;
        }
        List<ParentPlatform> list = parentPlatformMapper.lambda().eq(ParentPlatform::getDeviceGbId, deviceGbId).list();
        if (list.isEmpty()) {
            log.info("[上级平台] ： 当前平台{}没有添加级联平台", deviceGbId);
            return null;
        }
        return VideoSpiConvert.toList(
            list.stream().filter(o -> Objects.equals(o.getEnable(), ConstEnum.Flag.YES.getValue())).toList(),
            ParentPlatformVo.class);
    }

    /**
     * 按上级平台国标编号集合批量查询平台。
     *
     * @param platformGbId 上级平台国标编号集合
     * @return 平台列表，未获取到返回 null
     */
    @Override
    public List<ParentPlatformVo> getParentPlatformByServerGbIdList(List<String> platformGbId) {
        if (platformGbId == null || platformGbId.isEmpty()) {
            log.info("[上级平台] ： 未获取上级平台编号");
            return null;
        }
        List<ParentPlatform> list = parentPlatformMapper.lambda()
            .in(ParentPlatform::getServerGbId, platformGbId)
            .list();
        if (list.isEmpty()) {
            log.info("[上级平台] ： 未获取到上级平台信息");
            return null;
        }
        return VideoSpiConvert.toList(list, ParentPlatformVo.class);
    }

    /**
     * 更新上级平台在线状态。
     *
     * @param platformGbID 上级平台国标编号
     * @param online       是否在线
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int updateParentPlatformStatus(String platformGbID, boolean online) {
        if (StringUtils.isEmpty(platformGbID)) {
            log.info("[上级平台] ： 未获取上级平台编号");
            return ConstEnum.Flag.NO.getValue();
        }
        boolean updated = parentPlatformMapper.lambda()
            .set(ParentPlatform::getStatus, online ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue())
            .eq(ParentPlatform::getServerGbId, platformGbID)
            .update();
        return updated ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 查询全部已启用的上级平台。
     *
     * @return 平台列表，未获取到返回 null
     */
    @Override
    public List<ParentPlatformVo> queryEnableParentPlatformList() {
        List<ParentPlatform> list = parentPlatformMapper.lambda()
            .eq(ParentPlatform::getEnable, ConstEnum.Flag.YES.getValue())
            .list();
        if (list.isEmpty()) {
            log.info("[上级平台] ： 未获取到上级平台信息");
            return null;
        }
        return VideoSpiConvert.toList(list, ParentPlatformVo.class);
    }

    /**
     * 查询全部已启用且可作为消息通道的上级平台。
     *
     * @return 平台列表，未获取到返回 null
     */
    @Override
    public List<ParentPlatformVo> queryEnablePlatformListWithAsMessageChannel() {
        List<ParentPlatform> list = parentPlatformMapper.lambda()
            .eq(ParentPlatform::getEnable, ConstEnum.Flag.YES.getValue())
            .eq(ParentPlatform::getAsMessageChannel, ConstEnum.Flag.YES.getValue())
            .list();
        if (list.isEmpty()) {
            log.info("[上级平台] ： 未获取到上级平台信息");
            return null;
        }
        return VideoSpiConvert.toList(list, ParentPlatformVo.class);
    }

    /**
     * 统计国标编号在上级平台下的通道来源与流来源。
     *
     * @param serverGbId 上级平台国标编号
     * @param channelId  国标通道编号
     * @return 通道来源统计列表
     */
    @Override
    public List<ChannelSourceInfo> getChannelSource(String serverGbId, String channelId) {
        return parentPlatformMapper.selectChannelSource(serverGbId, channelId);
    }

    /**
     * 查询共享了指定通道的上级平台。
     *
     * @param channelId     通道国标编号
     * @param allPlatformId 待过滤的平台国标编号集合
     * @return 平台列表，未获取到返回 null
     */
    @Override
    public List<ParentPlatformVo> queryPlatFormListForGBWithGBId(String channelId, List<String> allPlatformId) {
        if (StringUtils.isEmpty(channelId) || allPlatformId == null || allPlatformId.isEmpty()) {
            log.info("[上级平台] ： 信息不存在 channelId :{},  allPlatformId :{}", channelId, allPlatformId);
            return null;
        }
        List<ParentPlatform> list = parentPlatformMapper.selectPlatformListForChannel(channelId, allPlatformId);
        if (list.isEmpty()) {
            log.info("[上级平台] ： 未获取到上级平台信息");
            return null;
        }
        return VideoSpiConvert.toList(list, ParentPlatformVo.class);
    }

    /**
     * 查询共享了指定国标流的上级平台。
     *
     * @param app           流应用名
     * @param stream        流编号
     * @param allPlatformId 待过滤的平台国标编号集合，为空时不做过滤
     * @return 平台列表，未获取到返回 null
     */
    @Override
    public List<ParentPlatformVo> queryPlatFormListForStreamWithGBId(String app, String stream,
        List<String> allPlatformId) {
        if (StringUtils.isEmpty(app) || StringUtils.isEmpty(stream)) {
            log.info("[上级平台] ： 信息不存在 app :{}, stream :{}", app, stream);
            return null;
        }
        List<ParentPlatform> list = parentPlatformMapper.selectPlatformListForStream(app, stream, allPlatformId);
        if (list.isEmpty()) {
            log.info("[上级平台] ： 未获取到上级平台信息");
            return null;
        }
        return VideoSpiConvert.toList(list, ParentPlatformVo.class);
    }

    /**
     * 移除国标流在全部上级平台的共享记录。
     *
     * @param app    流应用名
     * @param stream 流编号
     */
    @Override
    public void delPlatformGbStream(String app, String stream) {
        platformShareHelper.removeStreamRel(app, stream);
    }

    /**
     * 查询共享了指定通道的上级平台。
     *
     * @param channelId 通道国标编号
     * @return 平台列表，未获取到返回空列表
     */
    @Override
    public List<ParentPlatformVo> findPlatformGbChannel(String channelId) {
        if (StringUtils.isEmpty(channelId)) {
            return List.of();
        }
        List<String> platformIds = platformGbChannelMapper.lambda()
            .select(PlatformGbChannel::getPlatformId)
            .eq(PlatformGbChannel::getDeviceChannelId, channelId)
            .list()
            .stream()
            .map(PlatformGbChannel::getPlatformId)
            .distinct()
            .toList();
        if (platformIds.isEmpty()) {
            return List.of();
        }
        return VideoSpiConvert.toList(
            parentPlatformMapper.lambda().in(ParentPlatform::getServerGbId, platformIds).list(),
            ParentPlatformVo.class);
    }
}
