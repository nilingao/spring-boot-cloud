package cn.com.nla.common.freeswitch.client.sip.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "video-settings", ignoreInvalidFields = true)
@Data
public class VideoProperties {
    // [可选] 服务ID，不写则为000000
    private String serverId = "000000";
    // [可选] 部分设备需要扩展SDP，需要打开此设置
    private Boolean seniorSdp = Boolean.FALSE;
    // 保存移动位置历史轨迹：true:保留历史数据，false:仅保留最后的位置(默认)
    private Boolean savePositionHistory = Boolean.FALSE;
    // 点播/录像回放 等待超时时间,单位：秒
    private Integer playTimeout = 18;
    // 上级点播等待超时时间,单位：秒
    private int platformPlayTimeout = 60;
    // 国标是否录制
    private Boolean recordSip = Boolean.FALSE;
    // 推流直播是否录制
    private Boolean recordPushLive = Boolean.FALSE;
    // 使用推流状态作为推流通道状态
    private Boolean usePushingAsStatus = Boolean.TRUE;
    // 是否使用设备来源Ip作为回复IP， 不设置则为 false
    private Boolean sipUseSourceIpAsRemoteAddress = Boolean.FALSE;
    // 国标点播 按需拉流, true：有人观看拉流，无人观看释放， false：拉起后不自动释放
    private Boolean streamOnDemand = Boolean.TRUE;
    // 推流鉴权， 默认开启
    private Boolean pushAuthority = Boolean.TRUE;
    // 是否开启sip日志
    private Boolean sipLog = Boolean.FALSE;
    // 消息通道功能-缺少国标ID是否给所有上级发送消息
    private Boolean sendToPlatformsWhenIdLost = Boolean.FALSE;
    // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
    private Boolean useCustomSsrcForParentInvite = Boolean.TRUE;
    // 是否在设备流媒体离线时切换在线流媒体
    private Boolean useClientOnLineZlm = Boolean.TRUE;
}
