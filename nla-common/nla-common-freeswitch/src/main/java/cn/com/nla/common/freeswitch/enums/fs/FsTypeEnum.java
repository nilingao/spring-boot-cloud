package cn.com.nla.common.freeswitch.enums.fs;

import java.util.HashMap;
import java.util.Map;

public enum FsTypeEnum {
    //通话记录设置
    NOT_FIND(0, "fs/not_find.xml", "notFind"),
    //对外网关中继设置
    EXTERNAL(1, "fs/sip_profiles/external.xml", "gateway"),
    //网关中继IPV6设置
    EXTERNAL_IPV6(2, "fs/sip_profiles/external-ipv6.xml", "externalIpv6"),
    //对内网关
    INTERNAL(3, "fs/sip_profiles/internal.xml", "internal"),
    //对内网关IPV6设置
    INTERNAL_IPV6(4, "fs/sip_profiles/internal-ipv6.xml", "internalIpv6"),
    //主要设置控制台快捷键、数据库处理句柄、session会话、日志等级等
    POST_LOAD_SWITCH(5, "fs/autoload_configs/post_load_switch.conf.xml", "config"),
    //用户目录配置
    USER(6, "fs/directory/user.xml", "user"),
    //拨号计划
    DIALPLAN(7, "fs/dialplan/dialplan.xml", "dialplan"),
    //会议
    CONFERENCE(8, "fs/autoload_configs/conference.conf.xml", "conference"),
    ;
    private final int value;
    private final String path;
    private final String name;

    FsTypeEnum(int value,String path, String name) {
        this.value = value;
        this.path = path;
        this.name = name;
    }
    public int getValue() {
        return value;
    }
    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }
    public static FsTypeEnum getFsTypeEnum(int value){
        return MAP.get(value);
    }

    private final static Map<Integer, FsTypeEnum> MAP = new HashMap<>();
    static {
        for (FsTypeEnum s : FsTypeEnum.values()) {
            MAP.put(s.getValue(), s);
        }
    }
}
