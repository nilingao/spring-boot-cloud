package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.cmd;

import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.basic.vo.sip.RecordInfo;
import cn.com.nla.common.video.basic.vo.sip.RecordItem;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.properties.VideoProperties;
import cn.com.nla.common.video.core.redis.subscribe.record.RecordEndSubscribeHandle;
import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.ResponseMessageHandler;
import cn.com.nla.common.video.core.utils.XmlUtils;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 录像查询应答
 */
@Slf4j
public class RecordInfoResponseMessageHandler  extends SipResponseEvent implements MessageHandler {

    @Autowired
    private VideoProperties videoProperties;
    @Autowired
    private DeferredResultHolder deferredResultHolder;
    @Autowired
    private RecordEndSubscribeHandle recordEndSubscribeHandle;

    public RecordInfoResponseMessageHandler(ResponseMessageHandler handler){
        handler.setMessageHandler(CmdType.RECORD_INFO_RESPONSE.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        try {
            // 回复200 OK
            responseAck((SIPRequest) evt.getRequest(), Response.OK,null);
        }catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 国标录像: {}", e.getMessage());
        }
        ThreadUtil.execute(()->{
            try {
                String sn = XmlUtils.getText(element, "SN");
                String channelId = XmlUtils.getText(element, "DeviceID");
                RecordInfo recordInfo = new RecordInfo();
                recordInfo.setChannelId(channelId);
                recordInfo.setDeviceId(deviceVo.getDeviceId());
                recordInfo.setSn(sn);
                recordInfo.setName(XmlUtils.getText(element, "Name"));
                String sumNumStr = XmlUtils.getText(element, "SumNum");
                int sumNum = 0;
                if (!ObjectUtils.isEmpty(sumNumStr)) {
                    sumNum = Integer.parseInt(sumNumStr);
                }
                recordInfo.setSumNum(sumNum);
                Element recordModel = XmlUtil.getElement(element, "RecordList");
                List<Element> recordListElement = XmlUtil.getElements(recordModel, "Item");
                if (recordListElement.isEmpty() || sumNum == 0) {
                    log.info("无录像数据");
                    recordInfo.setCount(sumNum);
                    recordEndSubscribeHandle.handlerEvent(recordInfo);
                    releaseRequest(deviceVo.getDeviceId(), sn,recordInfo);
                } else if(! recordListElement.isEmpty()){
                    List<RecordItem> recordList = new ArrayList<>();
                    for (Element itemRecord : recordListElement) {
                        String deviceId= XmlUtils.getText(itemRecord, "DeviceID");
                        if (StringUtils.isEmpty(deviceId)) {
                            log.info("记录为空，下一个...");
                            continue;
                        }
                        RecordItem record = new RecordItem();
                        record.setDeviceId(XmlUtils.getText(itemRecord, "DeviceID"));
                        record.setName(XmlUtils.getText(itemRecord, "Name"));
                        record.setFilePath(XmlUtils.getText(itemRecord, "FilePath"));
                        record.setFileSize(XmlUtils.getText(itemRecord, "FileSize"));
                        record.setAddress(XmlUtils.getText(itemRecord, "Address"));

                        String startTimeStr = XmlUtils.getText(itemRecord, "StartTime");
                        record.setStartTime(DateUtil.formatDateTime(DateUtil.parse(startTimeStr, DatePattern.UTC_SIMPLE_PATTERN)));

                        String endTimeStr = XmlUtils.getText(itemRecord, "EndTime");
                        record.setEndTime(DateUtil.formatDateTime(DateUtil.parse(endTimeStr,DatePattern.UTC_SIMPLE_PATTERN)));

                        record.setSecrecy(StringUtils.isEmpty(XmlUtils.getText(itemRecord,"Secrecy"))? 0 : Integer.parseInt(XmlUtils.getText(itemRecord, "Secrecy")));
                        record.setType(XmlUtils.getText(itemRecord, "Type"));
                        record.setRecorderId(XmlUtils.getText(itemRecord, "RecorderID"));
                        recordList.add(record);
                    }
                    recordInfo.setRecordList(recordList);
                    recordInfo.setCount(Math.toIntExact(recordList.size()));
                    recordEndSubscribeHandle.handlerEvent(recordInfo);
                    //数据有可能分次传输
                    String key = String.format("%s%s:%s", VideoConstant.REDIS_RECORD_INFO_RES_PRE, channelId, sn);
                    String countKey = String.format("%s%s:%s", VideoConstant.REDIS_RECORD_INFO_RES_COUNT_PRE, channelId, sn);
                    Map<String, Object> collect = recordList.stream().collect(Collectors.toMap(o -> o.getStartTime() + o.getEndTime(), o->o,(o1, o2)->o2));
                    VideoCache.putMap(key,collect,videoProperties.getPlayTimeout());
                    long incr =  VideoCache.increment(countKey,recordList.size(),videoProperties.getPlayTimeout());
                    if(incr < sumNum){
                        return;
                    }
                    RedisUtils.deleteObject(countKey);
                    // 已接收完成
                    Map<String, RecordItem> map =VideoCache.<RecordItem>readMap(key);
                    RedisUtils.deleteObject(key);
                    RecordInfo build = RecordInfo.builder()
                            .deviceId(recordInfo.getDeviceId())
                            .channelId(recordInfo.getChannelId())
                            .sn(recordInfo.getSn())
                            .name(recordInfo.getName())
                            .count(map.size())
                            .sumNum(recordInfo.getSumNum())
                            .recordList(new ArrayList<>(map.values()))
                            .build();
                    releaseRequest(deviceVo.getDeviceId(), sn,build);
                }
            } catch (Exception e) {
                log.error("[国标录像] 发现未处理的异常, \r\n{}", evt.getRequest());
                log.error("[国标录像] 异常内容： ", e);
            }
        });
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {

    }

    private void releaseRequest(String deviceId, String sn,RecordInfo recordInfo){
        String key = String.format("%s%s_%s",DeferredResultHolder.CALLBACK_CMD_RECORDINFO,deviceId,sn);
        // 对数据进行排序
        if(recordInfo!=null && recordInfo.getRecordList()!=null) {
            Collections.sort(recordInfo.getRecordList());
        }else{
            recordInfo.setRecordList(new ArrayList<>());
        }
        deferredResultHolder.invokeAllResult(key, ProtocolResult.result(RespCode.CODE_0.getValue(),null,recordInfo));
    }
}
