package cn.com.nla.message.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 短信模板对象 sms_mobile_message_template
 * <p>
 * 发送时按 type + configId 取最新模板，对 content 做变量替换（variable 定义变量，JSON）。
 * </p>
 *
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sms_mobile_message_template")
public class MobileMessageTemplate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 短信渠道配置id
     */
    private Long configId;

    /**
     * 模板编号
     */
    private String code;

    /**
     * 模板类型
     */
    private Integer type;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容（含变量占位）
     */
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
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
