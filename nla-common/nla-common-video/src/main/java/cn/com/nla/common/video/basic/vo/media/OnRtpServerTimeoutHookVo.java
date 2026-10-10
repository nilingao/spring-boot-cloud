package cn.com.nla.common.video.basic.vo.media;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OnRtpServerTimeoutHookVo extends HookVo{
    private int local_port;
    private String stream_id;
    private int tcpMode;
    private boolean re_use_port;
    private String ssrc;
}
