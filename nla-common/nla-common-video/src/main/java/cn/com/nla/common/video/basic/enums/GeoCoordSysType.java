package cn.com.nla.common.video.basic.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 地理坐标系
 */
public enum GeoCoordSysType {

    WGS84(1, "WGS84"),
    GCJ02(2, "GCJ02"),
    ;

    private final int value;
    private final String name;

    GeoCoordSysType(int value, String name) {
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
        for (GeoCoordSysType s : GeoCoordSysType.values()) {
            map.put(s.getValue(), s.getName());
        }
    }

}
