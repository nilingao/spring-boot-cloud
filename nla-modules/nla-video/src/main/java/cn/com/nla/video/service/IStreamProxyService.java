package cn.com.nla.video.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.video.domain.bo.StreamProxyBo;
import cn.com.nla.video.domain.vo.StreamProxyVo;

import java.util.Collection;
import java.util.List;

/**
 * 拉流代理 Service 接口。
 *
 * @author TZY
 */
public interface IStreamProxyService {

    /**
     * 查询拉流代理详情。
     *
     * @param id 主键
     * @return 拉流代理视图对象
     */
    StreamProxyVo queryById(Long id);

    /**
     * 按流标识查询拉流代理配置。
     *
     * @param app    流应用名
     * @param stream 流Id
     * @return 拉流代理视图对象，不存在返回 null
     */
    StreamProxyVo queryByAppAndStream(String app, String stream);

    /**
     * 分页查询拉流代理列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 拉流代理分页列表
     */
    PageResult<StreamProxyVo> queryPageList(StreamProxyBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的拉流代理列表。
     *
     * @param bo 查询条件
     * @return 拉流代理列表
     */
    List<StreamProxyVo> queryList(StreamProxyBo bo);

    /**
     * 新增拉流代理，应用名与流Id 组合不允许重复。
     *
     * @param bo 拉流代理业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(StreamProxyBo bo);

    /**
     * 修改拉流代理，应用名与流Id 组合不允许与其他配置重复。
     *
     * @param bo 拉流代理业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(StreamProxyBo bo);

    /**
     * 批量删除拉流代理。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验（代理配置无下游引用，保留扩展点）
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
