package cn.com.nla.video.service;

import cn.hutool.core.lang.tree.Tree;
import cn.com.nla.common.video.basic.vo.sip.SyncStatus;

import java.util.List;

/**
 * 设备通道目录服务：设备/通道树形目录与国标目录同步。
 *
 * @author TZY
 */
public interface IVideoChannelCatalogService {

    /**
     * 查询设备与通道树形目录，非超级管理员只可见未标记为管理员专属的设备及其通道。
     *
     * @return 树形目录，根节点为设备
     */
    List<Tree<String>> queryTree();

    /**
     * 发起设备通道目录同步，同步进度由 {@link #querySyncStatus(String)} 轮询。
     *
     * @param deviceId 设备国标编号
     */
    void sync(String deviceId);

    /**
     * 查询设备通道目录同步进度。
     *
     * @param deviceId 设备国标编号
     * @return 同步进度
     */
    SyncStatus querySyncStatus(String deviceId);
}
