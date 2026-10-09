package cn.com.nla.common.freeswitch.model.message;

import cn.com.nla.common.freeswitch.model.MessageModel;
import cn.com.nla.common.freeswitch.model.call.CallInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 转接电话参数
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class TransferCallModel implements MessageModel {

    private boolean isConference;
    //---会议参数---
    private String conferenceCode;
    private String conferencePwd;
    //---转接参数---
    private CallInfo callInfo;//通话编号
    private String recordPath;
    private String recordFile;
    private Long eventDate;//事件事件
    private String mediaAddr;//fs通话设备地址
    private String oldDeviceId;//挂断设备uuid
    private String formDeviceId;//转接设备uuid
    private String toDeviceId;//要转接uuid



}
