package cn.com.nla.common.video.core.config.runner;

import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.vo.sip.Address;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.properties.SipConfigProperties;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * 系统启动时控制上级平台重新注册
 */
@Slf4j
@Order(50)
public class SipPlatformRunner implements CommandLineRunner {

    @Autowired
    private SipServer localSipServer;
    @Autowired
    private SipConfigProperties sipConfigProperties;

    @Override
    public void run(String... args) throws Exception {
        List<ParentPlatformVo> parentPlatformVoList = VideoService.getParentPlatformService().getParentPlatformByDeviceGbId(sipConfigProperties.getId());
        if(parentPlatformVoList == null || parentPlatformVoList.isEmpty()){
            return;
        }
        for (ParentPlatformVo parentPlatformVo : parentPlatformVoList) {
            // 设置所有平台离线
            VideoService.getParentPlatformService().offline(parentPlatformVo);
            // 先注销然后注册
            RedisService.getRegisterServerManager().putPlatform(parentPlatformVo.getServerGbId(),parentPlatformVo.getKeepTimeout()+ VideoConstant.DELAY_TIME, Address.builder().gbId(parentPlatformVo.getServerGbId()).ip(localSipServer.getAdvertisedIp(null)).port(localSipServer.getSipConfigProperties().getPort()).build());
            VideoService.getParentPlatformService().unregister(parentPlatformVo, (eventResult)->{
                VideoService.getParentPlatformService().login(parentPlatformVo);
            } ,null);
        }
    }
}
