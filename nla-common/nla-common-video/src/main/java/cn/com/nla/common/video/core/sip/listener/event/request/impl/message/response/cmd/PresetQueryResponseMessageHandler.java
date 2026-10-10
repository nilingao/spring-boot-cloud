package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.cmd;

import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.enums.CharsetType;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.basic.vo.sip.PresetQuerySipReq;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.ResponseMessageHandler;
import cn.com.nla.common.video.core.utils.XmlUtils;
import cn.hutool.core.util.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备预置位查询应答
 */
@Slf4j
public class PresetQueryResponseMessageHandler extends SipResponseEvent implements MessageHandler {

    @Autowired
    private DeferredResultHolder deferredResultHolder;

    public PresetQueryResponseMessageHandler(ResponseMessageHandler handler){
        handler.setMessageHandler(CmdType.PRESET_QUERY_RESPONSE.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        SIPRequest request = (SIPRequest) evt.getRequest();
        Element rootElement = getRootElement(evt, CharsetType.getName(deviceVo.getCharset()));
        if (rootElement == null) {
            log.warn("[ 设备预置位查询应答 ] content cannot be null, {}", evt.getRequest());
            try {
                responseAck(request, Response.BAD_REQUEST,null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                log.error("[命令发送失败] 设备预置位查询应答处理: {}", e.getMessage());
            }
            return;
        }
        Element presetModel = XmlUtil.getElement(rootElement, "PresetList");
        String sn =  XmlUtils.getText(rootElement, "SN");
        //该字段可能为通道或则设备的id
        String deviceId = XmlUtils.getText(rootElement, "DeviceID");
        String key = String.format("%s%s",DeferredResultHolder.CALLBACK_CMD_PRESETQUERY, deviceId);
        if (StringUtils.isEmpty(sn) || presetModel == null) {
            try {
                responseAck(request, Response.BAD_REQUEST, "xml error");
            } catch (InvalidArgumentException | ParseException | SipException e) {
                log.error("[命令发送失败] 设备预置位查询应答处理: {}", e.getMessage());
            }
            return;
        }
        List<PresetQuerySipReq> presetQuerySipReqList = new ArrayList<>();
        List<Element> presetList = XmlUtil.getElements(presetModel, "Item");
        for (Element itemOne : presetList) {
            PresetQuerySipReq presetQuerySipReq = new PresetQuerySipReq();
            // 遍历item
            String PresetID = XmlUtils.getText(itemOne, "PresetID");
            String PresetName = XmlUtils.getText(itemOne, "PresetName");
            presetQuerySipReq.setPresetId(PresetID);
            presetQuerySipReq.setPresetName(PresetName);
            presetQuerySipReqList.add(presetQuerySipReq);
        }
        deferredResultHolder.invokeAllResult(key, ProtocolResult.result(RespCode.CODE_0.getValue(),null,presetQuerySipReqList));
        try {
            responseAck(request, Response.OK,null);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[命令发送失败] 设备预置位查询应答处理: {}", e.getMessage());
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {

    }
}
