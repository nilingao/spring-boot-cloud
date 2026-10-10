package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.GateWay;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/** 网关中继信息输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = GateWay.class, reverseConvertGenerate = false)
public class GateWayBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 网关名称。 */
    @Size(max = 30, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 关联路由id。 */
    @Size(max = 30, message = "routeId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String routeId;

    /** 服务器地址。 */
    @Size(max = 30, message = "realm长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String realm;

    /** 是否注册。 */
    @Min(value = 0, message = "register不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer register;

    /** 传输类型。 */
    @Min(value = 0, message = "transport不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer transport;

    /** 重连间隔（秒）。 */
    @Min(value = 0, message = "retrySeconds不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer retrySeconds;

    /** 账户。 */
    @Min(value = 0, message = "username不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer username;

    /** 密码。 */
    @Size(max = 256, message = "password长度超出限制", groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 是否选中0:未选中，1：已选中。 */
    @Size(max = 256, message = "selected长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String selected;
}
