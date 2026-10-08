package cn.com.nla.message.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.message.domain.bo.MobileMessageTemplateBo;
import cn.com.nla.message.domain.vo.MobileMessageTemplateVo;

import java.util.Collection;
import java.util.List;

/**
 * 短信模板Service接口
 *
 * @author TZY
 */
public interface IMobileMessageTemplateService {

    /**
     * 查询短信模板
     *
     * @param id 主键
     * @return 短信模板
     */
    MobileMessageTemplateVo queryById(Long id);

    /**
     * 分页查询短信模板列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 短信模板分页列表
     */
    PageResult<MobileMessageTemplateVo> queryPageList(MobileMessageTemplateBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的短信模板列表
     *
     * @param bo 查询条件
     * @return 短信模板列表
     */
    List<MobileMessageTemplateVo> queryList(MobileMessageTemplateBo bo);

    /**
     * 新增短信模板
     *
     * @param bo 短信模板
     * @return 是否新增成功
     */
    Boolean insertByBo(MobileMessageTemplateBo bo);

    /**
     * 修改短信模板
     *
     * @param bo 短信模板
     * @return 是否修改成功
     */
    Boolean updateByBo(MobileMessageTemplateBo bo);

    /**
     * 校验并批量删除短信模板信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
