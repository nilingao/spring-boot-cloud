package cn.com.nla.common.video.core.service.video;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.common.CatalogEventConstant;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.GbStreamVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.basic.vo.video.PlatformCatalogVo;
import cn.com.nla.common.video.core.demo.NotifySubscribeInfo;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommanderForPlatform;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class PlatformCatalogVoService {

    @Autowired
    protected SipServer sipServer;
    @Autowired
    private SIPCommanderForPlatform sipCommanderForPlatform;

    public abstract PlatformCatalogVo findId(String id);
    public abstract PlatformCatalogVo findChannelId(String channelId);
    // 目录与上级平台的关系
    public abstract List<DeviceChannelVo> queryCatalogInPlatform(String serverGbId);

    public void sendCatalogMsg(String app,String stream,String type){
        GbStreamVoService gbStreamVoService = VideoService.getGbStreamService();
        GbStreamVo appStream = gbStreamVoService.findAppStream(app, stream);
        if(appStream == null){
            return;
        }
        sendCatalogMsg(Collections.singletonList(appStream),type);
    }

    public  void sendCatalogMsg(List<GbStreamVo> asList, String type){
        if(asList == null || type.isEmpty()){
            return;
        }
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        for (GbStreamVo gbStreamVo : asList) {
            if(StringUtils.isEmpty(gbStreamVo.getGbId())){
                continue;
            }
            List<ParentPlatformVo> parentPlatformVos = parentPlatformVoService.queryPlatFormListForStreamWithGBId(gbStreamVo.getApp(), gbStreamVo.getStream(), null);
            for (ParentPlatformVo parentPlatformVo : parentPlatformVos) {
                handleCatalogEvent(type,parentPlatformVo.getServerGbId(),null,Collections.singletonList(gbStreamVo),null);
            }
        }
    }
    /**
     * 处理目录事件
     * @param type 事件类型
     * @param platformId 平台国标编号
     * @param deviceChannels 设备通道信息
     * @param gbStreams 国标流信息
     */
    public void handleCatalogEvent(String type,String platformId,List<DeviceChannelVo> deviceChannels,List<GbStreamVo> gbStreams,List<PlatformCatalogVo> platformCatalogs){
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();

        Map<String, ParentPlatformVo> parentPlatformMap = new HashMap<>();
        Map<String, List<DeviceChannelVo>> deviceChannelMap = new HashMap<>();

        if(StringUtils.isNotEmpty(platformId)){
            List<DeviceChannelVo> deviceChannelVoList = new ArrayList<>();
            ParentPlatformVo platformVo = parentPlatformVoService.getParentPlatformByServerGbId(platformId);
            if (platformVo == null || platformVo.getStatus() == ConstEnum.Flag.NO.getValue()) {
                return;
            }
            if(deviceChannels != null && deviceChannels.size() > 0){
                deviceChannelVoList.addAll(deviceChannels);
            }
            if(gbStreams != null  && !gbStreams.isEmpty()){
                for (GbStreamVo gbStream : gbStreams) {
                    DeviceChannelVo deviceChannelVo = DeviceChannelVo.getDeviceChannelListByGbStream(platformVo, gbStream, findId(gbStream.getCatalogId()));
                    deviceChannelVoList.add(deviceChannelVo);
                }
            }
            if(platformCatalogs != null && !platformCatalogs.isEmpty()){
                for (PlatformCatalogVo platformCatalog : platformCatalogs) {
                    DeviceChannelVo deviceChannelVo = DeviceChannelVo.getDeviceChannelListByPlatformCatalog(platformVo, platformCatalog);
                    deviceChannelVoList.add(deviceChannelVo);
                }
            }
            parentPlatformMap.put(platformVo.getServerGbId(),platformVo);
            deviceChannelMap.put(platformVo.getServerGbId(),deviceChannelVoList);
        }else {
            List<String> allPlatformId = RedisService.getPlatformNotifySubscribeManager().getAllCatalogSubscribePlatform();
            if(allPlatformId.isEmpty()){
                return;
            }
            List<ParentPlatformVo> platformByDeviceGbIdList = parentPlatformVoService.getParentPlatformByServerGbIdList(allPlatformId);
            if(platformByDeviceGbIdList.isEmpty()){
                log.info("[Catalog事件: {}] 异常 未获取到上级平台信息：{}",type, JSONUtil.toJsonStr(allPlatformId));
                return;
            }
            parentPlatformMap = platformByDeviceGbIdList.stream().collect(Collectors.toMap(ParentPlatformVo::getServerGbId,o->o));
            if(deviceChannels != null && deviceChannels.size() > 0){
                for (DeviceChannelVo deviceChannel : deviceChannels) {
                    DeviceChannelVo deviceChannelVo = deviceChannelVoService.findChannelId(deviceChannel.getChannelId());
                    if(deviceChannelVo == null){
                        continue;
                    }
                    List<ParentPlatformVo> parentPlatformsForGB = parentPlatformVoService.queryPlatFormListForGBWithGBId(deviceChannel.getChannelId(),allPlatformId);
                    for (ParentPlatformVo platformVo : parentPlatformsForGB) {
                        List<DeviceChannelVo> deviceChannelVoList = deviceChannelMap.computeIfAbsent(platformVo.getServerGbId(),k->new ArrayList<DeviceChannelVo>());
                        deviceChannelVoList.add(deviceChannelVo);
                    }
                }
            }
            if(gbStreams != null  && gbStreams.size() > 0){
                for (GbStreamVo gbStream : gbStreams) {
                    List<ParentPlatformVo> parentPlatformsForGB = parentPlatformVoService.queryPlatFormListForStreamWithGBId(gbStream.getApp(),gbStream.getStream(),allPlatformId);
                    for (ParentPlatformVo platformVo : parentPlatformsForGB) {
                        List<DeviceChannelVo> deviceChannelVoList = deviceChannelMap.computeIfAbsent(platformVo.getServerGbId(),k->new ArrayList<DeviceChannelVo>());
                        DeviceChannelVo deviceChannelVo = DeviceChannelVo.getDeviceChannelListByGbStream(platformVo, gbStream, findId(gbStream.getCatalogId()));
                        deviceChannelVoList.add(deviceChannelVo);
                    }
                }
            }
            if(platformCatalogs != null  && platformCatalogs.size() > 0){
                for (PlatformCatalogVo platformCatalog : platformCatalogs) {
                    ParentPlatformVo platformVo = parentPlatformMap.get(platformCatalog.getPlatformId());
                    if( platformVo != null && allPlatformId.contains(platformCatalog.getPlatformId())){
                        List<DeviceChannelVo> deviceChannelVoList = deviceChannelMap.computeIfAbsent(platformCatalog.getPlatformId(),k->new ArrayList<DeviceChannelVo>());
                        DeviceChannelVo deviceChannelVo = DeviceChannelVo.getDeviceChannelListByPlatformCatalog(platformVo,platformCatalog);
                        deviceChannelVoList.add(deviceChannelVo);
                    }
                }
            }
        }
        switch (type){
            case CatalogEventConstant.ON:
            case CatalogEventConstant.OFF:
            case CatalogEventConstant.DEL:
                del(type,parentPlatformMap,deviceChannelMap);
                break;
            case CatalogEventConstant.VLOST:
            case CatalogEventConstant.DEFECT:
                break;
            case CatalogEventConstant.ADD:
            case CatalogEventConstant.UPDATE:
                edit(type,parentPlatformMap,deviceChannelMap);
                break;
            default:
               break;
        }
    }
    private void edit(String type, Map<String, ParentPlatformVo> parentPlatformMap,Map<String, List<DeviceChannelVo>> deviceChannelMap){
        for (Map.Entry<String, List<DeviceChannelVo>> entry : deviceChannelMap.entrySet()) {
            if(entry.getValue().isEmpty()){
                continue;
            }
            ParentPlatformVo platformVo = parentPlatformMap.get(entry.getKey());
            NotifySubscribeInfo catalogSubscribe = RedisService.getPlatformNotifySubscribeManager().getCatalogSubscribe(entry.getKey());
            if(catalogSubscribe != null){
                try {
                    sipCommanderForPlatform.sendNotifyForCatalogAddOrUpdate(sipServer,type, platformVo, entry.getValue(), catalogSubscribe, null,null,null);
                } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                         IllegalAccessException e) {
                    log.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                }
            }
        }
    }

    private void del(String type, Map<String, ParentPlatformVo> parentPlatformMap,Map<String, List<DeviceChannelVo>> deviceChannelMap){
        for (Map.Entry<String, List<DeviceChannelVo>> entry : deviceChannelMap.entrySet()) {
            if(entry.getValue().isEmpty()){
                continue;
            }
            log.info("[Catalog事件: {}]平台：{}，影响通道{}个", type, entry.getKey(), entry.getValue().size());
            ParentPlatformVo platformVo = parentPlatformMap.get(entry.getKey());

            NotifySubscribeInfo catalogSubscribe = RedisService.getPlatformNotifySubscribeManager().getCatalogSubscribe(entry.getKey());
            if(catalogSubscribe != null){
                log.info("[Catalog事件: {}]平台：{}，影响通道{}个", type, entry.getKey(), entry.getValue().size());
                try {
                    sipCommanderForPlatform.sendNotifyForCatalogOther(sipServer,type, platformVo, entry.getValue(), catalogSubscribe, null,null,error->{
                        del(type,parentPlatformMap,deviceChannelMap);
                    });
                } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                         IllegalAccessException e) {
                    log.error("[命令发送失败] 国标级联 Catalog通知: {}", e.getMessage());
                }
            }
        }
    }

}
