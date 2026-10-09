package cn.com.nla.common.freeswitch.enums.sip;

import java.util.HashMap;
import java.util.Map;

public enum CharsetType {

    UTF_8(1, "UTF-8"),
    GB2312(2, "GB2312"),
            ;

    private final int value;
    private final String name;

    CharsetType(int value, String name) {
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
        for (CharsetType s : CharsetType.values()) {
            map.put(s.getValue(), s.getName());
        }
    }
}
