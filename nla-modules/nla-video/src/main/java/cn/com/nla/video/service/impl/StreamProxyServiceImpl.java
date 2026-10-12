package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.video.domain.StreamProxy;
import cn.com.nla.video.domain.bo.StreamProxyBo;
import cn.com.nla.video.domain.vo.StreamProxyVo;
import cn.com.nla.video.mapper.StreamProxyMapper;
import cn.com.nla.video.service.IStreamProxyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 拉流代理 Service 实现：仅维护配置，实际启停由协议侧下发媒体服务器。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class StreamProxyServiceImpl implements IStreamProxyService {

    private final StreamProxyMapper streamProxyMapper;

    /**
     * 查询拉流代理详情。
     *
     * @param id 主键
     * @return 拉流代理视图对象
     */
    @Override
    public StreamProxyVo queryById(Long id) {
        return streamProxyMapper.selectVoById(id);
    }

    /**
     * 按流标识查询拉流代理配置。
     *
     * @param app    流应用名
     * @param stream 流Id
     * @return 拉流代理视图对象，不存在返回 null
     */
    @Override
    public StreamProxyVo queryByAppAndStream(String app, String stream) {
        return streamProxyMapper.lambda()
            .eq(StreamProxy::getApp, app)
            .eq(StreamProxy::getStream, stream)
            .voOne();
    }

    /**
     * 分页查询拉流代理列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 拉流代理分页列表
     */
    @Override
    public PageResult<StreamProxyVo> queryPageList(StreamProxyBo bo, PageQuery pageQuery) {
        Page<StreamProxyVo> result = streamProxyMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的拉流代理列表。
     *
     * @param bo 查询条件
     * @return 拉流代理列表
     */
    @Override
    public List<StreamProxyVo> queryList(StreamProxyBo bo) {
        return streamProxyMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构造拉流代理查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<StreamProxy> buildQueryWrapper(StreamProxyBo bo) {
        return QueryBuilder.lambda(StreamProxy.class)
            .eqIfPresent(StreamProxy::getType, bo.getType())
            .eqIfText(StreamProxy::getApp, bo.getApp())
            .eqIfText(StreamProxy::getStream, bo.getStream())
            .likeIfText(StreamProxy::getName, bo.getName())
            .eqIfPresent(StreamProxy::getStatus, bo.getStatus())
            .eqIfText(StreamProxy::getMediaServerId, bo.getMediaServerId())
            .eqIfPresent(StreamProxy::getEnable, bo.getEnable())
            .orderByDesc(StreamProxy::getId)
            .build();
    }

    /**
     * 新增拉流代理。
     *
     * @param bo 拉流代理业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(StreamProxyBo bo) {
        StreamProxy add = MapstructUtils.convert(bo, StreamProxy.class);
        validEntityBeforeSave(add, null);
        boolean flag = streamProxyMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改拉流代理。
     *
     * @param bo 拉流代理业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(StreamProxyBo bo) {
        StreamProxy update = MapstructUtils.convert(bo, StreamProxy.class);
        validEntityBeforeSave(update, update.getId());
        return streamProxyMapper.updateById(update) > 0;
    }

    /**
     * 保存前校验：应用名与流Id 组合在有效配置内不允许重复（编辑排除自身）。
     *
     * @param entity    待保存实体
     * @param excludeId 编辑时排除的主键，新增传 null
     */
    private void validEntityBeforeSave(StreamProxy entity, Long excludeId) {
        var checker = streamProxyMapper.lambda()
            .eq(StreamProxy::getApp, entity.getApp())
            .eq(StreamProxy::getStream, entity.getStream());
        if (excludeId != null) {
            checker.ne(StreamProxy::getId, excludeId);
        }
        if (checker.exists()) {
            throw new ServiceException("该应用名与流Id 已存在拉流代理配置，请勿重复录入");
        }
    }

    /**
     * 批量删除拉流代理配置。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验（配置无下游引用）
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return streamProxyMapper.deleteByIds(ids) > 0;
    }
}
