package cn.com.nla.common.freeswitch.vo.sip;

import cn.com.nla.common.freeswitch.enums.sip.VideoStreamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.sip.SipException;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceRawContent {

    private DeviceInfo audioInfo;

    private DeviceInfo videoInfo;

    public void setDeviceInfo(String mediaFormat,DeviceInfo info) throws SipException {
        if(VideoStreamType.CALL_AUDIO_PHONE.getPt() == Integer.parseInt(mediaFormat)){
            this.audioInfo =info;
        }else if(VideoStreamType.CALL_VIDEO_PHONE.getPt() == Integer.parseInt(mediaFormat)){
            this.videoInfo =info;
        }else {
            throw new SipException("不支持此类型数据");
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceInfo{
        private String username;

        private String addressStr;

        private Integer port;

        private String ssrc;

        private boolean mediaTransmissionTCP;

        private boolean tcpActive;

        private String sessionName;

        private String downloadSpeed;
    }

}
