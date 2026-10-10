package cn.com.nla.callcenter.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;

/** 字冠路由表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_route_call")
public class RouteCall extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属企业。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 所属网关组。 */
    @TableField(value = "`route_group_id`")
    private Long routeGroupId;

    /** 字冠号码。 */
    @TableField(value = "`route_num`")
    private String routeNum;

    /** 最长。 */
    @TableField(value = "`num_max`")
    private Integer numMax;

    /** 最短。 */
    @TableField(value = "`num_min`")
    private Integer numMin;

    /** 主叫替换规则。 */
    @TableField(value = "`caller_change`")
    private Integer callerChange;

    /** 替换号码。 */
    @TableField(value = "`caller_change_num`")
    @ToString.Exclude
    private String callerChangeNum;

    /** 被叫替换规则。 */
    @TableField(value = "`called_change`")
    private Integer calledChange;

    /** 替换号码。 */
    @TableField(value = "`called_change_num`")
    @ToString.Exclude
    private String calledChangeNum;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
