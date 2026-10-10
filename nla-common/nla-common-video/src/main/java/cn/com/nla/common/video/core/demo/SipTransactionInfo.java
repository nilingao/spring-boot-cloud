package cn.com.nla.common.video.core.demo;

import gov.nist.javax.sip.message.SIPMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SipTransactionInfo implements Serializable {

    private String callId;
    private String fromTag;
    private String toTag;
    private String viaBranch;
    // 心跳未回复次数(平台)
    private int keepAliveReply = 0;
    // 注册未回复次数(平台)
    private int registerAliveReply = 0;


    public void sipTransactionInfo(SIPMessage response){
        this.callId = response.getCallIdHeader().getCallId();
        this.fromTag = response.getFromTag();
        this.toTag = response.getToTag();
        this.viaBranch = response.getTopmostViaHeader().getBranch();
    }
    public SipTransactionInfo(SIPMessage response) {
        this.callId = response.getCallIdHeader().getCallId();
        this.fromTag = response.getFromTag();
        this.toTag = response.getToTag();
        this.viaBranch = response.getTopmostViaHeader().getBranch();
    }
}
