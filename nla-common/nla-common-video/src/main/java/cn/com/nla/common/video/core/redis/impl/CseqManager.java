package cn.com.nla.common.video.core.redis.impl;

import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.core.properties.VideoProperties;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class CseqManager {

    @Autowired
    private VideoProperties videoProperties;

    private final String SIP_CSEQ_PREFIX = VideoConstant.SIP_CSEQ_PREFIX;
    /**
     * 生成序列号
     * @return
     */
    public  long getCSEQ(){
        String key = SIP_CSEQ_PREFIX + videoProperties.getServerId();
        long incr = VideoCache.increment(key, 1L,0);
        if(incr > Integer.MAX_VALUE){
            VideoCache.set(key,1);
            incr = 1;
        }
        return incr;
    }
}
