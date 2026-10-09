package cn.com.nla.common.freeswitch.model.message;

import cn.com.nla.common.freeswitch.model.MessageModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 *桥接参数
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class BridgeCallModel implements MessageModel {

    private String mediaAddr;//fs通话设备地址
    private String oneDeviceId;//设备1
    private String twoDeviceId;//设备2


}
