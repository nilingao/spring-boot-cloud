package cn.com.nla.common.freeswitch.enums.sip;

import java.util.HashMap;
import java.util.Map;

public enum TransportType {

    UDP(1, "UDP"),
    TCP(2, "TCP"),
    ;

    private final int value;
    private final String name;

    TransportType(int value, String name) {
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
        for (TransportType s : TransportType.values()) {
            map.put(s.getValue(), s.getName());
        }
    }

}
