package cn.com.nla.common.freeswitch.model.message;

import cn.com.nla.common.freeswitch.model.MessageModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 播放按键导航音参数
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ReceiveDtmfModel implements MessageModel {
    private String mediaAddr;//fs通话设备地址
    private String deviceId;//设备
}
