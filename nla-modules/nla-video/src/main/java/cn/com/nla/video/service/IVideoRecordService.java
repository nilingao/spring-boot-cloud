package cn.com.nla.video.service;

import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.domain.vo.VideoDownloadTaskVo;

import java.util.List;

/**
 * 国标录像服务：录像查询与录像下载走 SSE 异步回执，下载任务管理为同步缓存操作。
 *
 * @author TZY
 */
public interface IVideoRecordService {

    /**
     * 录像目录查询，结果经 SSE 回执推送。
     *
     * @param deviceGbId 设备国标编号
     * @param channelId  通道国标编号
     * @param startTime  开始时间，格式 yyyy-MM-dd HH:mm:ss
     * @param endTime    结束时间，格式 yyyy-MM-dd HH:mm:ss
     * @return 受理凭据
     */
    VideoCommandTicketVo query(String deviceGbId, String channelId, String startTime, String endTime);

    /**
     * 开始下载录像，结果经 SSE 回执推送。
     *
     * @param deviceGbId    设备国标编号
     * @param channelId     通道国标编号
     * @param startTime     开始时间，格式 yyyy-MM-dd HH:mm:ss
     * @param endTime       结束时间，格式 yyyy-MM-dd HH:mm:ss
     * @param downloadSpeed 下载倍速
     * @return 受理凭据
     */
    VideoCommandTicketVo download(String deviceGbId, String channelId, String startTime, String endTime,
                                  Integer downloadSpeed);

    /**
     * 停止下载录像。
     *
     * @param deviceGbId 设备国标编号
     * @param channelId  通道国标编号
     * @param stream     流ID
     */
    void stopDownload(String deviceGbId, String channelId, String stream);

    /**
     * 查询当前用户的下载任务。
     *
     * @return 下载任务列表
     */
    List<VideoDownloadTaskVo> queryDownloadList();

    /**
     * 清除当前用户的下载任务记录。
     *
     * @param key 下载任务缓存键
     */
    void deleteDownload(String key);
}
