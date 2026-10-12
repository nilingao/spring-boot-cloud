package cn.com.nla.video.service;

import cn.com.nla.video.domain.vo.DeviceMobilePositionVo;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;

import java.util.List;

/**
 * 设备移动位置服务：历史/最新位置查库，实时位置走 SIP 异步回执，另含订阅管理与坐标系换算。
 *
 * @author TZY
 */
public interface IVideoPositionService {

    /**
     * 查询设备历史轨迹。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号，可为空
     * @param start     起始时间串，与 end 同时传入才生效，可为空
     * @param end       结束时间串，可为空
     * @return 轨迹列表，按上报时间倒序
     */
    List<DeviceMobilePositionVo> queryHistory(String deviceId, String channelId, String start, String end);

    /**
     * 查询设备最新位置。
     *
     * @param deviceId 设备国标编号
     * @return 最新位置记录，无记录返回 {@code null}
     */
    DeviceMobilePositionVo queryLatest(String deviceId);

    /**
     * 主动查询设备实时位置，结果经 SSE 回执推送。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号，可为空
     * @return 受理凭据
     */
    VideoCommandTicketVo queryRealtime(String deviceId, String channelId);

    /**
     * 订阅/取消订阅设备通知，取消时只需把周期置为 0。
     *
     * @param deviceId 设备国标编号
     * @param type     订阅类型：1 目录，2 移动位置，3 报警
     * @param expires  订阅周期，单位秒，大于 0 建立订阅
     */
    void subscribe(String deviceId, Integer type, Integer expires);

    /**
     * 按设备坐标系重算全部通道的 WGS84/GCJ02 坐标。
     *
     * @param deviceId 设备国标编号
     */
    void transform(String deviceId);
}
