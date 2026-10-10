package cn.com.nla.common.video.core.demo;

import cn.com.nla.common.video.basic.enums.InviteSessionStatus;
import cn.com.nla.common.video.basic.enums.VideoStreamType;
import cn.com.nla.common.video.basic.vo.sip.SSRCInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记录每次发送invite消息的状态
 */
@Data
@NoArgsConstructor
public class InviteInfo {

    /**
     * 操作用户
     */
    private Long userId;
    private String deviceId;

    private String channelId;

    private String stream;

    private SSRCInfo ssrcInfo;

    private String receiveIp;

    private Integer receivePort;
    /**
     * 数据流传输模式 0.UDP:udp传输 2.TCP-ACTIVE：tcp主动模式 2.TCP-PASSIVE：tcp被动模式
     */
    private Integer streamMode;

    private VideoStreamType type;

    private InviteSessionStatus status;

    private StreamInfo streamInfo;
    public InviteInfo(Long userId,String deviceId, String channelId, String stream, SSRCInfo ssrcInfo, String receiveIp, Integer receivePort, Integer streamMode, VideoStreamType type, InviteSessionStatus status){
        this.userId = userId;
        this.deviceId = deviceId;
        this.channelId = channelId;
        this.stream = stream;
        this.ssrcInfo = ssrcInfo;
        this.receiveIp = receiveIp;
        this.receivePort = receivePort;
        this.streamMode = streamMode;
        this.type = type;
        this.status = status;
    }
}
