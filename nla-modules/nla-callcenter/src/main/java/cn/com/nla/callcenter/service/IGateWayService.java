package cn.com.nla.callcenter.service;

import cn.com.nla.callcenter.domain.bo.GateWayBo;
import cn.com.nla.callcenter.domain.vo.FsOptionVo;
import cn.com.nla.callcenter.domain.vo.GateWayVo;
import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * FS 注册网关 Service 接口。
 *
 * @author TZY
 */
public interface IGateWayService {

    /**
     * 查询网关详情, 密码字段不回显。
     *
     * @param id 网关编号
     * @return 网关视图对象
     */
    GateWayVo queryById(Long id);

    /**
     * 分页查询网关列表。
     *
     * @param bo        查询条件(名称模糊/选中标记)
     * @param pageQuery 分页参数
     * @return 网关分页列表
     */
    List<GateWayVo> queryList(GateWayBo bo);

    PageResult<GateWayVo> queryPageList(GateWayBo bo, PageQuery pageQuery);

    /**
     * 新增网关。
     *
     * @param bo 网关业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(GateWayBo bo);

    /**
     * 修改网关, 密码留空时保持原值。
     *
     * @param bo 网关业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(GateWayBo bo);

    /**
     * 切换网关选中标记(0 未选中 / 1 已选中)。
     *
     * @param bo 仅使用 id 与 selected 字段
     * @return 是否更新成功
     */
    Boolean updateSelected(GateWayBo bo);

    /**
     * 批量删除网关, 无外部引用校验。
     *
     * @param ids     待删除的网关编号集合
     * @param isValid 是否进行有效性校验(保留参数形态, 当前域无引用约束)
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 查询网关下拉选项。
     *
     * @param keyword 名称模糊关键词, 可为空
     * @param limit   返回条数上限, 为空时默认 20
     * @return 网关选项列表
     */
    List<FsOptionVo> queryOptionList(String keyword, Integer limit);

}
