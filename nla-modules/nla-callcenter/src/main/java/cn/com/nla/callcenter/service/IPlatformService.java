package cn.com.nla.callcenter.service;

import java.util.List;
import cn.com.nla.callcenter.domain.bo.PlatformBo;
import cn.com.nla.callcenter.domain.vo.PlatformVo;
import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;

import java.util.Collection;

/**
 * FreeSWITCH 平台信息 Service 接口。
 *
 * @author TZY
 */
public interface IPlatformService {

    /**
     * 查询平台详情。
     *
     * @param id 平台编号
     * @return 平台视图对象
     */
    PlatformVo queryById(Long id);

    /**
     * 分页查询平台列表。
     *
     * @param bo        查询条件(名称/内外网 IP 模糊, 启用与在线状态)
     * @param pageQuery 分页参数
     * @return 平台分页列表
     */
    List<PlatformVo> queryList(PlatformBo bo);

    PageResult<PlatformVo> queryPageList(PlatformBo bo, PageQuery pageQuery);

    /**
     * 新增平台, 校验 RTP 端口区间与名称唯一。
     *
     * @param bo 平台业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(PlatformBo bo);

    /**
     * 修改平台, 保留运行时在线状态不被覆盖。
     *
     * @param bo 平台业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(PlatformBo bo);

    /**
     * 切换平台启用标记(仅更新 enable 字段)。
     *
     * @param bo 仅使用 id 与 status 字段(写入 enable)
     * @return 是否更新成功
     */
    Boolean updateStatus(PlatformBo bo);

    /**
     * 批量删除平台, 启用中的平台要求先禁用。
     *
     * @param ids     待删除的平台编号集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
