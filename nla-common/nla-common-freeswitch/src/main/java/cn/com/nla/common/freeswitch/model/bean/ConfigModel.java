package cn.com.nla.common.freeswitch.model.bean;

import cn.com.nla.common.freeswitch.model.BeanModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 服务配置信息
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ConfigModel extends BeanModel {
    /** 本机IP */
    private String localIp;

    /** 外网IP */
    private String remoteIp;

    /** 中继端口 */
    private String externalPort;

    /** 注册端口 */
    private String internalPort;

    /** 起始RTP端口 */
    private String startRtpPort;

    /** 结束RTP端口 */
    private String endRtpPort;

    private String wsPort;

    private String wssPort;

    /** 音频编码 */
    private String audioCode;

    /** 视频编码 */
    private String videoCode;

    /** 分辨率 */
    private String frameRate;

    /** 码率 */
    private Long bitRate;

    /** 是否启动ice */
    private Integer iceStart;

    /** stun地址 */
    private String stunAddress;

    /** 是否音频 */
    private Integer audioRecord;

    /** 是否视频 */
    private Integer videoRecord;

    /** 音频存储地址 */
    private String audioRecordPath;

    /** 视频存储地址 */
    private String videoRecordPath;

    /** 声音文件地址 */
    private String soundFilePath;

    /** 交换服务文件路径 */
    private String freeswitchPath;

    /** 交换服务日志路径 */
    private String freeswitchLogPath;


}
