package cn.com.nla.common.video.basic.enums;


import java.util.HashMap;
import java.util.Map;

public enum StreamModeType {

    UDP(0, "UDP"),
    TCP_PASSIVE(1, "TCP-PASSIVE"),//tcp被动模式
    TCP_ACTIVE(2, "TCP-ACTIVE"),//tcp主动模式
    ;

    private final int value;
    private final String name;

    StreamModeType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static String getName(Integer value) {
        return map.get(value);
    }
    private static Map<Integer , String> map = new HashMap<Integer , String>();
    static {
        for (StreamModeType s : StreamModeType.values()) {
            map.put(s.getValue(), s.getName());
        }
    }
}
