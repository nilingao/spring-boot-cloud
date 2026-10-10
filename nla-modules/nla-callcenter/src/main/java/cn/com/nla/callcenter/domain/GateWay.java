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

/** 网关中继信息；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_gate_way")
public class GateWay extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 网关名称。 */
    @TableField(value = "`name`")
    private String name;

    /** 关联路由id。 */
    @TableField(value = "`route_id`")
    private String routeId;

    /** 服务器地址。 */
    @TableField(value = "`realm`")
    private String realm;

    /** 是否注册。 */
    @TableField(value = "`register`")
    private Integer register;

    /** 传输类型。 */
    @TableField(value = "`transport`")
    private Integer transport;

    /** 重连间隔（秒）。 */
    @TableField(value = "`retry_seconds`")
    private Integer retrySeconds;

    /** 账户。 */
    @TableField(value = "`username`")
    private Integer username;

    /** 密码。 */
    @TableField(value = "`password`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 是否选中0:未选中，1：已选中。 */
    @TableField(value = "`selected`")
    private String selected;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
