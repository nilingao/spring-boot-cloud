package cn.com.nla.video.service;

import cn.com.nla.common.video.basic.vo.media.OnStreamChangedResult;
import cn.com.nla.common.video.core.demo.StreamInfo;

/**
 * 流媒体运行态服务：在线流地址查询、流信息查询与拉流代理启停。
 *
 * @author TZY
 */
public interface IVideoStreamService {

    /**
     * 查询设备通道当前点播会话的各协议播放地址。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 播放地址信息
     */
    StreamInfo queryInviteStreamUrl(String deviceId, String channelId);

    /**
     * 查询流媒体上指定流的媒体信息。
     *
     * @param app           流应用名
     * @param stream        流编号
     * @param mediaServerId 流媒体服务编号
     * @return 流媒体信息
     */
    OnStreamChangedResult queryMediaInfo(String app, String stream, String mediaServerId);

    /**
     * 查询流媒体节点的 FFmpeg 拉流命令模板。
     *
     * @param mediaServerId 流媒体服务编号
     * @return FFmpeg 命令模板
     */
    String queryFfmpegCmd(String mediaServerId);

    /**
     * 启用拉流代理。
     *
     * @param id 拉流代理主键
     */
    void startProxy(Long id);

    /**
     * 停用拉流代理。
     *
     * @param id 拉流代理主键
     */
    void stopProxy(Long id);

    /**
     * 查询拉流代理的各协议播放地址。
     *
     * @param id 拉流代理主键
     * @return 播放地址信息
     */
    StreamInfo queryProxyStreamUrl(Long id);

    /**
     * 查询推流的各协议播放地址。
     *
     * @param id 推流主键
     * @return 播放地址信息
     */
    StreamInfo queryPushStreamUrl(Long id);
}
