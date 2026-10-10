package cn.com.nla.common.video.basic.enums;

import java.util.HashMap;
import java.util.Map;

public enum PtzTypeEnum {

    UNKNOWN(0, "未知"),
    PTZ(1, "球机"),
    HEMISPHERE(2, "半球"),
    FIXED(3, "固定枪机"),
    REMOTE_CONTROL(4, "遥控枪机"),
    ;

    private final int value;
    private final String name;

    PtzTypeEnum(int value, String name) {
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
        for (PtzTypeEnum s : PtzTypeEnum.values()) {
            map.put(s.getValue(), s.getName());
        }
    }
}
