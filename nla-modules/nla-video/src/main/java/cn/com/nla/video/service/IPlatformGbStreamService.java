package cn.com.nla.video.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.video.domain.bo.PlatformGbStreamBo;
import cn.com.nla.video.domain.vo.PlatformGbStreamVo;

import java.util.Collection;
import java.util.List;

/**
 * 上级平台目录与直播流级联关系 Service 接口。
 *
 * @author TZY
 */
public interface IPlatformGbStreamService {

    /**
     * 查询级联关系详情。
     *
     * @param id 主键
     * @return 级联关系视图对象
     */
    PlatformGbStreamVo queryById(Long id);

    /**
     * 分页查询级联关系列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 级联关系分页列表
     */
    PageResult<PlatformGbStreamVo> queryPageList(PlatformGbStreamBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的级联关系列表。
     *
     * @param bo 查询条件
     * @return 级联关系列表
     */
    List<PlatformGbStreamVo> queryList(PlatformGbStreamBo bo);

    /**
     * 新增级联关系，同一平台目录下直播流编号不允许重复。
     *
     * @param bo 级联关系业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(PlatformGbStreamBo bo);

    /**
     * 修改级联关系。
     *
     * @param bo 级联关系业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(PlatformGbStreamBo bo);

    /**
     * 批量删除级联关系。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验（关系表无下游引用，保留扩展点）
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
