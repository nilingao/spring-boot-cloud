package cn.com.nla.face.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.face.domain.bo.PersonBo;
import cn.com.nla.face.domain.vo.PersonVo;

import java.util.Collection;
import java.util.List;

/**
 * 人员信息 Service 接口。
 *
 * @author TZY
 */
public interface IPersonService {

    /**
     * 查询人员详情。
     *
     * @param id 主键
     * @return 人员视图对象
     */
    PersonVo queryById(Long id);

    /**
     * 分页查询人员列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 人员分页列表
     */
    PageResult<PersonVo> queryPageList(PersonBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的人员列表。
     *
     * @param bo 查询条件
     * @return 人员列表
     */
    List<PersonVo> queryList(PersonBo bo);

    /**
     * 新增人员，按图片地址自动提取人脸特征并同步特征缓存。
     *
     * @param bo 人员业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(PersonBo bo);

    /**
     * 修改人员，图片地址变更时重新提取特征，同步维护特征缓存键。
     *
     * @param bo 人员业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(PersonBo bo);

    /**
     * 校验并批量删除人员，删除成功后移除特征缓存。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 按图片编号批量查询有效人员，供 1:N 检索回填结果，不加载特征。
     *
     * @param imgIds 图片编号集合
     * @return 人员视图列表；集合为空返回空列表
     */
    List<PersonVo> selectByImgIds(Collection<String> imgIds);
}
