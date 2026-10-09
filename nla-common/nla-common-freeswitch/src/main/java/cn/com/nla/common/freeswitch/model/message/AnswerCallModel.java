package cn.com.nla.common.freeswitch.model.message;

import cn.com.nla.common.freeswitch.model.MessageModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 *应答参数
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class AnswerCallModel implements MessageModel {

    private boolean isActive;// 是否主动应答
    private String mediaAddr;//fs通话设备地址 主动
    private String deviceId;//设备

}
