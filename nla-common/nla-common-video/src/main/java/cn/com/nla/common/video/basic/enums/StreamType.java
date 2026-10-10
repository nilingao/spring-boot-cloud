package cn.com.nla.common.video.basic.enums;

import java.util.HashMap;
import java.util.Map;

public enum StreamType {

    PROXY(1, "拉流"),
    PULL(2, "推流"),
    ;

    private final int value;
    private final String name;

    StreamType(int value, String name) {
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
        for (StreamType s : StreamType.values()) {
            map.put(s.getValue(), s.getName());
        }
    }
}
