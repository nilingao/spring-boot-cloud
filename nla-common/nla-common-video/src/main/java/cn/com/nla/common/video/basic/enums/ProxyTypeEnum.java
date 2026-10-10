package cn.com.nla.common.video.basic.enums;

import java.util.HashMap;
import java.util.Map;

public enum ProxyTypeEnum{
        DEFAULT(1, "默认拉流"),
        FFMPEG(2, "ffmpeg拉流"),
        ;
        private final int value;
        private final String name;

        private ProxyTypeEnum(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public static Map<Integer, String> map = new HashMap<Integer, String>();
        static {
            for (ProxyTypeEnum e : ProxyTypeEnum.values()) {
                map.put(e.getValue(), e.getName());
            }
        }

        public static String getName(int value) {
            return map.get(value);
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
    }