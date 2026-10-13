package cn.com.nla.callcenter.service;

import java.util.List;
import cn.com.nla.callcenter.domain.bo.RouteCallBo;
import cn.com.nla.callcenter.domain.vo.RouteCallVo;
import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;

import java.util.Collection;

/**
 * 外呼路由(字冠路由) Service 接口。
 *
 * @author TZY
 */
public interface IRouteCallService {

    /**
     * 查询外呼路由详情。
     *
     * @param id 外呼路由编号
     * @return 外呼路由视图对象
     */
    RouteCallVo queryById(Long id);

    /**
     * 分页查询外呼路由列表。
     *
     * @param bo        查询条件(企业/路由组/状态)
     * @param pageQuery 分页参数
     * @return 外呼路由分页列表
     */
    List<RouteCallVo> queryList(RouteCallBo bo);

    PageResult<RouteCallVo> queryPageList(RouteCallBo bo, PageQuery pageQuery);

    /**
     * 新增外呼路由, 校验号码区间与同企业字冠唯一。
     *
     * @param bo 外呼路由业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(RouteCallBo bo);

    /**
     * 修改外呼路由, 校验号码区间与同企业字冠唯一(排除自身)。
     *
     * @param bo 外呼路由业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(RouteCallBo bo);

    /**
     * 切换外呼路由状态。
     *
     * @param bo 仅使用 id 与 status 字段
     * @return 是否更新成功
     */
    Boolean updateStatus(RouteCallBo bo);

    /**
     * 批量删除外呼路由。
     *
     * @param ids     待删除的外呼路由编号集合
     * @param isValid 是否进行有效性校验(保留参数形态, 当前域无引用约束)
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
