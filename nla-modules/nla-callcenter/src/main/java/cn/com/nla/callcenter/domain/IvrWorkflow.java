package cn.com.nla.callcenter.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/** ivr流程表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_ivr_workflow")
public class IvrWorkflow extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业id。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 流程名称。 */
    @TableField(value = "`name`")
    private String name;

    /** 流程文件名。 */
    @TableField(value = "`oss_id`")
    private String ossId;

    /** 用来存贮 ivr 流程启动所需要的参数描述。 */
    @TableField(value = "`init_params`")
    private String initParams;

    /** 流程发布人。 */
    @TableField(value = "`create_user`")
    private String createUser;

    /** 流程审核人。 */
    @TableField(value = "`verify_user`")
    private String verifyUser;

    /** 流程内容(ivr)。 */
    @TableField(value = "`content`")
    private String content;

    /** 该流程用到的语音文件id，以英文逗号,分隔。 */
    @TableField(value = "`voice_item`")
    private String voiceItem;

    /** 1转接，2咨询。 */
    @TableField(value = "`type`")
    private Integer type;

    /** 流程状态    1：待发布   2：审核中  3：审核未通过  4：审核通过  5：已上线(ivr)。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
