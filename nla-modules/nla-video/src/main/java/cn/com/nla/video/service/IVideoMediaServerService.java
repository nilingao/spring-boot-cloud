package cn.com.nla.video.service;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.video.domain.bo.VideoMediaServerBo;
import cn.com.nla.video.domain.vo.VideoMediaServerVo;

import java.util.Collection;
import java.util.List;

/**
 * 流媒体服务信息 Service 接口。
 *
 * @author TZY
 */
public interface IVideoMediaServerService {

    /**
     * 查询流媒体服务详情。
     *
     * @param id 流媒体服务编号
     * @return 流媒体服务视图对象
     */
    VideoMediaServerVo queryById(String id);

    /**
     * 分页查询流媒体服务列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 流媒体服务分页列表
     */
    PageResult<VideoMediaServerVo> queryPageList(VideoMediaServerBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的流媒体服务列表。
     *
     * @param bo 查询条件
     * @return 流媒体服务列表
     */
    List<VideoMediaServerVo> queryList(VideoMediaServerBo bo);

    /**
     * 查询已启用的流媒体服务列表。
     *
     * @return 启用的流媒体服务列表
     */
    List<VideoMediaServerVo> queryEnableList();

    /**
     * 查询已启用且在线的流媒体服务列表。
     *
     * @return 在线的流媒体服务列表
     */
    List<VideoMediaServerVo> queryOnlineList();

    /**
     * 新增流媒体服务，主键由调用方录入且不允许重复。
     *
     * @param bo 流媒体服务业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(VideoMediaServerBo bo);

    /**
     * 修改流媒体服务。
     *
     * @param bo 流媒体服务业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(VideoMediaServerBo bo);

    /**
     * 批量删除流媒体服务。
     *
     * @param ids     待删除的流媒体服务编号集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
