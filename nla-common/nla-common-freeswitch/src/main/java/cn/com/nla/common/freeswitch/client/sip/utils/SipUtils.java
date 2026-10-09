package cn.com.nla.common.freeswitch.client.sip.utils;

import cn.com.nla.common.freeswitch.enums.sip.VideoStreamType;
import cn.com.nla.common.freeswitch.vo.sip.Address;
import cn.com.nla.common.freeswitch.vo.sip.DeviceRawContent;
import cn.com.nla.common.freeswitch.vo.sip.Gb28181Sdp;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.ObjectUtils;

import javax.sdp.*;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import java.net.InetAddress;
import java.util.Vector;

/**
 * SIP的工具类
 */
@Slf4j
public class SipUtils {

    public static String getUserIdFromHeader(Request request) {
        FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
        return getUserIdFromHeader(fromHeader);
    }

    public static String getUserIdFromHeader(FromHeader fromHeader) {
        AddressImpl address = (AddressImpl)fromHeader.getAddress();
        SipUri uri = (SipUri) address.getURI();
        return uri.getUser();
    }

    public static String getUserIdToHeader(Request request) {
        ToHeader fromHeader = (ToHeader)request.getHeader(ToHeader.NAME);
        return getUserIdToHeader(fromHeader);
    }
    public static String getUserIdToHeader(ToHeader toHeader) {
        AddressImpl address = (AddressImpl)toHeader.getAddress();
        SipUri uri = (SipUri) address.getURI();
        return uri.getUser();
    }



    public static  String getNewViaTag() {
        return "z9hG4bKPj" + getNewFromTag();
    }

    public static String getNewFromTag(){
        return RandomStringUtils.randomAlphanumeric(32);
    }

