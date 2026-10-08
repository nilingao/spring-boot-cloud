package cn.com.nla.message.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.message.domain.bo.SmsConfigBo;
import cn.com.nla.message.domain.vo.SmsConfigVo;

import java.util.Collection;
import java.util.List;

/**
 * 短信渠道配置Service接口
 *
 * @author TZY
 */
public interface ISmsConfigService {

    /**
     * 查询短信渠道配置
     *
     * @param id 主键
     * @return 短信渠道配置
     */
    SmsConfigVo queryById(Long id);

    /**
     * 分页查询短信渠道配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 短信渠道配置分页列表
     */
    PageResult<SmsConfigVo> queryPageList(SmsConfigBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的短信渠道配置列表
     *
     * @param bo 查询条件
     * @return 短信渠道配置列表
     */
    List<SmsConfigVo> queryList(SmsConfigBo bo);

    /**
     * 新增短信渠道配置
     *
     * @param bo 短信渠道配置
     * @return 是否新增成功
     */
    Boolean insertByBo(SmsConfigBo bo);

    /**
     * 修改短信渠道配置
     *
     * @param bo 短信渠道配置
     * @return 是否修改成功
     */
    Boolean updateByBo(SmsConfigBo bo);

    /**
     * 校验并批量删除短信渠道配置信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
