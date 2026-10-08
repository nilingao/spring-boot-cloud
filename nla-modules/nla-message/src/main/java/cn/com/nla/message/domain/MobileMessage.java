package cn.com.nla.message.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 短信发送记录对象 sms_mobile_message
 * <p>
 * 每次发送落库，用于审计与回执追踪（追加型记录，不做逻辑删除）。
 * </p>
 *
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sms_mobile_message")
public class MobileMessage extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
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

}
