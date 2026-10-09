package cn.com.nla.common.freeswitch.redis.impl.sip;

import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.freeswitch.client.sip.properties.VideoProperties;
import cn.com.nla.common.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Slf4j
@Component
public class CseqManager {

    @Resource
    private VideoProperties videoProperties;

    private final String SIP_CSEQ_PREFIX = SipConstant.SIP_CSEQ_PREFIX;
    /**
     * 生成序列号
     * @return
     */
    public  long getCSEQ(){
        String key = SIP_CSEQ_PREFIX + videoProperties.getServerId();
        long incr = RedisUtils.incrAtomicValue(key);
        if(incr > Integer.MAX_VALUE){
            RedisUtils.setAtomicValue(key,1);
            incr = 1;
        }
        return incr;
    }
}
