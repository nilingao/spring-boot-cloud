package cn.com.nla.common.video.core.redis.subscribe.media;

import cn.com.nla.common.video.basic.vo.media.HookVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;

@FunctionalInterface
public interface HookEvent {
    void response(MediaServerVo mediaServerVo, HookVo response);
}
