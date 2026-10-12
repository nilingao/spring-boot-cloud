package cn.com.nla.video.spi;

import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.PlatformCatalogVo;
import cn.com.nla.common.video.core.service.video.PlatformCatalogVoService;
import cn.com.nla.video.mapper.PlatformCatalogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 国标级联目录协议侧实现：供目录订阅与目录事件推送回查目录节点。
 *
 * <p>仅在 {@code video.enabled=true} 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class PlatformCatalogVoServiceImpl extends PlatformCatalogVoService {

    private final PlatformCatalogMapper platformCatalogMapper;

    /**
     * 按目录编号查询目录详情。
     *
     * @param id 目录编号
     * @return 目录视图，未获取到返回 null
     */
    @Override
    public PlatformCatalogVo findId(String id) {
        return platformCatalogMapper.selectCatalogDetail(id);
    }

    /**
     * 按目录编号查询目录详情，目录编号与通道编号在同一命名空间内。
     *
     * @param channelId 目录编号
     * @return 目录视图，未获取到返回 null
     */
    @Override
    public PlatformCatalogVo findChannelId(String channelId) {
        return platformCatalogMapper.selectCatalogDetail(channelId);
    }

    /**
     * 查询上级平台下的目录节点，结果以通道形式返回，供目录树推送。
     *
     * @param serverGbId 上级平台国标编号
     * @return 通道形式的目录列表
     */
    @Override
    public List<DeviceChannelVo> queryCatalogInPlatform(String serverGbId) {
        return platformCatalogMapper.selectCatalogChannelListInPlatform(serverGbId);
    }
}
