package cn.com.nla.common.freeswitch.model.message;

import cn.com.nla.common.freeswitch.model.MessageModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 语音播放参数
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class PlayBackCallModel implements MessageModel {
    private String mediaAddr;//fs通话设备地址
    private String deviceId;//设备
    private boolean isDown;//是否关闭录音
    private String playPath;//录音地址
}
