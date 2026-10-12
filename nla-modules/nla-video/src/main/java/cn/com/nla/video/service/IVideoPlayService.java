package cn.com.nla.video.service;

import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.domain.vo.VideoConvertVo;

/**
 * 实时视频点播服务：点播/停止走 SIP 异步回执，转码走流媒体同步接口。
 *
 * @author TZY
 */
public interface IVideoPlayService {

    /**
     * 开始点播，结果经 SSE 回执推送。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 受理凭据
     */
    VideoCommandTicketVo play(String deviceId, String channelId);

    /**
     * 停止点播，结果经 SSE 回执推送。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 受理凭据
     */
    VideoCommandTicketVo stop(String deviceId, String channelId);

    /**
     * 视频流转码，非 H264 流转码为 H264 流。
     *
     * @param streamId 视频流ID
     * @return 转码结果，含结束转码所需的进程句柄
     */
    VideoConvertVo convert(String streamId);

    /**
     * 结束视频流转码。
     *
     * @param key           转码进程句柄
     * @param mediaServerId 流媒体服务ID
     */
    void convertStop(String key, String mediaServerId);
}
