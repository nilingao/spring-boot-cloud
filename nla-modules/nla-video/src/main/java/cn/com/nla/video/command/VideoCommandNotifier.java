package cn.com.nla.video.command;

import cn.com.nla.video.domain.vo.VideoCommandResultVo;

/**
 * 视频指令异步回执通知器，把协议侧结果投递给指令发起用户。
 *
 * @author TZY
 */
public interface VideoCommandNotifier {

    /**
     * 推送一条指令回执。
     *
     * @param userId 指令发起用户，为空表示协议侧/匿名触发，不投递
     * @param result 回执内容
     */
    void push(Long userId, VideoCommandResultVo result);
}
