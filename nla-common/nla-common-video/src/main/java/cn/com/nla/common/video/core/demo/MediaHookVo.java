package cn.com.nla.common.video.core.demo;

import cn.com.nla.common.video.basic.enums.HookType;
import cn.com.nla.common.video.basic.vo.media.HookVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 流媒体信息回调消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaHookVo implements Serializable {
    /**
     * 发送类型
     */
    private HookType type;

    /**
     * 是否发送所有 1.是 0.否
     */
    @Builder.Default
    private int onAll = 0;
    /**
     * 流媒体信息
     */
    private MediaServerVo mediaServerVo;
    /**
     * 流媒体相应信息
     */
    private HookVo hookVo;
}
