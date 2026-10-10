package cn.com.nla.common.video.basic.vo.media;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OnSendRtpStoppedHookVo extends HookVo{
    private String app;
    private String stream;
}
