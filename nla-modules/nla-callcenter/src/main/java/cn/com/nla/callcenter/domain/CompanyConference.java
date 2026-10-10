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

/** 企业会议室表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_company_conference")
public class CompanyConference extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业id。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 会议室名。 */
    @TableField(value = "`name`")
    private String name;

    /** 会议室号码。 */
    @TableField(value = "`code`")
    private String code;

    /** 会议室密码。 */
    @TableField(value = "`password`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 使用状态(1.未使用 1.使用中)。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 启用状态(0.禁用 1.启用)。 */
    @TableField(value = "`enable`")
    private Integer enable;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
