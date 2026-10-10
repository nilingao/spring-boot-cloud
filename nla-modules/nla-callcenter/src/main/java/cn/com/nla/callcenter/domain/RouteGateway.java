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

/** 媒体网关表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_route_gateway")
public class RouteGateway extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 号码。 */
    @TableField(value = "`name`")
    private String name;

    /** 媒体地址。 */
    @TableField(value = "`media_host`")
    private String mediaHost;

    /** 媒体端口。 */
    @TableField(value = "`media_port`")
    private Integer mediaPort;

    /** 主叫号码前缀。 */
    @TableField(value = "`caller_prefix`")
    private String callerPrefix;

    /** 被叫号码前缀。 */
    @TableField(value = "`called_prefix`")
    private String calledPrefix;

    /** fs的context规则。 */
    @TableField(value = "`profile`")
    private String profile;

    /** sip头1。 */
    @TableField(value = "`sip_header1`")
    private String sipHeader1;

    /** sip头2。 */
    @TableField(value = "`sip_header2`")
    private String sipHeader2;

    /** sip头3。 */
    @TableField(value = "`sip_header3`")
    private String sipHeader3;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
