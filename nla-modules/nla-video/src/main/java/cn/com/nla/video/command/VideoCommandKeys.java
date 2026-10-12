package cn.com.nla.video.command;

import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;

/**
 * 视频指令异步回执路由键，键格式必须与 {@code nla-common-video} 内协议回调发布方保持一致。
 *
 * @author TZY
 */
public final class VideoCommandKeys {

    private VideoCommandKeys() {
    }

    /**
     * 点播回执键，与 ZLM Hook 自动点播发布键一致。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 回执路由键
     */
    public static String play(String deviceId, String channelId) {
        return String.format("%s%s_%s", DeferredResultHolder.CALLBACK_CMD_PLAY, deviceId, channelId);
    }

    /**
     * 停止点播回执键，与 ZLM Hook 停止点播发布键一致。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 回执路由键
     */
    public static String stop(String deviceId, String channelId) {
        return String.format("%s%s_%s", DeferredResultHolder.CALLBACK_CMD_STOP, deviceId, channelId);
    }

    /**
     * 回放回执键，与 ZLM Hook 自动回放发布键一致。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param startTime 开始时间串
     * @param endTime   结束时间串
     * @return 回执路由键
     */
    public static String playback(String deviceId, String channelId, String startTime, String endTime) {
        return String.format("%s_%s_%s_%s", deviceId, channelId, startTime, endTime);
    }

    /**
     * 录像下载回执键。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 回执路由键
     */
    public static String download(String deviceId, String channelId) {
        return DeferredResultHolder.CALLBACK_CMD_DOWNLOAD + deviceId + channelId;
    }

    /**
     * 录像查询回执键，与录像应答处理发布键一致。
     *
     * @param deviceId 设备国标编号
     * @param sn       查询流水号
     * @return 回执路由键
     */
    public static String recordInfo(String deviceId, int sn) {
        return String.format("%s%s_%s", DeferredResultHolder.CALLBACK_CMD_RECORDINFO, deviceId, sn);
    }

    /**
     * 设备控制回执键，与设备控制应答处理发布键一致。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 回执路由键
     */
    public static String deviceControl(String deviceId, String channelId) {
        return String.format("%s%s_%s", DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL, deviceId, channelId);
    }

    /**
     * 设备配置命令回执键，与设备配置应答处理发布键一致。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 回执路由键
     */
    public static String deviceConfig(String deviceId, String channelId) {
        return String.format("%s%s_%s", DeferredResultHolder.CALLBACK_CMD_DEVICECONFIG, deviceId, channelId);
    }

    /**
     * 设备配置下载回执键，与配置下载应答处理发布键一致。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 回执路由键
     */
    public static String configDownload(String deviceId, String channelId) {
        return String.format("%s%s_%s", DeferredResultHolder.CALLBACK_CMD_CONFIGDOWNLOAD, deviceId, channelId);
    }

    /**
     * 设备状态查询回执键，与设备状态应答处理发布键一致。
     *
     * @param deviceId 设备国标编号
     * @return 回执路由键
     */
    public static String deviceStatus(String deviceId) {
        return String.format("%s%s", DeferredResultHolder.CALLBACK_CMD_DEVICESTATUS, deviceId);
    }

    /**
     * 设备信息查询回执键，与设备信息应答处理发布键一致。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 回执路由键
     */
    public static String deviceInfo(String deviceId, String channelId) {
        return DeferredResultHolder.CALLBACK_CMD_DEVICEINFO + deviceId + channelId;
    }

    /**
     * 报警应答回执键，与报警应答处理发布键一致。
     *
     * @param deviceId 设备国标编号
     * @return 回执路由键
     */
    public static String alarm(String deviceId) {
        return String.format("%s%s", DeferredResultHolder.CALLBACK_CMD_ALARM, deviceId);
    }

    /**
     * 语音广播回执键，与广播 ACK 处理发布键一致。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 回执路由键
     */
    public static String broadcast(String deviceId, String channelId) {
        return String.format("%s%s_%s", DeferredResultHolder.CALLBACK_CMD_BROADCAST, deviceId, channelId);
    }

    /**
     * 预置位查询回执键，与预置位应答处理发布键一致。
     *
     * @param deviceId 设备国标编号
     * @return 回执路由键
     */
    public static String presetQuery(String deviceId) {
        return String.format("%s%s", DeferredResultHolder.CALLBACK_CMD_PRESETQUERY, deviceId);
    }

    /**
     * 移动位置查询回执键，与移动位置应答处理发布键一致。
     *
     * @param deviceId 设备国标编号
     * @return 回执路由键
     */
    public static String mobilePosition(String deviceId) {
        return String.format("%s%s", DeferredResultHolder.CALLBACK_CMD_MOBILEPOSITION, deviceId);
    }
}
