package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.Agent;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 座席工号表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = Agent.class, reverseConvertGenerate = false)
public class AgentBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 坐席工号。 */
    @Size(max = 255, message = "agentId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String agentId;

    /** 坐席账户。 */
    @Size(max = 255, message = "agentKey长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String agentKey;

    /** 坐席名称。 */
    @Size(max = 255, message = "agentName长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String agentName;

    /** 坐席分机号。 */
    @Size(max = 20, message = "agentCode长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String agentCode;

    /** 座席类型：1:普通座席；2：班长。 */
    @Min(value = 0, message = "agentType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer agentType;

    /** 座席密码。 */
    @Size(max = 255, message = "passwd长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwd;

    /** 是否录音 0 no 1 yes。 */
    @Min(value = 0, message = "record不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer record;

    /** 座席主要技能组  不能为空 必填项。 */
    @Min(value = 0, message = "groupId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long groupId;

    /** 话后自动空闲间隔时长。 */
    @Min(value = 0, message = "afterInterval不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer afterInterval;

    /** 振铃时长。 */
    @Min(value = 0, message = "ringTime不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer ringTime;

    /** 登录服务器地址。 */
    @Size(max = 255, message = "host长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String host;

    /** 坐席状态(1:在线,0:不在线)。 */
    @Min(value = 0, message = "state不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer state;

    /** 状态：1 开通，0关闭。 */
    @Min(value = 0, message = "status不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer status;

    /** 注册时间。 */
    private LocalDateTime registerTime;

    /** 续订时间。 */
    private LocalDateTime renewTime;

    /** 心跳时间。 */
    private LocalDateTime keepaliveTime;

    /** 心跳间隔 (最低25秒)。 */
    private Integer keepTimeout;

    /** 注册有效期（单位：秒 默认1天）。 */
    @Min(value = 0, message = "expires不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer expires;

    /** 数据流传输模式 0.UDP:udp传输 1.TCP-PASSIVE：tcp被动模式 2.TCP-ACTIVE：tcp主动模式。 */
    private Integer streamMode;

    /** 传输协议 1.UDP 2.TCP。 */
    @Min(value = 0, groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer transport;

    /** 字符集, 1.UTF-8 2.GB2312。 */
    private Integer charset;

    /** 主叫显号。 */
    @Size(max = 255, message = "display长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String display;
}
