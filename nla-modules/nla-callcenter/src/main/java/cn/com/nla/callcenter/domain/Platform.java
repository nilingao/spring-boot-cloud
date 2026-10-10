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

/** 平台信息；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_platform")
public class Platform extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 本机IP。 */
    @TableField(value = "`local_ip`")
    private String localIp;

    /** 外网IP。 */
    @TableField(value = "`remote_ip`")
    private String remoteIp;

    /** 注册端口。 */
    @TableField(value = "`internal_port`")
    private Integer internalPort;

    /** 中继端口。 */
    @TableField(value = "`external_port`")
    private Integer externalPort;

    /** 起始RTP端口。 */
    @TableField(value = "`start_rtp_port`")
    private Integer startRtpPort;

    /** 结束RTP端口。 */
    @TableField(value = "`end_rtp_port`")
    private Integer endRtpPort;

    /** ws端口。 */
    @TableField(value = "`ws_port`")
    private Integer wsPort;

    /** wss端口。 */
    @TableField(value = "`wss_port`")
    private Integer wssPort;

    /** 音频编码。 */
    @TableField(value = "`audio_code`")
    private String audioCode;

    /** 视频编码。 */
    @TableField(value = "`video_code`")
    private String videoCode;

    /** 分辨率。 */
    @TableField(value = "`frame_rate`")
    private String frameRate;

    /** 码率。 */
    @TableField(value = "`bit_rate`")
    private String bitRate;

    /** 是否启动ice。 */
    @TableField(value = "`ice_start`")
    private Integer iceStart;

    /** stun地址。 */
    @TableField(value = "`stun_address`")
    private String stunAddress;

    /** 名称。 */
    @TableField(value = "`name`")
    private String name;

    /** 是否启用。 */
    @TableField(value = "`enable`")
    private Integer enable;

    /** 在线状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 是否开启音频。 */
    @TableField(value = "`audio_record`")
    private Integer audioRecord;

    /** 是否开启视频。 */
    @TableField(value = "`video_record`")
    private Integer videoRecord;

    /** 音频存储地址。 */
    @TableField(value = "`audio_record_path`")
    private String audioRecordPath;

    /** 视频存储地址。 */
    @TableField(value = "`video_record_path`")
    private String videoRecordPath;

    /** 声音文件地址。 */
    @TableField(value = "`sound_rile_path`")
    private String soundRilePath;

    /** 交换服务文件路径。 */
    @TableField(value = "`freeswitch_path`")
    private String freeswitchPath;

    /** 交换服务日志路径。 */
    @TableField(value = "`freeswitch_log_path`")
    private String freeswitchLogPath;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
