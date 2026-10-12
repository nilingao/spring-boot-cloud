package cn.com.nla.video.spi;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.vo.video.StreamPushVo;
import cn.com.nla.common.video.core.service.video.StreamPushVoService;
import cn.com.nla.video.domain.StreamPush;
import cn.com.nla.video.mapper.StreamPushMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 推流信息协议侧实现：登记第三方推流并维护其在线状态。
 *
 * <p>仅在 {@code video.enabled=true} 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class StreamPushVoServiceImpl extends StreamPushVoService {

    private final StreamPushMapper streamPushMapper;
    private final PlatformShareHelper platformShareHelper;

    /**
     * 按应用名与流编号查询推流记录。
     *
     * @param app    流应用名
     * @param stream 流编号
     * @return 推流视图，未获取到返回 null
     */
    @Override
    public StreamPushVo findAppStream(String app, String stream) {
        if (StringUtils.isEmpty(app) || StringUtils.isEmpty(stream)) {
            log.info("[推流信息] 未获取到 app:{},stream:{} ", app, stream);
            return null;
        }
        return VideoSpiConvert.to(streamPushMapper.lambda()
            .eq(StreamPush::getApp, app)
            .eq(StreamPush::getStream, stream)
            .one(), StreamPushVo.class);
    }

    /**
     * 查询挂在指定流媒体下且未共享给上级平台的推流记录。
     *
     * @param mediaServiceId 流媒体服务编号
     * @return 推流列表
     */
    @Override
    public List<StreamPushVo> findMediaServiceNotGbId(String mediaServiceId) {
        if (StringUtils.isEmpty(mediaServiceId)) {
            log.info("[推流信息] 未获取到 mediaServerId 信息");
            return List.of();
        }
        return VideoSpiConvert.toList(streamPushMapper.selectNotSharedByMediaServer(mediaServiceId),
            StreamPushVo.class);
    }

    /**
     * 新增推流记录。
     *
     * @param param 推流视图
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int insert(StreamPushVo param) {
        if (StringUtils.isEmpty(param.getApp()) || StringUtils.isEmpty(param.getStream())) {
            log.info("[推流信息] 未获取到 app:{},stream:{} ", param.getApp(), param.getStream());
            return ConstEnum.Flag.NO.getValue();
        }
        boolean saved = streamPushMapper.insert(VideoSpiConvert.to(param, StreamPush.class)) > 0;
        return saved ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 按应用名与流编号更新推流记录。
     *
     * @param param 推流视图
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int update(StreamPushVo param) {
        return updateStreamPush(param);
    }

    /**
     * 按应用名与流编号更新推流记录，供流变化回调复用。
     *
     * @param param 推流视图
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int updateStreamPush(StreamPushVo param) {
        if (StringUtils.isEmpty(param.getApp()) || StringUtils.isEmpty(param.getStream())) {
            log.info("[推流信息] 未获取到 app:{},stream:{} ", param.getApp(), param.getStream());
            return ConstEnum.Flag.NO.getValue();
        }
        boolean updated = streamPushMapper.lambda()
            .eq(StreamPush::getApp, param.getApp())
            .eq(StreamPush::getStream, param.getStream())
            .update(VideoSpiConvert.to(param, StreamPush.class));
        return updated ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 批量更新某个流媒体节点下全部推流的在线状态。
     *
     * @param mediaServiceId 流媒体服务编号
     * @param status         是否在线
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int updateStatus(String mediaServiceId, boolean status) {
        if (StringUtils.isEmpty(mediaServiceId)) {
            log.info("[推流信息] 未获取到 mediaServerId 信息");
            return ConstEnum.Flag.NO.getValue();
        }
        boolean updated = streamPushMapper.lambda()
            .set(StreamPush::getStatus, status ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue())
            .eq(StreamPush::getMediaServerId, mediaServiceId)
            .update();
        return updated ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 更新指定推流的在线状态。
     *
     * @param app    流应用名
     * @param stream 流编号
     * @param status 是否在线
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int updateStatus(String app, String stream, boolean status) {
        if (StringUtils.isEmpty(app) || StringUtils.isEmpty(stream)) {
            log.info("[推流信息] 未获取到 app:{},stream:{} ", app, stream);
            return ConstEnum.Flag.NO.getValue();
        }
        boolean updated = streamPushMapper.lambda()
            .set(StreamPush::getStatus, status ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue())
            .eq(StreamPush::getApp, app)
            .eq(StreamPush::getStream, stream)
            .update();
        return updated ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 删除推流记录并清理其在上级平台的共享记录。
     *
     * @param app    流应用名
     * @param stream 流编号
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int del(String app, String stream) {
        if (StringUtils.isEmpty(app) || StringUtils.isEmpty(stream)) {
            log.info("[推流信息] 未获取到 app:{},stream:{} ", app, stream);
            return ConstEnum.Flag.NO.getValue();
        }
        platformShareHelper.removeStreamRel(app, stream);
        boolean removed = streamPushMapper.lambda()
            .eq(StreamPush::getApp, app)
            .eq(StreamPush::getStream, stream)
            .deleteCount() > 0;
        return removed ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }
}
