package cn.com.nla.video.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;

/** 平台信息；业务服务与协议接入在阶段 6.5 实现。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_parent_platform")
public class ParentPlatform extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 是否启用。 */
    @TableField(value = "`enable`")
    private Integer enable;

    /** 名称。 */
    @TableField(value = "`name`")
    private String name;

    /** SIP服务国标编码。 */
    @TableField(value = "`server_gb_id`")
    private String serverGbId;

    /** SIP服务国标域。 */
    @TableField(value = "`server_gb_domain`")
    private String serverGbDomain;

    /** SIP服务IP。 */
    @TableField(value = "`server_ip`")
    private String serverIp;

    /** SIP服务端口。 */
    @TableField(value = "`server_port`")
    private Integer serverPort;

    /** SIP认证用户名(默认使用设备国标编号)。 */
    @TableField(value = "`username`")
    private String username;

    /** SIP认证密码。 */
    @TableField(value = "`password`")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 设备国标编号。 */
    @TableField(value = "`device_gb_id`")
    private String deviceGbId;

    /** 设备ip。 */
    @TableField(value = "`device_ip`")
    private String deviceIp;

    /** 设备端口。 */
    @TableField(value = "`device_port`")
    private Integer devicePort;

    /** 注册周期 (秒)。 */
    @TableField(value = "`expires`")
    private Integer expires;

    /** 心跳周期(秒)。 */
    @TableField(value = "`keep_timeout`")
    private Integer keepTimeout;

    /** 传输协议 1.UDP 2.TCP。 */
    @TableField(value = "`transport`")
    private Integer transport;

    /** 字符集, 1.UTF-8 2.GB2312。 */
    @TableField(value = "`character_set`")
    private Integer characterSet;

    /** 默认目录Id,自动添加的通道多放在这个目录下。 */
    @TableField(value = "`catalog_id`")
    private String catalogId;

    /** 目录分组-每次向上级发送通道信息时单个包携带的通道数量，取值1,2,4,8。 */
    @TableField(value = "`catalog_group`")
    private Integer catalogGroup;

    /** 是否允许云台控制。 */
    @TableField(value = "`ptz`")
    private Integer ptz;

    /** RTCP流保活。 */
    @TableField(value = "`rtcp`")
    private Integer rtcp;

    /** 在线状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 点播未推流的设备时是否使用redis通知拉起。 */
    @TableField(value = "`start_offline_push`")
    private Integer startOfflinePush;

    /** 行政区划。 */
    @TableField(value = "`administrative_division`")
    private String administrativeDivision;

    /** 树类型 国标规定了两种树的展现方式 216.行政区划：CivilCode 215.业务分组:BusinessGroup。 */
    @TableField(value = "`tree_type`")
    private Integer treeType;

    /** 是否作为消息通道 1.是 0.否。 */
    @TableField(value = "`as_message_channel`")
    private Integer asMessageChannel;

    /** 删除标志：0 存在、1 删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
