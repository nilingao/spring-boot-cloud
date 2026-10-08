package cn.com.nla.message.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.message.domain.bo.MobileMessageBo;
import cn.com.nla.message.domain.vo.MobileMessageVo;

import java.util.Collection;
import java.util.List;

/**
 * 短信发送记录Service接口
 *
 * @author TZY
 */
public interface IMobileMessageService {

    /**
     * 分页查询短信发送记录列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 短信发送记录分页列表
     */
    PageResult<MobileMessageVo> queryPageList(MobileMessageBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的短信发送记录列表
     *
     * @param bo 查询条件
     * @return 短信发送记录列表
     */
    List<MobileMessageVo> queryList(MobileMessageBo bo);

    /**
     * 批量删除短信发送记录
     *
     * @param ids 待删除的主键集合
     * @return 是否删除成功
     */
    Boolean deleteByIds(Collection<Long> ids);

    /**
     * 清空短信发送记录
     */
    void clean();

}
