package cn.com.nla.callcenter.service;

import cn.com.nla.callcenter.domain.bo.RouteGatewayBo;
import cn.com.nla.callcenter.domain.vo.FsOptionVo;
import cn.com.nla.callcenter.domain.vo.RouteGatewayVo;
import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 路由网关(媒体网关) Service 接口。
 *
 * @author TZY
 */
public interface IRouteGatewayService {

    /**
     * 查询路由网关详情。
     *
     * @param id 路由网关编号
     * @return 路由网关视图对象
     */
    RouteGatewayVo queryById(Long id);

    /**
     * 分页查询路由网关列表。
     *
     * @param bo        查询条件(名称模糊/状态)
     * @param pageQuery 分页参数
     * @return 路由网关分页列表
     */
    List<RouteGatewayVo> queryList(RouteGatewayBo bo);

    PageResult<RouteGatewayVo> queryPageList(RouteGatewayBo bo, PageQuery pageQuery);

    /**
     * 新增路由网关, 校验名称唯一。
     *
     * @param bo 路由网关业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(RouteGatewayBo bo);

    /**
     * 修改路由网关, 校验名称唯一(排除自身)。
     *
     * @param bo 路由网关业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(RouteGatewayBo bo);

    /**
     * 切换路由网关状态。
     *
     * @param bo 仅使用 id 与 status 字段
     * @return 是否更新成功
     */
    Boolean updateStatus(RouteGatewayBo bo);

    /**
     * 批量删除路由网关, 被路由组引用时拒绝删除。
     *
     * @param ids     待删除的路由网关编号集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 查询路由网关下拉选项。
     *
     * @param keyword 名称模糊关键词, 可为空
     * @param limit   返回条数上限, 为空时默认 20
     * @return 路由网关选项列表
     */
    List<FsOptionVo> queryOptionList(String keyword, Integer limit);

}
