package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.video.domain.Device;
import cn.com.nla.video.domain.GbStream;
import cn.com.nla.video.domain.StreamProxy;
import cn.com.nla.video.domain.StreamPush;
import cn.com.nla.video.domain.VideoMediaServer;
import cn.com.nla.video.domain.bo.VideoMediaServerBo;
import cn.com.nla.video.domain.vo.VideoMediaServerVo;
import cn.com.nla.video.mapper.DeviceMapper;
import cn.com.nla.video.mapper.GbStreamMapper;
import cn.com.nla.video.mapper.StreamProxyMapper;
import cn.com.nla.video.mapper.StreamPushMapper;
import cn.com.nla.video.mapper.VideoMediaServerMapper;
import cn.com.nla.video.service.IVideoMediaServerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 流媒体服务信息 Service 实现：仅维护节点配置，节点在线状态与自动配置由协议侧心跳回调更新。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VideoMediaServerServiceImpl implements IVideoMediaServerService {

    /**
     * 在线标记值，与库内 status 字段语义一致。
     */
    private static final int ONLINE = 1;

    /**
     * 启用标记值，与库内 enable 字段语义一致。
     */
    private static final int ENABLED = 1;

    private final VideoMediaServerMapper videoMediaServerMapper;
    private final DeviceMapper deviceMapper;
    private final GbStreamMapper gbStreamMapper;
    private final StreamProxyMapper streamProxyMapper;
    private final StreamPushMapper streamPushMapper;

    /**
     * 查询流媒体服务详情。
     *
     * @param id 流媒体服务编号
     * @return 流媒体服务视图对象
     */
    @Override
    public VideoMediaServerVo queryById(String id) {
        return videoMediaServerMapper.selectVoById(id);
    }

    /**
     * 分页查询流媒体服务列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 流媒体服务分页列表
     */
    @Override
    public PageResult<VideoMediaServerVo> queryPageList(VideoMediaServerBo bo, PageQuery pageQuery) {
        Page<VideoMediaServerVo> result = videoMediaServerMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的流媒体服务列表。
     *
     * @param bo 查询条件
     * @return 流媒体服务列表
     */
    @Override
    public List<VideoMediaServerVo> queryList(VideoMediaServerBo bo) {
        return videoMediaServerMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 查询已启用的流媒体服务列表。
     *
     * @return 启用的流媒体服务列表
     */
    @Override
    public List<VideoMediaServerVo> queryEnableList() {
        return videoMediaServerMapper.lambda().eq(VideoMediaServer::getEnable, ENABLED).voList();
    }

    /**
     * 查询已启用且在线的流媒体服务列表。
     *
     * @return 在线的流媒体服务列表
     */
    @Override
    public List<VideoMediaServerVo> queryOnlineList() {
        return videoMediaServerMapper.lambda()
            .eq(VideoMediaServer::getEnable, ENABLED)
            .eq(VideoMediaServer::getStatus, ONLINE)
            .voList();
    }

    /**
     * 构造流媒体服务查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<VideoMediaServer> buildQueryWrapper(VideoMediaServerBo bo) {
        return QueryBuilder.lambda(VideoMediaServer.class)
            .eqIfText(VideoMediaServer::getIp, bo.getIp())
            .eqIfText(VideoMediaServer::getStreamIp, bo.getStreamIp())
            .eqIfPresent(VideoMediaServer::getHttpPort, bo.getHttpPort())
            .eqIfPresent(VideoMediaServer::getEnable, bo.getEnable())
            .eqIfPresent(VideoMediaServer::getStatus, bo.getStatus())
            .orderByAsc(VideoMediaServer::getId)
            .build();
    }

    /**
     * 新增流媒体服务：主键为录入的节点编号，需校验非空且未被占用。
     *
     * @param bo 流媒体服务业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(VideoMediaServerBo bo) {
        VideoMediaServer add = MapstructUtils.convert(bo, VideoMediaServer.class);
        if (StringUtils.isBlank(add.getId())) {
            throw new ServiceException("流媒体服务编号不能为空");
        }
        if (videoMediaServerMapper.lambda().eq(VideoMediaServer::getId, add.getId()).exists()) {
            throw new ServiceException("流媒体服务编号已存在，请勿重复录入");
        }
        validEntityBeforeSave(add, null);
        return videoMediaServerMapper.insert(add) > 0;
    }

    /**
     * 修改流媒体服务。
     *
     * @param bo 流媒体服务业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(VideoMediaServerBo bo) {
        VideoMediaServer update = MapstructUtils.convert(bo, VideoMediaServer.class);
        if (StringUtils.isBlank(update.getId())) {
            throw new ServiceException("修改时必须指定流媒体服务编号");
        }
        validEntityBeforeSave(update, update.getId());
        return videoMediaServerMapper.updateById(update) > 0;
    }

    /**
     * 保存前校验：同一 IP 下的 HTTP 端口不允许重复（编辑排除自身）。
     *
     * <p>端口为空时库表唯一键不生效，跳过校验。</p>
     *
     * @param entity    待保存实体
     * @param excludeId 编辑时排除的编号，新增传 null
     */
    private void validEntityBeforeSave(VideoMediaServer entity, String excludeId) {
        if (entity.getHttpPort() == null) {
            return;
        }
        var checker = videoMediaServerMapper.lambda()
            .eq(VideoMediaServer::getIp, entity.getIp())
            .eq(VideoMediaServer::getHttpPort, entity.getHttpPort());
        if (StringUtils.isNotBlank(excludeId)) {
            checker.ne(VideoMediaServer::getId, excludeId);
        }
        if (checker.exists()) {
            throw new ServiceException("该 IP 下已存在相同 HTTP 端口的流媒体服务，请勿重复录入");
        }
    }

    /**
     * 批量删除流媒体服务：先校验设备、直播流、拉流代理与推流是否仍指向该节点。
     *
     * @param ids     待删除的流媒体服务编号集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        if (Boolean.TRUE.equals(isValid)) {
            boolean deviceReferenced = deviceMapper.lambda().in(Device::getMediaServerId, ids).exists();
            boolean gbStreamReferenced = gbStreamMapper.lambda().in(GbStream::getMediaServerId, ids).exists();
            boolean proxyReferenced = streamProxyMapper.lambda().in(StreamProxy::getMediaServerId, ids).exists();
            boolean pushReferenced = streamPushMapper.lambda().in(StreamPush::getMediaServerId, ids).exists();
            if (deviceReferenced) {
                throw new ServiceException("仍有设备绑定该流媒体服务，请先修改设备的流媒体配置");
            }
            if (gbStreamReferenced) {
                throw new ServiceException("仍有直播流绑定该流媒体服务，请先修改直播流的流媒体配置");
            }
            if (proxyReferenced || pushReferenced) {
                throw new ServiceException("仍有拉流代理或推流记录使用该流媒体服务，请先删除相关配置");
            }
        }
        return videoMediaServerMapper.deleteByIds(ids) > 0;
    }
}
