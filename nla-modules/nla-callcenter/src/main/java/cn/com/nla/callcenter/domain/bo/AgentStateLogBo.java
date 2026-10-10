package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.AgentStateLog;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 坐席状态历史表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = AgentStateLog.class, reverseConvertGenerate = false)
public class AgentStateLogBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业id。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 主技能组id。 */
    @Min(value = 0, message = "groupId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long groupId;

    /** 坐席id。 */
    @Min(value = 0, message = "agentId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long agentId;

    /** 坐席编号。 */
    @Size(max = 255, message = "agentKey长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String agentKey;

    /** 坐席名称。 */
    @Size(max = 255, message = "agentName长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String agentName;

    /** 通话唯一标识。 */
    @Min(value = 0, message = "callId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long callId;

    /** 登录类型。 */
    @Min(value = 0, message = "loginType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer loginType;

    /** 工作类型。 */
    @Min(value = 0, message = "workType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer workType;

    /** 服务站点。 */
    @Size(max = 255, message = "host长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String host;

    /** 远端地址。 */
    @Size(max = 255, message = "remoteAddress长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String remoteAddress;

    /** 变更之前状态。 */
    @Size(max = 50, message = "beforeState长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String beforeState;

    /** 更变之前时间。 */
    @Min(value = 0, message = "beforeTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long beforeTime;

    /** 变更之后状态。 */
    @Size(max = 50, message = "state长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String state;

    /** 当前时间(秒)。 */
    @Min(value = 0, message = "stateTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long stateTime;

    /** 持续时间(秒)。 */
    @Min(value = 0, message = "duration不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer duration;

    /** 忙碌类型。 */
    @Size(max = 255, message = "busyDesc长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String busyDesc;

    /** 状态。 */
    private Integer status;
}
