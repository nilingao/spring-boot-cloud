package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.RouteGateway;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 媒体网关表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = RouteGateway.class, reverseConvertGenerate = false)
public class RouteGatewayBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 号码。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 媒体地址。 */
    @Size(max = 255, message = "mediaHost长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String mediaHost;

    /** 媒体端口。 */
    @Min(value = 0, message = "mediaPort不能为负数", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 65535, message = "端口不能超过65535", groups = {AddGroup.class, EditGroup.class})
    private Integer mediaPort;

    /** 主叫号码前缀。 */
    @Size(max = 255, message = "callerPrefix长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String callerPrefix;

    /** 被叫号码前缀。 */
    @Size(max = 255, message = "calledPrefix长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String calledPrefix;

    /** fs的context规则。 */
    @Size(max = 255, message = "profile长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String profile;

    /** sip头1。 */
    @Size(max = 255, message = "sipHeader1长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String sipHeader1;

    /** sip头2。 */
    @Size(max = 255, message = "sipHeader2长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String sipHeader2;

    /** sip头3。 */
    @Size(max = 255, message = "sipHeader3长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String sipHeader3;

    /** 状态。 */
    private Integer status;
}
