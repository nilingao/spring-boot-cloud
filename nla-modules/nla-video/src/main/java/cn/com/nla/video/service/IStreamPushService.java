package cn.com.nla.video.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.video.domain.bo.StreamPushBo;
import cn.com.nla.video.domain.vo.StreamPushVo;

import java.util.Collection;
import java.util.List;

/**
 * 推流流信息 Service 接口。
 *
 * @author TZY
 */
public interface IStreamPushService {

    /**
     * 查询推流详情。
     *
     * @param id 主键
     * @return 推流视图对象
     */
    StreamPushVo queryById(Long id);

    /**
     * 按流标识查询推流信息。
     *
     * @param app    流应用名
     * @param stream 流Id
     * @return 推流视图对象，不存在返回 null
     */
    StreamPushVo queryByAppAndStream(String app, String stream);

    /**
     * 分页查询推流列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 推流分页列表
     */
    PageResult<StreamPushVo> queryPageList(StreamPushBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的推流列表。
     *
     * @param bo 查询条件
     * @return 推流列表
     */
    List<StreamPushVo> queryList(StreamPushBo bo);

    /**
     * 新增推流记录，应用名与流Id 组合不允许重复。
     *
     * @param bo 推流业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(StreamPushBo bo);

    /**
     * 修改推流记录。
     *
     * @param bo 推流业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(StreamPushBo bo);

    /**
     * 批量删除推流记录。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验（推流记录无下游引用，保留扩展点）
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
