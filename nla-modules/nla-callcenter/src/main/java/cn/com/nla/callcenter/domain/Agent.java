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
import java.time.LocalDateTime;

/** 座席工号表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_agent")
public class Agent extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业ID。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 坐席工号。 */
    @TableField(value = "`agent_id`")
    private String agentId;

    /** 坐席账户。 */
    @TableField(value = "`agent_key`")
    private String agentKey;

    /** 坐席名称。 */
    @TableField(value = "`agent_name`")
    private String agentName;

    /** 坐席分机号。 */
    @TableField(value = "`agent_code`")
    private String agentCode;

    /** 座席类型：1:普通座席；2：班长。 */
    @TableField(value = "`agent_type`")
    private Integer agentType;

    /** 座席密码。 */
    @TableField(value = "`passwd`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwd;

    /** 是否录音 0 no 1 yes。 */
    @TableField(value = "`record`")
    private Integer record;

    /** 座席主要技能组  不能为空 必填项。 */
    @TableField(value = "`group_id`")
    private Long groupId;

    /** 话后自动空闲间隔时长。 */
    @TableField(value = "`after_interval`")
    private Integer afterInterval;

    /** 振铃时长。 */
    @TableField(value = "`ring_time`")
    private Integer ringTime;

    /** 登录服务器地址。 */
    @TableField(value = "`host`")
    private String host;

    /** 坐席状态(1:在线,0:不在线)。 */
    @TableField(value = "`state`")
    private Integer state;

    /** 状态：1 开通，0关闭。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 注册时间。 */
    @TableField(value = "`register_time`")
    private LocalDateTime registerTime;

    /** 续订时间。 */
    @TableField(value = "`renew_time`")
    private LocalDateTime renewTime;

    /** 心跳时间。 */
    @TableField(value = "`keepalive_time`")
    private LocalDateTime keepaliveTime;

    /** 心跳间隔 (最低25秒)。 */
    @TableField(value = "`keep_timeout`")
    private Integer keepTimeout;

    /** 注册有效期（单位：秒 默认1天）。 */
    @TableField(value = "`expires`")
    private Integer expires;

    /** 数据流传输模式 0.UDP:udp传输 1.TCP-PASSIVE：tcp被动模式 2.TCP-ACTIVE：tcp主动模式。 */
    @TableField(value = "`stream_mode`")
    private Integer streamMode;

    /** 传输协议 1.UDP 2.TCP。 */
    @TableField(value = "`transport`")
    private Integer transport;

    /** 字符集, 1.UTF-8 2.GB2312。 */
    @TableField(value = "`charset`")
    private Integer charset;

    /** 主叫显号。 */
    @TableField(value = "`display`")
    @ToString.Exclude
    private String display;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
