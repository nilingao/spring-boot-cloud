package cn.com.nla.video.service;

import cn.com.nla.common.video.basic.vo.sip.Address;
import cn.com.nla.video.domain.bo.VideoPlatformShareBo;
import cn.com.nla.video.domain.vo.GbStreamVo;
import cn.com.nla.video.domain.vo.VideoBindKeyVo;
import cn.hutool.core.lang.tree.Tree;

import java.util.List;

/**
 * 上级平台级联共享服务：目录树、SIP 注册节点查询与通道/直播流的目录绑定。
 *
 * <p>绑定关系变更后会向上级平台推送目录增量事件，未注册或已下线的平台由协议侧忽略。</p>
 *
 * @author TZY
 */
public interface IVideoPlatformShareService {

    /**
     * 查询注册到本级 SIP 服务的全部节点。
     *
     * @return SIP 注册地址列表
     */
    List<Address> querySipList();

    /**
     * 查询可共享的设备通道树，与设备通道目录树一致（根节点为设备）。
     *
     * @return 树形目录
     */
    List<Tree<String>> queryChannelTree();

    /**
     * 查询上级平台目录树，根节点为平台自身。
     *
     * @param platformId 上级平台国标编号
     * @return 树形目录
     */
    List<Tree<String>> queryCatalogTree(String platformId);

    /**
     * 解除目录下已共享的通道或直播流。
     *
     * @param catalogId 目录国标编号，等于上级平台国标编号时为平台根目录
     * @param type      解除类型：0 全部，1 通道，2 直播流，为空按全部处理
     */
    void deleteCatalogRelation(String catalogId, Integer type);

    /**
     * 查询当前目录下已绑定的通道编号。
     *
     * @param bo 平台、目录与范围条件
     * @return 已绑定编号集合
     */
    VideoBindKeyVo queryChannelBindKey(VideoPlatformShareBo bo);

    /**
     * 将设备通道共享到上级平台目录。
     *
     * @param bo 平台、目录与通道范围条件
     */
    void bindChannel(VideoPlatformShareBo bo);

    /**
     * 从上级平台目录移除设备通道共享。
     *
     * @param bo 平台、目录与通道范围条件
     */
    void unbindChannel(VideoPlatformShareBo bo);

    /**
     * 查询可共享的国标直播流列表。
     *
     * @return 直播流列表
     */
    List<GbStreamVo> queryStreamList();

    /**
     * 查询当前目录下已绑定的直播流编号。
     *
     * @param bo 平台、目录与范围条件
     * @return 已绑定编号集合
     */
    VideoBindKeyVo queryStreamBindKey(VideoPlatformShareBo bo);

    /**
     * 将国标直播流共享到上级平台目录。
     *
     * @param bo 平台、目录与直播流范围条件
     */
    void bindStream(VideoPlatformShareBo bo);

    /**
     * 从上级平台目录移除国标直播流共享。
     *
     * @param bo 平台、目录与直播流范围条件
     */
    void unbindStream(VideoPlatformShareBo bo);
}
