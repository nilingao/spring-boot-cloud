package cn.com.nla.common.freeswitch.enums.fs;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum LoginTypeEnum {

    SOCKET(1,"socket方式登陆"),
    SIP(2,"sip方式登陆"),
    ;
    private final int type;//登录方式
    private final String name;//登陆方式名称

    LoginTypeEnum(int type, String name) {
        this.type = type;
        this.name = name;
    }
    private static Map<Integer, LoginTypeEnum> map = new HashMap<>();
    static {
        for (LoginTypeEnum e : LoginTypeEnum.values()) {
            map.put(e.getType(), e);
        }
    }
    public static LoginTypeEnum getLoginType(Integer type) {
        return map.get(type);
    }

}
