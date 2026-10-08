package cn.com.nla.message.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.message.domain.MobileMessageTemplate;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 短信模板业务对象 sms_mobile_message_template
 *
 * @author TZY
 */
@Data
@AutoMapper(target = MobileMessageTemplate.class, reverseConvertGenerate = false)
public class MobileMessageTemplateBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 短信渠道配置id
     */
    @NotNull(message = "渠道配置不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long configId;

    /**
     * 模板编号
     */
    private String code;

    /**
     * 模板类型
     */
    @NotNull(message = "模板类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer type;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容（含变量占位）
     */
    @NotBlank(message = "模板内容不能为空", groups = {AddGroup.class, EditGroup.class})
    private String content;

    /**
     * 接收人
     */
    private String receiver;

    /**
     * 变量定义（JSON）
     */
    private String variable;

    /**
     * 查询参数
     */
    private Map<String, Object> params = new HashMap<>();

}
