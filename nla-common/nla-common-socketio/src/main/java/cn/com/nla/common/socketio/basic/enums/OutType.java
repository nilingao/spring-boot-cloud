package cn.com.nla.common.socketio.basic.enums;

import java.util.HashMap;
import java.util.Map;

public enum OutType{
        MESSAGE(1,"字符串"),
        IMG(2,"图片"),
        VIDEO(3,"视频"),
        ;
        private final Integer value;
        private final String name;

        OutType(Integer value, String name) {
            this.value = value;
            this.name = name;
        }

        public Integer getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        private static Map<Integer, String> map = new HashMap<Integer, String>();
        static {
            for (OutType s : OutType.values()) {
                map.put(s.getValue(), s.getName());
            }
        }
    }
