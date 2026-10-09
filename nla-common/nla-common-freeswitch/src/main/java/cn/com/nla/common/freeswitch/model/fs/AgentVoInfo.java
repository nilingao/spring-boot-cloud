package cn.com.nla.common.freeswitch.model.fs;

import cn.com.nla.common.freeswitch.enums.ConstEnum;
import cn.com.nla.common.freeswitch.common.fs.Constant;
import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.enums.fs.LoginTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class AgentVoInfo implements Serializable {

    /**
     * 主键
     */
    private Long id;
    /**
     * 企业ID
     */
    private String companyId;
    /**
     * 坐席工号
     */
    private String agentId;
    /**
     * 坐席账户
     */
    private String agentKey;
    /**
     * 坐席名称
     */
    private String agentName;
    /**
     * 坐席分机号
     */
    private String agentCode;
    /**
     * 座席类型：1:普通座席；2：班长
     */
    private Integer agentType;
    /**
     * 座席密码
     */
    private String passwd;
    /**
     * 是否录音 0 no 1 yes
     */
    private Integer record;
    /**
     * 座席主要技能组  不能为空 必填项
     */
    private String groupId;
    /**
     * 话后自动空闲间隔时长
     */
    private Integer afterInterval;
    /**
     * 振铃时长
     */
    private Integer ringTime;
    /**
     * 登录服务地址
     */
    private String host;
    /**
     * 坐席状态(1:在线,0:不在线)
     */
    private Integer state;
    /**
     * 状态：1 启用，0关闭
     */
    private Integer status;
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
     * 心跳周期(秒)
     */
    private Integer keepTimeout;
    /**
     * 注册时长
     */
    @Builder.Default
    private int expires = 3600;
    /**
     * 传输协议 1.UDP 2.TCP
     */
    private Integer transport;
    /**
     * 数据流传输模式 0.UDP:udp传输 2.TCP-ACTIVE：tcp主动模式 2.TCP-PASSIVE：tcp被动模式
     */
    private Integer streamMode;
    /**
     * 字符集, 1.UTF-8 2.GB2312
     */
    @Builder.Default
    private Integer charset= 2;
    //-----------------以下为缓存数据---------------------------

    private String sipPhone;
    /**
     * 绑定的电话号码
     */
    private List<String> sipPhoneList;
    /**
     * 流媒体编号
     */
    private String mediaServerId;
    /**
     * 流媒体地址
     */
    private String mediaAddress;
    /**
     * 1：socket登陆
     * 2：sip登陆
     */
    @Builder.Default
    private Integer loginType = LoginTypeEnum.SOCKET.getType();
    /**
     * 总机坐席
     */
    private Integer agentOnline;
    /**
     * 客户端地址
     */
    private String fsHost;
    /**
     * 客户端地址
     */
    private String fsPost;
    /**
     * 客户端地址
     */
    private String remoteAddress;

    /**
     * 所属技能组
     */
    private List<String> groupIds;
    /**
     * 坐席sip
     */
    private List<String> sips;
    /**
     * 坐席技能
     */
    private List<SkillAgentInfo> skillAgents;
    /**
     * 通话id
     */
    private String callId;
    /**
     * 设备id
     */
    private String deviceId;
    /**
     *  被咨询设备
     */
    private String consultDeviceId;

    /**
     * 是否开启ssrc效验
     */
    private Integer ssrcCheck = ConstEnum.Flag.NO.getValue();
    /**
     * 登录时间(秒)
     */
    @Builder.Default
    private Long loginTime = 0L;
    /**
     * 1:普通
     * 2:预测
     */
    private Integer workType;
    /**
     * 坐席最近的一次服务时间,电话则是振铃时间(秒)
     */
    @Builder.Default
    private Long serviceTime = 0L;
    /**
     * 当前状态
     */
    private AgentStateEnum agentState;
    /**
     * 当天状态时间(秒)
     */
    @Builder.Default
    private Long stateTime = 0L;
    /**
     * 上一次状态
     */
    private AgentStateEnum beforeState;
    /**
     * 上一次状态时间(秒)
     */
    @Builder.Default
    private Long beforeTime = 0L;
    /**
     * 最大空闲时长
     */
    @Builder.Default
    private Long maxReadyTime = 0L;
    /**
     * 累计空闲
     */
    @Builder.Default
    private Long totalReadyTime = 0L;
    /**
     * 空闲次数
     */
    @Builder.Default
    private Long readyTimes = 0L;
    /**
     * 忙碌次数
     */
    @Builder.Default
    private Long notReadyTimes = 0L;
    /**
     * 累计话后时间长
     */
    @Builder.Default
    private Long totalAfterTime = 0L;
    /**
     * 最大通话时长
     */
    @Builder.Default
    private Long maxTalkTime = 0L;
    /**
     * 当日累计通话时长
     */
    @Builder.Default
    private Long totalTalkTime = 0L;
    /**
     * 振铃次数
     */
    @Builder.Default
    private Long totalRingTimes = 0L;
    /**
     * 当日累计接听次数
     */
    @Builder.Default
    private Long totalAnswerTimes = 0L;
    /**
     * 隐藏客户号码(0:不隐藏;1:隐藏)
     */
    @Builder.Default
    private Integer hiddenCustomer= 0;

    /**
     * 获取坐席被叫号码
     *
     * @return
     */
    public String getCalled() {
        if (loginType == null) {
            return null;
        }
        if(sipPhone != null){
            return sipPhone;
        }
        if(sipPhoneList==null || sipPhoneList.isEmpty()){
            return null;
        }
        sipPhone = sipPhoneList.get(0);
        return sipPhone;
    }
    public Integer getState() {
        if(agentState == null || agentState == AgentStateEnum.LOGOUT){
            return ConstEnum.Flag.NO.getValue();
        }else{
            return ConstEnum.Flag.YES.getValue();
        }
    }
    public boolean expire(){
        if(renewTime == null){
            return true;
        }
        LocalDateTime endTime = renewTime.plusSeconds(expires + SipConstant.DELAY_TIME);
        return endTime.isBefore(LocalDateTime.now());
    }

    public boolean keepalive(){
        if(keepaliveTime == null || keepTimeout == null){
            return true;
        }
        LocalDateTime endTime = keepaliveTime.plusSeconds(keepTimeout + SipConstant.DELAY_TIME);
        return endTime.isBefore(LocalDateTime.now());
    }
}
