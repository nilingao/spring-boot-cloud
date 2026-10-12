package cn.com.nla.video.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.video.domain.bo.GbStreamBo;
import cn.com.nla.video.domain.vo.GbStreamVo;

import java.util.Collection;
import java.util.List;

/**
 * 国标直播流 Service 接口。
 *
 * @author TZY
 */
public interface IGbStreamService {

    /**
     * 查询直播流详情。
     *
     * @param gbStreamId 主键
     * @return 直播流视图对象
     */
    GbStreamVo queryById(Long gbStreamId);

    /**
     * 按流国标编号查询直播流详情。
     *
     * @param gbId 流国标编号
     * @return 直播流视图对象，不存在返回 null
     */
    GbStreamVo queryByGbId(String gbId);

    /**
     * 分页查询直播流列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 直播流分页列表
     */
    PageResult<GbStreamVo> queryPageList(GbStreamBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的直播流列表。
     *
     * @param bo 查询条件
     * @return 直播流列表
     */
    List<GbStreamVo> queryList(GbStreamBo bo);

    /**
     * 新增直播流，流国标编号不允许重复。
     *
     * @param bo 直播流业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(GbStreamBo bo);

    /**
     * 修改直播流，流国标编号不允许与其他流重复。
     *
     * @param bo 直播流业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(GbStreamBo bo);

    /**
     * 校验并批量删除直播流，已被平台级联引用的流不允许删除。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
