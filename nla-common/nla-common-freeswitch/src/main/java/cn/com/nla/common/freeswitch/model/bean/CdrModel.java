package cn.com.nla.common.freeswitch.model.bean;

import cn.com.nla.common.freeswitch.common.fs.Constant;
import cn.com.nla.common.freeswitch.model.BeanModel;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 通话记录
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CdrModel extends BeanModel {
    /**
     * IP地址
     */
    private String localIpV4;
    /**
     *
     */
    private String callerIdName;
    /**
     * 主叫号码
     */
    private String callerIdNumber;
    /**
     * 被叫号码
     */
    private String destinationNumber;
    /**
     * 呼叫内容
     */
    private String context;
    /**
     * 呼叫时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime startStamp;
    /**
     * 应答时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime answerStamp;
    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime endStamp;
    /**
     * 呼叫时长
     */
    private Integer duration;
    /**
     * billsec
     */
    private Integer billsec;
    /**
     * 挂断原因
     */
    private String hangupCause;
    /**
     * uuid
     */
    private String uuid;
    /**
     * blegUUID
     */
    private String blegUuid;
    /**
     * 账户编码
     */
    private String accountcode;
    /**
     * 读取编码
     */
    private String readCodec;
    /**
     * 写入编码
     */
    private String writeCodec;
    /**
     * sipHangupDisposition
     */
    private String sipHangupDisposition;
    /**
     * ani
     */
    private String ani;

}