    public static String getNewTag(){
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * 从请求中获取设备ip地址和端口号
     * @param request 请求
     * @param sipUseSourceIpAsRemoteAddress  false 从via中获取地址， true 直接获取远程地址
     * @return 地址信息
     */
    public static Address getRemoteAddressFromRequest(SIPRequest request, boolean sipUseSourceIpAsRemoteAddress) {
        String remoteAddress;
        int remotePort;
        if (sipUseSourceIpAsRemoteAddress) {
            remoteAddress = request.getPeerPacketSourceAddress().getHostAddress();
            remotePort = request.getPeerPacketSourcePort();
        }else {
            // 判断RPort是否改变，改变则说明路由nat信息变化，修改设备信息
            // 获取到通信地址等信息
            Via topmostViaHeader = (Via) request.getTopmostViaHeader();
            remoteAddress = topmostViaHeader.getReceived();
            remotePort = topmostViaHeader.getRPort();
            // 解析本地地址替代
            if (ObjectUtils.isEmpty(remoteAddress) || remotePort == -1) {
                InetAddress peerPacketSourceAddress = request.getPeerPacketSourceAddress();
                if(peerPacketSourceAddress ==null){
                    remoteAddress = request.getRemoteAddress().getHostAddress();
                    remotePort = request.getRemotePort();
                }else {
                    remoteAddress = request.getPeerPacketSourceAddress().getHostAddress();
                    remotePort = request.getPeerPacketSourcePort();
                }
            }
        }
        return Address.builder().ip(remoteAddress).port(remotePort).build();
    }

    public static String getSsrcFromSdp(String sdpStr) {

        // jainSip不支持y= f=字段， 移除以解析。
        int ssrcIndex = sdpStr.indexOf("y=");
        if (ssrcIndex == 0) {
            return null;
        }
        String lines[] = sdpStr.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().startsWith("y=")) {
                return line.substring(2);
            }
        }
        return null;
    }

    public static Gb28181Sdp parseSDP(String sdpStr) throws SdpParseException {
        // jainSip不支持y= f=字段， 移除以解析。
        int ssrcIndex = sdpStr.indexOf("y=");
        int mediaDescriptionIndex = sdpStr.indexOf("f=");
        // 检查是否有y字段
        SessionDescription sdp;
        String ssrc = null;
        String mediaDescription = null;
        if (mediaDescriptionIndex == 0 && ssrcIndex == 0) {
            sdp = SdpFactory.getInstance().createSessionDescription(sdpStr);
        }else {
            String lines[] = sdpStr.split("\\r?\\n");
            StringBuilder sdpBuffer = new StringBuilder();
            for (String line : lines) {
                if (line.trim().startsWith("y=")) {
                    ssrc = line.substring(2);
                }else if (line.trim().startsWith("f=")) {
                    mediaDescription = line.substring(2);
                }else {
                    sdpBuffer.append(line.trim()).append("\r\n");
                }
            }
            sdp = SdpFactory.getInstance().createSessionDescription(sdpBuffer.toString());
        }
        return Gb28181Sdp.getInstance(sdp, ssrc, mediaDescription);
    }

    public static DeviceRawContent handleDeviceRawContent(Message response) {
        // 解析sdp消息, 使用jainsip 自带的sdp解析方式
        DeviceRawContent deviceRawContent = new DeviceRawContent();
        try {
            String contentString = new String(response.getRawContent());
            Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
            SessionDescription sdp = gb28181Sdp.getBaseSdb();
            String sessionName = sdp.getSessionName().getValue();
            //  获取支持的格式
            Vector mediaDescriptions = sdp.getMediaDescriptions(true);
            // 查看是否支持PS 负载96
            //String ip = null;
            int port = -1;
            String downloadSpeed = "1";
            boolean mediaTransmissionTCP = false;
            boolean tcpActive = false;
            String ssrc  = gb28181Sdp.getSsrc();
            String username = sdp.getOrigin().getUsername();
            String addressStr = sdp.getOrigin().getAddress();


            for (Object description : mediaDescriptions) {
                MediaDescription mediaDescription = (MediaDescription) description;
                Media media = mediaDescription.getMedia();
                downloadSpeed = mediaDescription.getAttribute("downloadspeed");
                Vector mediaFormats = media.getMediaFormats(false);
                String mediaFormat = null;
                if (mediaFormats.contains(String.valueOf(VideoStreamType.CALL_AUDIO_PHONE.getPt()))) {
                    mediaFormat = String.valueOf(VideoStreamType.CALL_AUDIO_PHONE.getPt());
                }else if(mediaFormats.contains(String.valueOf(VideoStreamType.CALL_VIDEO_PHONE.getPt()))){
                    mediaFormat = String.valueOf(VideoStreamType.CALL_VIDEO_PHONE.getPt());
                }else if(mediaFormats.contains("103")){//也标识 h264
                    mediaFormat = String.valueOf(VideoStreamType.CALL_VIDEO_PHONE.getPt());
                }else {
                    continue;
                }
                port = media.getMediaPort();
                if (port == -1) {
                    log.info("不支持的媒体格式，返回415");
                    return null;
                }
                //String mediaType = media.getMediaType();
                String protocol = media.getProtocol();
                // 区分TCP发流还是udp， 当前默认udp
                if ("TCP/RTP/AVP".equalsIgnoreCase(protocol)) {
                    String setup = mediaDescription.getAttribute("setup");
                    if (setup != null) {
                        mediaTransmissionTCP = true;
                        if ("active".equalsIgnoreCase(setup)) {
                            tcpActive = true;
                        } else if ("passive".equalsIgnoreCase(setup)) {
                            tcpActive = false;
                        }
                    }
                }

                deviceRawContent.setDeviceInfo(mediaFormat,
                        DeviceRawContent.DeviceInfo.builder()
                                .username(username)
                                .addressStr(addressStr)
                                .downloadSpeed(downloadSpeed)
                                .port(port)
                                .ssrc(ssrc)
                                .mediaTransmissionTCP(mediaTransmissionTCP)
                                .tcpActive(tcpActive)
                                .sessionName(sessionName)
                                .build());
                ;
            }
        }catch (SdpException | SipException e){
            log.error("解析Sip数据失败：",e);
            return null;
        }
        return deviceRawContent;
    }

}
