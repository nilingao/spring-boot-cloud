package cn.com.nla.common.socketio.basic.netty.msg;


import cn.com.nla.common.socketio.basic.netty.msg.model.Msg100000039;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum MsgCode {
    MSG_100000039(100000039, true,Msg100000039.class),

    ;

    private static final Map<Integer, Class<? extends Message>> map = new HashMap<>();
    private static final List<Integer> inCodeCrcList = new ArrayList<>();

    static {
        for (MsgCode e : MsgCode.values()) {
            if(e.getOnCrc()){
                inCodeCrcList.add(e.getCode());
            }
            map.put(e.code, e.clazz);
        }
    }

    private final int code;

    private final boolean onCrc;
    private final Class<? extends Message> clazz;

    private MsgCode(int code,boolean onCrc, Class<? extends Message> clazz) {
        this.code = code;
        this.onCrc = onCrc;
        this.clazz = clazz;
    }
    public final int getCode() {
        return code;
    }


    public final boolean getOnCrc() {
        return onCrc;
    }

    public static Class<? extends Message> get(int code) {
        return map.get(code);
    }

    public static List<Integer> getInCodeCrcList() {
        return List.copyOf(inCodeCrcList);
    }
}
