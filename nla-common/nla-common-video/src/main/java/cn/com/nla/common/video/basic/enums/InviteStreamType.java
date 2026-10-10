package cn.com.nla.common.video.basic.enums;

import java.util.HashMap;
import java.util.Map;

public enum InviteStreamType {

    PLAY("PLAY","直播"),
    PLAYBACK("PLAYBACK","回放"),
    DOWNLOAD("DOWNLOAD","下载"),
    PUSH("PUSH","推流"),
    PROXY("PROXY","拉流"),
    CLOUD_RECORD_PUSH("CLOUD_RECORD_PUSH","云记录推流"),
    CLOUD_RECORD_PROXY("CLOUD_RECORD_PROXY","云记录拉流"),
    ;

    private final String value;
    private final String name;

    InviteStreamType(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static InviteStreamType getInviteStreamType(String value) {
        return map.get(value);
    }
    private static Map<String , InviteStreamType> map = new HashMap<String , InviteStreamType>();
    static {
        for (InviteStreamType s : InviteStreamType.values()) {
            map.put(s.getValue(), s);
        }
    }

}