package cn.com.nla.common.freeswitch.model.message;

import cn.com.nla.common.freeswitch.model.MessageModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RecordCallModel implements MessageModel {

    private String mediaAddr;//fs通话设备地址
    private String deviceId;//设备
    private String playPath;//录音地址
    private String sampleRate = "8000";

}
