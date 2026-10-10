package cn.com.nla.common.video.core.redis.subscribe.sip.register;

import cn.com.nla.common.video.core.redis.VideoMessageListener;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.properties.SipConfigProperties;
import cn.com.nla.common.video.core.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;


@Slf4j
public class SipRegisterHandle extends VideoMessageListener {

    @Autowired
    private SipConfigProperties sipConfigProperties;

    public SipRegisterHandle() {
        super(VideoConstant.VIDEO_SEND_SIP_REGISTER_MESSAGE);
    }

    @Override
    public void onMessage(byte[] payload) {
        Object body = org.springframework.util.SerializationUtils.deserialize(payload);
        ParentPlatformVo vo = (ParentPlatformVo) body;
        if(vo == null || !sipConfigProperties.getId().equals(vo.getDeviceGbId())){
            return;
        }
        //触发服务注册
        VideoService.getParentPlatformService().login(vo);
    }
}
