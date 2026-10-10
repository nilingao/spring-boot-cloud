package cn.com.nla.video.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.video.domain.ParentPlatform;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

/** 平台信息输入，不接受审计与删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = ParentPlatform.class, reverseConvertGenerate = false)
public class ParentPlatformBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 是否启用。 */
    private Integer enable;

    /** 名称。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** SIP服务国标编码。 */
    @NotBlank(message = "serverGbId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "serverGbId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String serverGbId;

    /** SIP服务国标域。 */
    @Size(max = 50, message = "serverGbDomain长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String serverGbDomain;

    /** SIP服务IP。 */
    @Size(max = 50, message = "serverIp长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String serverIp;

    /** SIP服务端口。 */
    @Min(value = 0, message = "serverPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer serverPort;

    /** SIP认证用户名(默认使用设备国标编号)。 */
    @Size(max = 255, message = "username长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String username;

    /** SIP认证密码。 */
    @Size(max = 50, message = "password长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 设备国标编号。 */
    @NotBlank(message = "deviceGbId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "deviceGbId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String deviceGbId;

    /** 设备ip。 */
    @Size(max = 50, message = "deviceIp长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String deviceIp;

    /** 设备端口。 */
    @Min(value = 0, message = "devicePort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer devicePort;

    /** 注册周期 (秒)。 */
    @Min(value = 0, message = "expires不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer expires;

    /** 心跳周期(秒)。 */
    @Min(value = 0, message = "keepTimeout不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer keepTimeout;

    /** 传输协议 1.UDP 2.TCP。 */
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer transport;

    /** 字符集, 1.UTF-8 2.GB2312。 */
    private Integer characterSet;

    /** 默认目录Id,自动添加的通道多放在这个目录下。 */
    @NotBlank(message = "catalogId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "catalogId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String catalogId;

    /** 目录分组-每次向上级发送通道信息时单个包携带的通道数量，取值1,2,4,8。 */
    @Min(value = 0, message = "catalogGroup不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer catalogGroup;

    /** 是否允许云台控制。 */
    private Integer ptz;

    /** RTCP流保活。 */
    private Integer rtcp;

    /** 在线状态。 */
    private Integer status;

    /** 点播未推流的设备时是否使用redis通知拉起。 */
    private Integer startOfflinePush;

    /** 行政区划。 */
    @Size(max = 50, message = "administrativeDivision长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String administrativeDivision;

    /** 树类型 国标规定了两种树的展现方式 216.行政区划：CivilCode 215.业务分组:BusinessGroup。 */
    @Min(value = 0, message = "treeType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer treeType;

    /** 是否作为消息通道 1.是 0.否。 */
    private Integer asMessageChannel;
}
