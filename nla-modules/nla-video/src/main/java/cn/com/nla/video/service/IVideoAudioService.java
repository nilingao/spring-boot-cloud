package cn.com.nla.video.service;

import cn.com.nla.video.domain.vo.VideoCommandTicketVo;

/**
 * 语音对讲与语音广播服务。
 *
 * <p>对讲推流地址由协议侧分配 SSRC 并登记推流事务，设备在 15 秒内回 INVITE 才算建立成功；
 * 语音广播需要设备 ACK 之外的应答，按 SSE 回执异步返回。</p>
 *
 * @author TZY
 */
public interface IVideoAudioService {

    /**
     * 获取语音对讲推流地址。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return WebRTC 推流地址
     */
    String findPushPath(String deviceId, String channelId);

    /**
     * 查询语音对讲推流是否已建立。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 是否正在推流
     */
    boolean isPushing(String deviceId, String channelId);

    /**
     * 关闭语音对讲推流。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     */
    void stopPush(String deviceId, String channelId);

    /**
     * 下发语音广播命令，结果经 SSE 回执推送。
     *
     * <p>超时无应答时设备侧不会自行回收，需同步关闭已登记的语音推流事务，故超时结果带回收副作用。</p>
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 受理凭据
     */
    VideoCommandTicketVo broadcast(String deviceId, String channelId);
}
