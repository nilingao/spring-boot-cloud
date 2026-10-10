package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.IvrWorkflow;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** ivr流程表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = IvrWorkflow.class, reverseConvertGenerate = false)
public class IvrWorkflowBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业id。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 流程名称。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 流程文件名。 */
    @Size(max = 255, message = "ossId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String ossId;

    /** 用来存贮 ivr 流程启动所需要的参数描述。 */
    private String initParams;

    /** 流程发布人。 */
    @Size(max = 255, message = "createUser长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String createUser;

    /** 流程审核人。 */
    @Size(max = 255, message = "verifyUser长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String verifyUser;

    /** 流程内容(ivr)。 */
    private String content;

    /** 该流程用到的语音文件id，以英文逗号,分隔。 */
    @Size(max = 10000, message = "voiceItem长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String voiceItem;

    /** 1转接，2咨询。 */
    @Min(value = 0, message = "type不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer type;

    /** 流程状态    1：待发布   2：审核中  3：审核未通过  4：审核通过  5：已上线(ivr)。 */
    @NotNull(message = "status不能为空", groups = {AddGroup.class, EditGroup.class})
    @Min(value = 0, message = "status不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer status;
}
