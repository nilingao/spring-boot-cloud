package cn.com.nla.video.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.video.domain.bo.ParentPlatformBo;
import cn.com.nla.video.domain.vo.ParentPlatformVo;

import java.util.Collection;
import java.util.List;

/**
 * 上级平台（国标级联）Service 接口。
 *
 * @author TZY
 */
public interface IParentPlatformService {

    /**
     * 查询上级平台详情。
     *
     * @param id 主键
     * @return 上级平台视图对象
     */
    ParentPlatformVo queryById(Long id);

    /**
     * 按上级平台国标编号查询详情。
     *
     * @param serverGbId 上级平台国标编号
     * @return 上级平台视图对象，不存在返回 null
     */
    ParentPlatformVo queryByServerGbId(String serverGbId);

    /**
     * 分页查询上级平台列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 上级平台分页列表
     */
    PageResult<ParentPlatformVo> queryPageList(ParentPlatformBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的上级平台列表。
     *
     * @param bo 查询条件
     * @return 上级平台列表
     */
    List<ParentPlatformVo> queryList(ParentPlatformBo bo);

    /**
     * 查询已启用的上级平台列表，供启动时批量发起注册。
     *
     * @return 上级平台列表
     */
    List<ParentPlatformVo> queryEnableList();

    /**
     * 新增上级平台，平台国标编号不允许重复。
     *
     * @param bo 上级平台业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(ParentPlatformBo bo);

    /**
     * 修改上级平台，平台国标编号不允许与其他平台重复。
     *
     * @param bo 上级平台业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(ParentPlatformBo bo);

    /**
     * 校验并批量删除上级平台，存在目录或级联关系时拒绝删除。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
