package cn.com.nla.message.domain.vo;

import cn.com.nla.message.domain.MobileMessage;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 短信发送记录视图对象 sms_mobile_message
 *
 * @author TZY
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = MobileMessage.class)
public class MobileMessageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 发送人id
     */
    private Long senderId;

    /**
     * 模板号
     */
    @ExcelProperty(value = "模板号")
    private String templateId;

    /**
     * 类型
     */
    @ExcelProperty(value = "类型")
    private Integer type;

    /**
     * 内容
     */
    @ExcelProperty(value = "内容")
    private String content;

    /**
     * 手机号
     */
    @ExcelProperty(value = "手机号")
    private String mobile;

    /**
     * 操作时间
     */
    @ExcelProperty(value = "操作时间")
    private LocalDateTime handleTime;

    /**
     * 状态
     */
    @ExcelProperty(value = "状态")
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
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

}
