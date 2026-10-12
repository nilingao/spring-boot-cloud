package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.exception.SsrcTransactionNotFoundException;
import cn.com.nla.common.video.basic.model.NotNullMap;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.service.PlayService;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.video.command.VideoCommandDispatcher;
import cn.com.nla.video.command.VideoCommandKeys;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.domain.vo.VideoDownloadTaskVo;
import cn.com.nla.video.service.IVideoRecordService;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * 国标录像服务实现。
 *
 * <p>录像查询按随机流水号建立回执路由，设备可能分多次上报录像条目，
 * 由协议侧汇总完成后统一释放；下载任务列表与清除只操作流媒体收流缓存。</p>
 *
 * @author TZY
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class VideoRecordServiceImpl implements IVideoRecordService {

    private final SipServer sipServer;
    private final SIPCommander sipCommander;
    private final PlayService playService;
    private final VideoCommandDispatcher videoCommandDispatcher;

    @Override
    public VideoCommandTicketVo query(String deviceGbId, String channelId, String startTime, String endTime) {
        int sn = RandomUtil.randomInt(100000, 999999);
        return videoCommandDispatcher.dispatch("record-query", VideoCommandKeys.recordInfo(deviceGbId, sn), handle -> {
            DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceGbId);
            if (deviceVo == null) {
                handle.fail(String.format("设备：%s 未找到", deviceGbId));
                return;
            }
            try {
                sipCommander.recordInfoQuery(sipServer, deviceVo, channelId, startTime, endTime, sn, null, null, null,
                    error -> handle.fail(error.getStatusCode(), error.getMsg()));
            } catch (InvalidArgumentException | SipException | ParseException e) {
                log.error("[命令发送失败] 查询录像：", e);
                handle.fail("查询录像失败");
            }
        });
    }

    @Override
    public VideoCommandTicketVo download(String deviceGbId, String channelId, String startTime, String endTime,
                                        Integer downloadSpeed) {
        return videoCommandDispatcher.dispatch("record-download", VideoCommandKeys.download(deviceGbId, channelId),
            handle -> {
                DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceGbId);
                if (deviceVo == null) {
                    handle.fail(String.format("设备：%s 未找到", deviceGbId));
                    return;
                }
                MediaServerVo mediaServerVo = VideoService.getMediaServerService()
                    .findMediaServerForMinimumLoad(deviceVo);
                if (mediaServerVo == null) {
                    handle.fail("流媒体未找到");
                    return;
                }
                playService.download(sipServer, mediaServerVo, deviceVo, channelId, null, startTime, endTime,
                    downloadSpeed, (code, msg, data) -> handle.complete(ProtocolResult.result(code, msg, data)));
            });
    }

    @Override
    public void stopDownload(String deviceGbId, String channelId, String stream) {
        DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceGbId);
        if (deviceVo == null) {
            throw new ServiceException("未获取设备信息");
        }
        try {
            sipCommander.streamByeCmd(sipServer, deviceVo, channelId, stream, null, null, null, null);
        } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
            log.error("[停止下载录像] 发送BYE失败：", e);
            throw new ServiceException("停止下载失败");
        }
    }

    @Override
    public List<VideoDownloadTaskVo> queryDownloadList() {
        return RedisService.getInviteStreamManager().getUserDownloadInviteInfoList(currentDownloadUser())
            .stream().map(this::toTaskVo).toList();
    }

    @Override
    public void deleteDownload(String key) {
        RedisService.getInviteStreamManager().delUserDownloadInviteInfoList(currentDownloadUser(), key);
    }

    /**
     * 下载任务归属用户，无登录态时回退到协议侧默认用户，与旧实现一致。
     *
     * @return 用户主键
     */
    private Long currentDownloadUser() {
        return ObjectUtil.defaultIfNull(LoginHelper.getUserId(), VideoConstant.DEFAULT_DOWNLOAD_USER);
    }

    /**
     * 缓存载荷转下载任务视图。
     *
     * @param map 流媒体收流缓存载荷
     * @return 下载任务
     */
    private VideoDownloadTaskVo toTaskVo(NotNullMap map) {
        VideoDownloadTaskVo vo = new VideoDownloadTaskVo();
        vo.setId(MapUtil.getStr(map, "id"));
        vo.setApp(MapUtil.getStr(map, "app"));
        vo.setStream(MapUtil.getStr(map, "stream"));
        vo.setDeviceId(MapUtil.getStr(map, "deviceId"));
        vo.setChannelId(MapUtil.getStr(map, "channelId"));
        vo.setMediaServerId(MapUtil.getStr(map, "mediaServerId"));
        vo.setStartTime(MapUtil.getStr(map, "startTime"));
        vo.setEndTime(MapUtil.getStr(map, "endTime"));
        vo.setProgress(MapUtil.getDouble(map, "progress"));
        return vo;
    }
}
