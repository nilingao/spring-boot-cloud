package cn.com.nla.common.video.core.service.video;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.common.CatalogEventConstant;
import cn.com.nla.common.video.basic.enums.StreamType;
import cn.com.nla.common.video.basic.vo.media.MediaRestResult;
import cn.com.nla.common.video.basic.vo.video.GbStreamVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.basic.vo.video.StreamProxyVo;
import cn.com.nla.common.video.core.media.client.MediaClient;
import cn.com.nla.common.video.core.service.VideoService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class StreamProxyVoService {

    public abstract StreamProxyVo findAppStream(String app, String stream);

    public abstract void updateEnable(Long id, ConstEnum.Flag flag);

    public abstract int updateStatus( String app, String stream,String mediaServerId,boolean status);

    public abstract int updateStatus( String mediaServerId,boolean status);

    public abstract List<StreamProxyVo> findAutoRemoveMediaServerIdList(String mediaServerId);
    /**
     * 获取是否启用的流信息
     */
    public abstract List<StreamProxyVo> findEnableInMediaServerList(String mediaServerId, boolean enable);

    public abstract int del(String app, String stream);

    public boolean start(StreamProxyVo streamProxyVo){
        if(streamProxyVo ==null){
            return false;
        }
        if(streamProxyVo.getEnable()==ConstEnum.Flag.NO.getValue()){
            log.warn("拉流未启用，请先启用：{}/{}",streamProxyVo.getApp(), streamProxyVo.getStream());
            return false;
        }
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        GbStreamVoService gbStreamVoService = VideoService.getGbStreamService();
        MediaServerVo mediaServerVo = mediaServerVoService.findOnLineMediaServerId(streamProxyVo.getMediaServerId());
        if(mediaServerVo == null){
            mediaServerVo = mediaServerVoService.findMediaServerForMinimumLoad();
        }
        if(mediaServerVo == null){
            log.error("拉流时，未获取流媒体信息：{}、{}/{}",streamProxyVo.getMediaServerId(),streamProxyVo.getApp(), streamProxyVo.getStream());
            return false;
        }
        //开始拉流
        MediaRestResult restResult = MediaClient.addStreamProxyToZlm(mediaServerVo, streamProxyVo);
        if(restResult == null){
            log.error("启用拉流代理失败：{}/{}",streamProxyVo.getApp(), streamProxyVo.getStream());
            return false;
        } else if(restResult.getCode() == RespCode.CODE_0.getValue()){
            updateStatus(streamProxyVo.getApp(),streamProxyVo.getStream(),mediaServerVo.getId(),true);
            GbStreamVo build = GbStreamVo.builder()
                    .app(streamProxyVo.getApp())
                    .stream(streamProxyVo.getStream())
                    .name(streamProxyVo.getName())
                    .streamType(StreamType.PROXY.getValue())
                    .mediaServerId(streamProxyVo.getMediaServerId())
                    .build();
            gbStreamVoService.update(build);
            return true;
        }else {
            log.error("启用拉流代理失败：{}/{}->{}({})",streamProxyVo.getApp(), streamProxyVo.getStream(),restResult.getMsg(), streamProxyVo.getSrcUrl() == null? streamProxyVo.getUrl(): streamProxyVo.getSrcUrl());
            return false;
        }
    }

    public boolean stop(String app, String stream){
        StreamProxyVo streamProxyVo = findAppStream(app, stream);
        if(streamProxyVo == null || streamProxyVo.getEnable() == ConstEnum.Flag.NO.getValue()){
            return false;
        }
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        GbStreamVoService gbStreamVoService = VideoService.getGbStreamService();
        //关闭流
        MediaServerVo mediaServerVo = mediaServerVoService.findOnLineMediaServerId(streamProxyVo.getMediaServerId());
        if(mediaServerVo != null){
            MediaRestResult result = MediaClient.closeStreams(mediaServerVo, "__defaultVhost__", app, stream);
            if(result != null && result.getCode() == RespCode.CODE_0.getValue()){
                updateStatus(streamProxyVo.getApp(),streamProxyVo.getStream(),null,false);
                GbStreamVo build = GbStreamVo.builder()
                        .app(streamProxyVo.getApp())
                        .stream(streamProxyVo.getStream())
                        .name(streamProxyVo.getName())
                        .streamType(StreamType.PROXY.getValue())
                        .mediaServerId(streamProxyVo.getMediaServerId())
                        .build();
                gbStreamVoService.update(build);
                return true;
            }
        }
        return false;
    }

    public void delete(String app, String stream){
        PlatformCatalogVoService platformCatalogVoService = VideoService.getPlatformCatalogService();
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        GbStreamVoService gbStreamVoService = VideoService.getGbStreamService();
        StreamProxyVo appStream = findAppStream(app, stream);
        if(appStream == null){
            return;
        }
        //通州上级删除
        platformCatalogVoService.sendCatalogMsg(app,stream, CatalogEventConstant.DEL);
        //自己删除
        del(app,stream);
        //如果关联了国标那么移除关联
        gbStreamVoService.delAppStream(app,stream);
        //关闭流
        MediaServerVo mediaServerVo = mediaServerVoService.findOnLineMediaServerId(appStream.getMediaServerId());
        if(mediaServerVo != null){
            MediaClient.closeStreams(mediaServerVo, "__defaultVhost__", app, stream);
        }
    }

}
