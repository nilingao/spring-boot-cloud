package cn.com.nla.common.socketio.basic.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Serializable {

    //状态码
    private Integer code;

    private String message;

    private Object data;

    /**
     * 扫描类型
     */
    public enum Code{
        SUCCESS(0,"操作完成"),
        OVERDUE(1,"二维码已过期"),

        ERROR(2,"操作错误"),
        ;

        private final int value;
        private final String name;

        Code(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public static Map<Integer, String> map = new HashMap<Integer, String>();
        static {
            for (Code e : Code.values()) {
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
}
