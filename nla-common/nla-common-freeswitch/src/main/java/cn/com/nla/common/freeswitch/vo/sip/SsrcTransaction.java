package cn.com.nla.common.freeswitch.vo.sip;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SsrcTransaction {

    private String agentKey;
    private String callId;
    private String app;
    private String stream;
    private String mediaServerId;
    private String ssrc;
    private SipTransactionInfo sipTransactionInfo;
    private String typeName;
}
