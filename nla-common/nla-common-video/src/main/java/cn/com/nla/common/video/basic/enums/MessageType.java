package cn.com.nla.common.video.basic.enums;

import java.util.HashMap;
import java.util.Map;

public enum MessageType {
    CONTROL("Control", "控制消息"),
    NOTIFY("Notify", "通知消息"),
    QUERY("Query", "查询消息"),
    RESPONSE("Response", "应答消息"),
    ;

    private final String value;
    private final String name;

    MessageType(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static String getName(String value) {
        return map.get(value);
    }
    private static Map<String , String> map = new HashMap<String , String>();
    static {
        for (MessageType s : MessageType.values()) {
            map.put(s.getValue(), s.getName());
        }
    }

}
