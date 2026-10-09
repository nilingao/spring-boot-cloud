package cn.com.nla.common.freeswitch.redis.subscribe.sip.register;

import cn.com.nla.common.freeswitch.client.sip.properties.SipConfigProperties;
import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.service.SipService;
import cn.com.nla.common.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import jakarta.annotation.PostConstruct;


@Slf4j
@Component
public class SipRegisterHandle {

    @Autowired
    private SipConfigProperties sipConfigProperties;

    @PostConstruct
    public void subscribe() {
        RedisUtils.subscribe(SipConstant.VIDEO_SEND_SIP_REGISTER_MESSAGE, byte[].class, this::onMessage);
    }

    public void onMessage(byte[] body) {
        AgentVoInfo vo = (AgentVoInfo) SerializationUtils.deserialize(body);
        if(vo == null){
            return;
        }
        //触发服务注册
        SipService.getParentPlatformService().login(vo,null,null);
    }
}
