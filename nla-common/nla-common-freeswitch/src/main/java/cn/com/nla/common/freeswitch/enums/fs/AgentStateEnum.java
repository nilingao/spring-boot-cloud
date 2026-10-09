package cn.com.nla.common.freeswitch.enums.fs;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 坐席状态
 */
@Getter
public enum AgentStateEnum {
    LOGOUT(1,"LOGOUT","退出"),
    LOGIN(2,"LOGIN","登录"),
    RECONNECT(3,"RECONNECT","重连"),
    READY(4,"READY","空闲"),
    NOT_READY(5,"NOT_READY","忙碌"),
    BUSY_OTHER(6,"BUSY_OTHER","繁忙"),
    AFTER(7,"AFTER","话后"),
    SIP_ERROR(8,"SIP_ERROR","sip错误"),
    OUT_CALL(9,"OUT_CALL","请求外呼"),
    OUT_CALLER_RING(10,"OUT_CALLER_RING","外呼振铃中"),
    OUT_CALLED_RING(11,"OUT_CALLED_RING","外呼被叫振铃"),
    INNER_CALL(12,"INNER_CALL","内呼"),
    IN_CALL_RING(13,"IN_CALL_RING","呼入来电振铃"),
    CONSULT(14,"CONSULT","咨询"),
    CONSULT_CALL_RING(15,"CONSULT_CALL_RING","咨询振铃"),
    CONSULTED_TALKING(16,"CONSULTED_TALKING","被咨询通话"),
    CONSULT_TALKING(17,"CONSULT_TALKING","咨询通话"),
    CONFERENCE_TALKING(18,"CONFERENCE_TALKING","会议中"),
    TRANSFER(19,"TRANSFER","转接"),
    TRANSFER_CALL_RING(20,"TRANSFER_CALL_RING","转接振铃"),
    TRANSFER_CALL(21,"TRANSFER_CALL","转接来电"),
    TALKING(22,"TALKING","通话中"),
    AUDIO_READY_MUTE(23,"AUDIO_READY_MUTE","静音中"),
    MONITOR(24,"MONITOR","班长监控中"),
    INSERT(25,"INSERT","强插中"),
    LISTEN(26,"LISTEN","监听中"),
    WHISPER(27,"WHISPER","耳语"),
    HOLD(28,"HOLD","保持中");

    private final int value;
    private final String type;
    private final String name;

    AgentStateEnum(int value,String type, String name) {
        this.value = value;
        this.type = type;
        this.name = name;
    }

    private static final Map<Integer, String> MAP = new HashMap<>();
    private static final Map<Integer, AgentStateEnum> MAP_TYPE = new HashMap<>();
    static {
        for (AgentStateEnum e : AgentStateEnum.values()) {
            MAP.put(e.getValue(), e.getName());
            MAP_TYPE.put(e.getValue(),e);
        }
    }

    public static String getName(int value) {
        return MAP.get(value);
    }

    public static AgentStateEnum getAgentState(int value) {
        return MAP_TYPE.get(value);
    }
}
