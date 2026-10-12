package cn.com.nla.video.service;

import cn.com.nla.video.domain.vo.VideoCommandTicketVo;

/**
 * 历史视频回放服务：起播走 SSE 异步回执，暂停/恢复/拖动/倍速为同步控制指令。
 *
 * @author TZY
 */
public interface IVideoPlaybackService {

    /**
     * 开始回放，结果经 SSE 回执推送。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param startTime 开始时间，格式 yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间，格式 yyyy-MM-dd HH:mm:ss
     * @return 受理凭据
     */
    VideoCommandTicketVo start(String deviceId, String channelId, String startTime, String endTime);

    /**
     * 停止回放。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param streamId  流ID
     */
    void stop(String deviceId, String channelId, String streamId);

    /**
     * 暂停回放。
     *
     * @param streamId 流ID
     */
    void suspend(String streamId);

    /**
     * 恢复已暂停的回放。
     *
     * @param streamId 流ID
     */
    void restore(String streamId);

    /**
     * 回放拖动播放。
     *
     * @param streamId 流ID
     * @param seekTime 拖动偏移量，单位秒
     */
    void seek(String streamId, Long seekTime);

    /**
     * 回放倍速播放。
     *
     * @param streamId 流ID
     * @param speed    倍速，仅支持 0.25、0.5、1、2、4
     */
    void speed(String streamId, Double speed);
}
