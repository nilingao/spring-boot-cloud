package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.video.domain.StreamPush;
import cn.com.nla.video.domain.bo.StreamPushBo;
import cn.com.nla.video.domain.vo.StreamPushVo;
import cn.com.nla.video.mapper.StreamPushMapper;
import cn.com.nla.video.service.IStreamPushService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 推流流信息 Service 实现：媒体服务器回调写入的流登记，管理端可查询与清理。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class StreamPushServiceImpl implements IStreamPushService {

    private final StreamPushMapper streamPushMapper;

    /**
     * 查询推流详情。
     *
     * @param id 主键
     * @return 推流视图对象
     */
    @Override
    public StreamPushVo queryById(Long id) {
        return streamPushMapper.selectVoById(id);
    }

    /**
     * 按流标识查询推流信息。
     *
     * @param app    流应用名
     * @param stream 流Id
     * @return 推流视图对象，不存在返回 null
     */
    @Override
    public StreamPushVo queryByAppAndStream(String app, String stream) {
        return streamPushMapper.lambda()
            .eq(StreamPush::getApp, app)
            .eq(StreamPush::getStream, stream)
            .voOne();
    }

    /**
     * 分页查询推流列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 推流分页列表
     */
    @Override
    public PageResult<StreamPushVo> queryPageList(StreamPushBo bo, PageQuery pageQuery) {
        Page<StreamPushVo> result = streamPushMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的推流列表。
     *
     * @param bo 查询条件
     * @return 推流列表
     */
    @Override
    public List<StreamPushVo> queryList(StreamPushBo bo) {
        return streamPushMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构造推流查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<StreamPush> buildQueryWrapper(StreamPushBo bo) {
        return QueryBuilder.lambda(StreamPush.class)
            .eqIfText(StreamPush::getApp, bo.getApp())
            .eqIfText(StreamPush::getStream, bo.getStream())
            .eqIfText(StreamPush::getMediaServerId, bo.getMediaServerId())
            .eqIfText(StreamPush::getServerId, bo.getServerId())
            .eqIfPresent(StreamPush::getStatus, bo.getStatus())
            .orderByDesc(StreamPush::getId)
            .build();
    }

    /**
     * 新增推流记录。
     *
     * @param bo 推流业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(StreamPushBo bo) {
        StreamPush add = MapstructUtils.convert(bo, StreamPush.class);
        validEntityBeforeSave(add, null);
        boolean flag = streamPushMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改推流记录。
     *
     * @param bo 推流业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(StreamPushBo bo) {
        StreamPush update = MapstructUtils.convert(bo, StreamPush.class);
        validEntityBeforeSave(update, update.getId());
        return streamPushMapper.updateById(update) > 0;
    }

    /**
     * 保存前校验：应用名与流Id 组合在有效推流内不允许重复（编辑排除自身）。
     *
     * @param entity    待保存实体
     * @param excludeId 编辑时排除的主键，新增传 null
     */
    private void validEntityBeforeSave(StreamPush entity, Long excludeId) {
        var checker = streamPushMapper.lambda()
            .eq(StreamPush::getApp, entity.getApp())
            .eq(StreamPush::getStream, entity.getStream());
        if (excludeId != null) {
            checker.ne(StreamPush::getId, excludeId);
        }
        if (checker.exists()) {
            throw new ServiceException("该应用名与流Id 已存在推流记录，请勿重复录入");
        }
    }

    /**
     * 批量删除推流记录。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验（推流记录无下游引用）
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return streamPushMapper.deleteByIds(ids) > 0;
    }
}
