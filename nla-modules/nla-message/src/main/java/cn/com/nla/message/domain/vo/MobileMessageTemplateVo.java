package cn.com.nla.message.domain.vo;

import cn.com.nla.message.domain.MobileMessageTemplate;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 短信模板视图对象 sms_mobile_message_template
 *
 * @author TZY
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = MobileMessageTemplate.class)
public class MobileMessageTemplateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 短信渠道配置id
     */
    @ExcelProperty(value = "渠道配置id")
    private Long configId;

    /**
     * 模板编号
     */
    @ExcelProperty(value = "模板编号")
    private String code;

    /**
     * 模板类型
     */
    @ExcelProperty(value = "模板类型")
    private Integer type;

    /**
     * 标题
     */
    @ExcelProperty(value = "标题")
    private String title;

    /**
     * 内容
     */
    @ExcelProperty(value = "内容")
    private String content;

    /**
     * 接收人
     */
    @ExcelProperty(value = "接收人")
    private String receiver;

    /**
     * 变量定义（JSON）
     */
    private String variable;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

}
