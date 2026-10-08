package cn.com.nla.workflow.api.domain;

/**
 * 抄送
 *
 * @param userId   抄送用户 ID
 * @param nickName 抄送用户昵称
 * @author TZY
 */
public record FlowCopyDTO(
    Long userId,
    String nickName
) {
}
