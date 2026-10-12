package cn.com.nla.video.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.video.domain.bo.PlatformCatalogBo;
import cn.com.nla.video.domain.vo.PlatformCatalogVo;

import java.util.Collection;
import java.util.List;

/**
 * 上级平台目录 Service 接口。
 *
 * @author TZY
 */
public interface IPlatformCatalogService {

    /**
     * 查询目录详情。
     *
     * @param id 目录国标编号
     * @return 目录视图对象
     */
    PlatformCatalogVo queryById(String id);

    /**
     * 分页查询目录列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 目录分页列表
     */
    PageResult<PlatformCatalogVo> queryPageList(PlatformCatalogBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的目录列表。
     *
     * @param bo 查询条件
     * @return 目录列表
     */
    List<PlatformCatalogVo> queryList(PlatformCatalogBo bo);

    /**
     * 按上级平台国标编号查询目录列表。
     *
     * @param platformId 上级平台国标编号
     * @return 目录列表
     */
    List<PlatformCatalogVo> queryByPlatformId(String platformId);

    /**
     * 新增目录，目录国标编号由录入方指定。
     *
     * @param bo 目录业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(PlatformCatalogBo bo);

    /**
     * 修改目录。
     *
     * @param bo 目录业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(PlatformCatalogBo bo);

    /**
     * 校验并批量删除目录，存在子目录或级联关系时拒绝删除。
     *
     * @param ids     待删除的目录编号集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
