package cn.com.nla.callcenter.service.impl;

import java.util.List;
import cn.com.nla.callcenter.domain.Platform;
import cn.com.nla.callcenter.domain.bo.PlatformBo;
import cn.com.nla.callcenter.domain.vo.PlatformVo;
import cn.com.nla.callcenter.mapper.PlatformMapper;
import cn.com.nla.callcenter.service.IPlatformService;
import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * FreeSWITCH 平台信息 Service 实现: RTP 端口区间与名称唯一校验,
 * 在线状态由协议侧心跳维护, 修改与删除不覆盖运行时字段。
 * <p>
 * 自旧工程 {@code PlatformManageServiceImpl} 迁移。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PlatformServiceImpl implements IPlatformService {

    /**
     * 启用标记值, 与库内 enable 字段语义一致。
     */
    private static final int ENABLED = 1;

    private final PlatformMapper platformMapper;

    /**
     * 查询平台详情。
     *
     * @param id 平台编号
     * @return 平台视图对象
     */
    @Override
    public PlatformVo queryById(Long id) {
        return platformMapper.selectVoById(id);
    }

    /**
     * 分页查询平台列表。
     *
     * @param bo        查询条件(名称/内外网 IP 模糊, 启用与在线状态)
     * @param pageQuery 分页参数
     * @return 平台分页列表
     */
    @Override
    public List<PlatformVo> queryList(PlatformBo bo) {
        return platformMapper.selectVoList(buildQueryWrapper(bo));
    }

    public PageResult<PlatformVo> queryPageList(PlatformBo bo, PageQuery pageQuery) {
        Page<PlatformVo> result = platformMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 构造平台查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<Platform> buildQueryWrapper(PlatformBo bo) {
        return QueryBuilder.lambda(Platform.class)
            .likeIfText(Platform::getName, bo.getName())
            .likeIfText(Platform::getLocalIp, bo.getLocalIp())
            .likeIfText(Platform::getRemoteIp, bo.getRemoteIp())
            .eqIfPresent(Platform::getEnable, bo.getEnable())
            .eqIfPresent(Platform::getStatus, bo.getStatus())
            .orderByAsc(Platform::getId)
            .build();
    }

    /**
     * 新增平台, 校验 RTP 端口区间与名称唯一。
     *
     * @param bo 平台业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(PlatformBo bo) {
        return saveOrUpdateValidated(bo, false);
    }

    /**
     * 修改平台, 保留运行时在线状态不被覆盖。
     *
     * @param bo 平台业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(PlatformBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改时必须指定平台编号");
        }
        return saveOrUpdateValidated(bo, true);
    }

    /**
     * 新增/修改共用的保存前校验与落库。
     *
     * @param bo       平台业务对象
     * @param isUpdate 是否为修改
     * @return 是否保存成功
     */
    private Boolean saveOrUpdateValidated(PlatformBo bo, boolean isUpdate) {
        // RTP 起止端口校验
        if (bo.getStartRtpPort() != null && bo.getEndRtpPort() != null
            && bo.getStartRtpPort() > bo.getEndRtpPort()) {
            throw new ServiceException("RTP起始端口不能大于结束端口");
        }
        if (isUpdate && platformMapper.selectById(bo.getId()) == null) {
            throw new ServiceException("平台不存在");
        }
        // 名称唯一性校验
        boolean nameDuplicate = platformMapper.lambda()
            .eq(Platform::getName, bo.getName())
            .ne(isUpdate, Platform::getId, bo.getId())
            .exists();
        if (nameDuplicate) {
            throw new ServiceException("平台名称已存在");
        }
        Platform entity = MapstructUtils.convert(bo, Platform.class);
        if (isUpdate) {
            // 保留运行时 status，不允许通过保存覆盖
            entity.setStatus(null);
            return platformMapper.updateById(entity) > 0;
        }
        return platformMapper.insert(entity) > 0;
    }

    /**
     * 切换平台启用标记(仅更新 enable 字段)。
     *
     * @param bo 仅使用 id 与 status 字段(写入 enable)
     * @return 是否更新成功
     */
    @Override
    public Boolean updateStatus(PlatformBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("必须指定平台编号");
        }
        if (platformMapper.selectById(bo.getId()) == null) {
            throw new ServiceException("平台不存在");
        }
        Platform update = new Platform();
        update.setId(bo.getId());
        update.setEnable(bo.getStatus());
        return platformMapper.updateById(update) > 0;
    }

    /**
     * 批量删除平台, 启用中的平台要求先禁用。
     *
     * @param ids     待删除的平台编号集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        if (Boolean.TRUE.equals(isValid)) {
            for (Long id : ids) {
                Platform platform = platformMapper.selectById(id);
                if (platform == null) {
                    continue;
                }
                if (Integer.valueOf(ENABLED).equals(platform.getEnable())) {
                    throw new ServiceException("请先禁用平台后再删除");
                }
            }
        }
        return platformMapper.deleteByIds(ids) > 0;
    }

}
