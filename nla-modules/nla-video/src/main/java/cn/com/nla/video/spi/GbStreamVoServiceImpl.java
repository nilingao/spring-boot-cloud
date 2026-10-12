package cn.com.nla.video.spi;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.GbStreamVo;
import cn.com.nla.common.video.core.properties.VideoProperties;
import cn.com.nla.common.video.core.service.video.GbStreamVoService;
import cn.com.nla.video.domain.GbStream;
import cn.com.nla.video.mapper.DeviceChannelMapper;
import cn.com.nla.video.mapper.GbStreamMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 国标流协议侧实现：维护直播流与上级平台目录的关联关系。
 *
 * <p>仅在 {@code video.enabled=true} 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class GbStreamVoServiceImpl extends GbStreamVoService {

    private final GbStreamMapper gbStreamMapper;
    private final DeviceChannelMapper deviceChannelMapper;
    private final VideoProperties videoProperties;

    /**
     * 查询已共享给上级平台的国标流。
     *
     * @param platformId 上级平台国标编号
     * @param gbId       国标流编号
     * @return 国标流视图，未获取到返回 null
     */
    @Override
    public GbStreamVo findPlatformId(String platformId, String gbId) {
        return VideoSpiConvert.to(gbStreamMapper.selectStreamByPlatformGbId(platformId, gbId), GbStreamVo.class);
    }

    /**
     * 按应用名与流编号查询国标流。
     *
     * @param app     流应用名
     * @param steamId 流编号
     * @return 国标流视图，未获取到返回 null
     */
    @Override
    public GbStreamVo findAppStream(String app, String steamId) {
        GbStream gbStream = gbStreamMapper.lambda().eq(GbStream::getApp, app).eq(GbStream::getStream, steamId).one();
        if (gbStream == null) {
            log.info("[直播流相关] 未获取到直播流相关信息");
            return null;
        }
        return VideoSpiConvert.to(gbStream, GbStreamVo.class);
    }

    /**
     * 删除国标流。
     *
     * @param app     流应用名
     * @param steamId 流编号
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int delAppStream(String app, String steamId) {
        if (StringUtils.isEmpty(app) || StringUtils.isEmpty(steamId)) {
            return ConstEnum.Flag.NO.getValue();
        }
        boolean removed = gbStreamMapper.lambda()
            .eq(GbStream::getApp, app)
            .eq(GbStream::getStream, steamId)
            .deleteCount() > 0;
        return removed ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 按应用名与流编号更新国标流信息。
     *
     * @param param 国标流视图
     * @return 成功返回 1，失败返回 0
     */
    @Override
    public int update(GbStreamVo param) {
        if (StringUtils.isEmpty(param.getApp()) || StringUtils.isEmpty(param.getStream())) {
            return ConstEnum.Flag.NO.getValue();
        }
        boolean updated = gbStreamMapper.lambda()
            .eq(GbStream::getApp, param.getApp())
            .eq(GbStream::getStream, param.getStream())
            .update(VideoSpiConvert.to(param, GbStream.class));
        return updated ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue();
    }

    /**
     * 查询上级平台目录下的国标流，结果以通道节点形式返回。
     *
     * @param serverGbId 上级平台国标编号
     * @param gbId       国标流编号，为空时返回平台全部国标流
     * @return 通道形式的国标流列表
     */
    @Override
    public List<DeviceChannelVo> queryGbStreamListInPlatform(String serverGbId, String gbId) {
        return deviceChannelMapper.selectGbStreamChannelListInPlatform(serverGbId, gbId,
            Boolean.TRUE.equals(videoProperties.getUsePushingAsStatus()));
    }
}
