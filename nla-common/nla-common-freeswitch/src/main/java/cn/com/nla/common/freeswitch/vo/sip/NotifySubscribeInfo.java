package cn.com.nla.common.freeswitch.vo.sip;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.sip.header.EventHeader;
import java.io.Serializable;

@Data
@NoArgsConstructor
public class NotifySubscribeInfo implements Serializable {
    private String id;//设备通道国标编号
    private SIPRequest request;
    private int expires;
    private String eventId;
    private String eventType;
    private SIPResponse response;
    private String sn;
    private int gpsInterval;

    public NotifySubscribeInfo(SIPRequest request, String id) {
        this.id = id;
        this.request = request;
        this.expires = request.getExpires().getExpires();
        EventHeader eventHeader = (EventHeader)request.getHeader(EventHeader.NAME);
        this.eventId = eventHeader.getEventId();
        this.eventType = eventHeader.getEventType();

    }
}
