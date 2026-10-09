package cn.com.nla.common.freeswitch.vo.sip;

import cn.com.nla.common.freeswitch.vo.BaseVo;
import cn.com.nla.common.freeswitch.common.fs.Constant;
import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 允许注册的设备信息
 */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DeviceVo extends BaseVo {
    /**
     * 设备国标编号
     */
    private String deviceId;

    /**
     * 设备名
     */
    private String name;

    /**
     * 传输协议 1.UDP 2.TCP
     */
    private Integer transport;

    /**
     * wan地址_ip
     */
    private String ip;

    /**
     * wan地址_port
     */
    private Integer port;

    /**
     * wan地址
     */
    private String hostAddress;

    /**
     * 设备密码
     */
    private String password;

    /**
     * 收流IP
     */
    private String sdpIp;

    /**
     * SIP交互IP（设备访问平台的IP）
     */
    private String localIp;

    /**
     * 字符集, 1.UTF-8 2.GB2312
     */
    private Integer charset= 2;

    /**
     * 是否在线，1.为在线，0.为离线
     */
    private Integer online= 0;

    /**
     * 注册时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime registerTime;
    /**
     * 续订时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime renewTime;
    /**
     * 心跳时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime keepaliveTime;

    /**
     * 心跳间隔
     */
    private Integer keepaliveIntervalTime= 30;

    /**
     * 数据流传输模式 0.UDP:udp传输 1.TCP-PASSIVE：tcp被动模式 2.TCP-ACTIVE：tcp主动模式
     */
    private Integer streamMode = 0;

    /**
     * 注册有效期
     */
    private Integer expires;

    /**
     * 是否开启ssrc校验，默认关闭，开启可以防止串流
     */
    private Integer ssrcCheck = 0;


    public boolean expire(){
        if(renewTime == null || expires == null){
            return true;
        }
        LocalDateTime endTime = renewTime.plusSeconds(expires + SipConstant.DELAY_TIME);
        return endTime.isBefore(LocalDateTime.now());
    }

    public boolean keepalive(){
        if(keepaliveTime == null || keepaliveIntervalTime == null){
            return true;
        }
        LocalDateTime endTime = keepaliveTime.plusSeconds(keepaliveIntervalTime + SipConstant.DELAY_TIME);
        return endTime.isBefore(LocalDateTime.now());
    }

}