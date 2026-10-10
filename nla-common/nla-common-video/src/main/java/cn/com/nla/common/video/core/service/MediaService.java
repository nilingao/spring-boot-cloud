package cn.com.nla.common.video.core.service;

import cn.com.nla.common.video.core.redis.subscribe.media.MediaHookSubscribe;
import cn.com.nla.common.video.core.service.authentication.TokenService;
import cn.hutool.extra.spring.SpringUtil;

public class MediaService {

    public static MediaHookSubscribe getMediaHookSubscribe(){
        return SpringUtil.getBean(MediaHookSubscribe.class);
    }

    public static TokenService getTokenService(){
        return SpringUtil.getBean(TokenService.class);
    }

}
