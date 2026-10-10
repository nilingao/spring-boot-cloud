package cn.com.nla.common.video.basic.vo.media;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OnStreamNotFoundHookVo extends HookVo{

    private String id;
    private String app;
    private String stream;
    private String ip;
    private String params;
    private int port;
    private String schema;
    private String vhost;
}
