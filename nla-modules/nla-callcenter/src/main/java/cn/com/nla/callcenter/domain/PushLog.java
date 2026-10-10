package cn.com.nla.callcenter.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.io.Serial;

/** 话单推送记录表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_push_log")
public class PushLog extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业id。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** callid。 */
    @TableField(value = "`call_id`")
    private Long callId;

    /** 发送次数。 */
    @TableField(value = "`cdr_notify_url`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String cdrNotifyUrl;

    /** 推送内容。 */
    @TableField(value = "`content`")
    private String content;

    /** 推送次数。 */
    @TableField(value = "`push_times`")
    private Integer pushTimes;

    /** 推送返回值。 */
    @TableField(value = "`push_response`")
    private String pushResponse;

    /** 状态(1:推送，0:不推送)。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
