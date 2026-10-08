package cn.com.nla.workflow.domain.vo;

import lombok.Data;
import cn.com.nla.common.translation.annotation.Translation;
import cn.com.nla.common.translation.constant.TransConstant;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程抄送视图对象。
 *
 * @author TZY
 */
@Data
public class FlowCopyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户昵称
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "userId")
    private String nickName;

    /**
     * 使用用户 ID 构造抄送对象。
     *
     * @param userId 用户ID
     */
    public FlowCopyVo(Long userId) {
        this.userId = userId;
    }

}
