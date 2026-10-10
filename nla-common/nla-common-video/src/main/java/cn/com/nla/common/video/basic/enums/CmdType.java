package cn.com.nla.common.video.basic.enums;

import java.util.HashMap;
import java.util.Map;

public enum CmdType {
    DEVICE_CONTROL_NOTIFY("DeviceControl", "设备控制"),

    ALARM_NOTIFY("Alarm", "设备控制"),
    KEEPALIVE_NOTIFY("Keepalive", "心跳报送"),
    MEDIA_STATUS_NOTIFY("MediaStatus", "媒体通知"),
    MOBILE_POSITION_NOTIFY("MobilePosition", "移动设备位置通知"),
    ALARM_QUERY("Alarm", "报警查询"),
    CATALOG_QUERY("Catalog", "目录查询"),
    DEVICE_INFO_QUERY("DeviceInfo", "设备信息查询"),
    DEVICE_STATUS_QUERY("DeviceStatus", "设备状态查询"),
    RECORD_INFO_QUERY("RecordInfo", "录像查询"),
    ALARM_RESPONSE("Alarm", "报警回复"),
    BROADCAST_RESPONSE("Broadcast", "广播回复"),
    CATALOG_RESPONSE("Catalog", "目录查询回复"),
    CONFIG_DOWNLOAD_RESPONSE("ConfigDownload", "设备配置查询的回复"),
    DEVICE_CONFIG_RESPONSE("DeviceConfig", "设备配置的回复"),
    DEVICE_CONTROL_RESPONSE("DeviceControl", "设备控制的回复"),
    DEVICE_INFO_RESPONSE("DeviceInfo", "设备信息的回复"),
    DEVICE_STATUS_RESPONSE("DeviceStatus", "设备状态的回复"),
    MOBILE_POSITION_RESPONSE("MobilePosition", "移动设备位置的回复"),
    PRESET_QUERY_RESPONSE("PresetQuery", "设备预置位查询应答"),
    RECORD_INFO_RESPONSE("RecordInfo", "录像查询应答"),
    ;

    private final String value;
    private final String name;

    CmdType(String value, String name) {
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
        for (CmdType s : CmdType.values()) {
            map.put(s.getValue(), s.getName());
        }
    }
}
