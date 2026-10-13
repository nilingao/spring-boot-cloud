package cn.com.nla.callcenter.service;

import cn.com.nla.callcenter.domain.bo.RouteGroupBo;
import cn.com.nla.callcenter.domain.bo.RouteGroupGatewayEditBo;
import cn.com.nla.callcenter.domain.vo.FsOptionVo;
import cn.com.nla.callcenter.domain.vo.RouteGroupVo;
import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 路由组(网关组) Service 接口。
 *
 * @author TZY
 */
public interface IRouteGroupService {

    /**
     * 查询路由组详情, 含已绑定的网关选项列表。
     *
     * @param id 路由组编号
     * @return 路由组视图对象(含 gateways)
     */
    RouteGroupVo queryById(Long id);

    /**
     * 分页查询路由组列表。
     *
     * @param bo        查询条件(名称模糊/状态)
     * @param pageQuery 分页参数
     * @return 路由组分页列表
     */
    List<RouteGroupVo> queryList(RouteGroupBo bo);

    PageResult<RouteGroupVo> queryPageList(RouteGroupBo bo, PageQuery pageQuery);

    /**
     * 新增路由组。
     *
     * @param bo 路由组业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(RouteGroupBo bo);

    /**
     * 修改路由组。
     *
     * @param bo 路由组业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(RouteGroupBo bo);

    /**
     * 以完整列表替换路由组下的网关绑定, 事务内先删旧关系再批量插入。
     *
     * @param bo 路由组编号与网关编号列表
     * @return 是否更新成功
     */
    Boolean updateGateways(RouteGroupGatewayEditBo bo);

    /**
     * 切换路由组状态。
     *
     * @param bo 仅使用 id 与 status 字段
     * @return 是否更新成功
     */
    Boolean updateStatus(RouteGroupBo bo);

    /**
     * 批量删除路由组, 被外呼路由引用时拒绝删除并同步清理网关关系。
     *
     * @param ids     待删除的路由组编号集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 查询路由组下拉选项。
     *
     * @param keyword 名称模糊关键词, 可为空
     * @param limit   返回条数上限, 为空时默认 20
     * @return 路由组选项列表
     */
    List<FsOptionVo> queryOptionList(String keyword, Integer limit);

}
