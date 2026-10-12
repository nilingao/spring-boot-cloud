package cn.com.nla.video.spi;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.vo.video.StreamProxyVo;
import cn.com.nla.common.video.core.service.video.StreamProxyVoService;
import cn.com.nla.video.domain.StreamProxy;
import cn.com.nla.video.mapper.StreamProxyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 拉流代理协议侧实现：维护第三方流代理配置的启停与在线状态。
 *
 * <p>仅在 {@code video.enabled=true} 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class StreamProxyVoServiceImpl extends StreamProxyVoService {

    private final StreamProxyMapper streamProxyMapper;
    private final PlatformShareHelper platformShareHelper;

    /**
     * 按应用名与流编号查询拉流代理。
     *
     * @param app    流应用名
     * @param stream 流编号
     * @return 拉流代理视图，未获取到返回 null
     */
    @Override
    public StreamProxyVo findAppStream(String app, String stream) {
        if (StringUtils.isEmpty(app) || StringUtils.isEmpty(stream)) {
            log.info("[拉流信息] 未获取到 app:{},stream:{} ", app, stream);
            return null;
        }
        return VideoSpiConvert.to(streamProxyMapper.lambda()
            .eq(StreamProxy::getApp, app)
            .eq(StreamProxy::getStream, stream)
            .one(), StreamProxyVo.class);
    }

    /**
     * 更新拉流代理启用状态。
     *
     * @param id    拉流代理主键
     * @param flag  启用标记
     */
    @Override
    public void updateEnable(Long id, ConstEnum.Flag flag) {
        if (id == null || flag == null) {
            log.info("[拉流信息] 未获取到 id:{},flag:{} ", id, flag);
            return;
        }
        StreamProxy update = new StreamProxy();
        update.setId(id);
        update.setEnable(flag.getValue());
        streamProxyMapper.updateById(update);
    }

    /**
     * 更新指定拉流代理的在线状态，可同步切换所属流媒体节点。
     *
     * @param app           流应用名
     * @param stream        流编号
     * @param mediaServerId 流媒体服务编号，为空时不更新
     * @param status        是否在线
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int updateStatus(String app, String stream, String mediaServerId, boolean status) {
        if (StringUtils.isEmpty(app) || StringUtils.isEmpty(stream)) {
            log.info("[拉流信息] 未获取到 app:{},stream:{} ", app, stream);
            return ConstEnum.Flag.NO.getValue();
        }
        boolean updated = streamProxyMapper.lambda()
            .set(StreamProxy::getStatus, status ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue())
            .setIfText(StreamProxy::getMediaServerId, mediaServerId)
            .eq(StreamProxy::getApp, app)
            .eq(StreamProxy::getStream, stream)
            .update();
        return updated ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 批量更新某个流媒体节点下全部拉流代理的在线状态。
     *
     * @param mediaServerId 流媒体服务编号
     * @param status        是否在线
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int updateStatus(String mediaServerId, boolean status) {
        if (StringUtils.isEmpty(mediaServerId)) {
            log.info("[拉流信息] 未获取到 mediaServerId 信息");
            return ConstEnum.Flag.NO.getValue();
        }
        boolean updated = streamProxyMapper.lambda()
            .set(StreamProxy::getStatus, status ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue())
            .eq(StreamProxy::getMediaServerId, mediaServerId)
            .update();
        return updated ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 查询无人观看时允许自动移除的拉流代理。
     *
     * @param mediaServerId 流媒体服务编号
     * @return 拉流代理列表
     */
    @Override
    public List<StreamProxyVo> findAutoRemoveMediaServerIdList(String mediaServerId) {
        if (StringUtils.isEmpty(mediaServerId)) {
            log.info("[拉流信息] 未获取到 mediaServerId 信息");
            return List.of();
        }
        return VideoSpiConvert.toList(streamProxyMapper.lambda()
            .eq(StreamProxy::getMediaServerId, mediaServerId)
            .eq(StreamProxy::getEnableRemoveNoneReader, ConstEnum.Flag.YES.getValue())
            .list(), StreamProxyVo.class);
    }

    /**
     * 查询某个流媒体节点下已启用且不会因无人观看而停用的拉流代理。
     *
     * @param mediaServerId 流媒体服务编号
     * @param enable        保留参数，历史实现固定按「已启用」过滤
     * @return 拉流代理列表
     */
    @Override
    public List<StreamProxyVo> findEnableInMediaServerList(String mediaServerId, boolean enable) {
        if (StringUtils.isEmpty(mediaServerId)) {
            log.info("[拉流信息] 未获取到 mediaServerId 信息");
            return List.of();
        }
        return VideoSpiConvert.toList(streamProxyMapper.lambda()
            .eq(StreamProxy::getMediaServerId, mediaServerId)
            .eq(StreamProxy::getEnable, ConstEnum.Flag.YES.getValue())
            .eq(StreamProxy::getEnableDisableNoneReader, ConstEnum.Flag.NO.getValue())
            .list(), StreamProxyVo.class);
    }

    /**
     * 删除拉流代理并清理其在上级平台的共享记录。
     *
     * @param app    流应用名
     * @param stream 流编号
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int del(String app, String stream) {
        if (StringUtils.isEmpty(app) || StringUtils.isEmpty(stream)) {
            log.info("[拉流信息] 未获取到 app:{},stream:{} ", app, stream);
            return ConstEnum.Flag.NO.getValue();
        }
        platformShareHelper.removeStreamRel(app, stream);
        boolean removed = streamProxyMapper.lambda()
            .eq(StreamProxy::getApp, app)
            .eq(StreamProxy::getStream, stream)
            .deleteCount() > 0;
        return removed ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }
}
