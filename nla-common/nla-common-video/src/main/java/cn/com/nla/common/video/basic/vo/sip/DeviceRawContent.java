package cn.com.nla.common.video.basic.vo.sip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceRawContent {

    private String username;

    private String addressStr;

    private Integer port;

    private String ssrc;

    private boolean mediaTransmissionTCP;

    private boolean tcpActive;

    private String sessionName;

    private Long startTime;

    private Long stopTime;

    private String downloadSpeed;
}
