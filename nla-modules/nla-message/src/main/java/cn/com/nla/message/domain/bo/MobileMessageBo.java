package cn.com.nla.message.domain.bo;

import cn.com.nla.message.domain.MobileMessage;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 短信发送记录业务对象 sms_mobile_message
 * <p>
 * 记录型查询载体：仅用于列表/导出过滤，不提供新增与修改。
 * </p>
 *
 * @author TZY
 */
@Data
@AutoMapper(target = MobileMessage.class, reverseConvertGenerate = false)
public class MobileMessageBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 发送人id
     */
    private Long senderId;

    /**
     * 模板号
     */
    private String templateId;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 内容
     */
    private String content;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 操作时间
     */
    private LocalDateTime handleTime;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 响应编号
     */
    private String msgId;

    /**
     * 返回状态（回执）
     */
    private String callbackStatus;

    /**
     * 重发次数
     */
    private Integer resendNum;

    /**
     * 变量（JSON）
     */
    private String variable;

    /**
     * 查询参数
     */
    private Map<String, Object> params = new HashMap<>();

}
