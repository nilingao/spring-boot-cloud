package cn.com.nla.common.freeswitch.vo.fs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备订阅通知类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentNotifyVo implements Serializable {
    /**
     * 消息类型 1.目录订阅 2.移动位置订阅
     */
    private Integer  type;

    /**
     * 操作类型 1.添加 2.删除
     */
    private Integer  operate;
    /**
     * 设备国标编号
     */
    private String agentKey;



    public enum TypeEnum {
        PRESENCE(1, "presence"),
        MESSAGE_SUMMARY(2, "message-summary"),
        ;

        private final int value;
        private final String name;

        TypeEnum(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
        private static Map<Integer, String> map = new HashMap<Integer, String>();
        static {
            for (TypeEnum s : TypeEnum.values()) {
                map.put(s.getValue(), s.getName());
            }
        }
    }

    public enum OperateEnum {
        ADD(1, "添加订阅"),
        DEL(2, "删除订阅"),
        ;

        private final int value;
        private final String name;

        OperateEnum(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
        private static Map<Integer, String> map = new HashMap<Integer, String>();
        static {
            for (OperateEnum s : OperateEnum.values()) {
                map.put(s.getValue(), s.getName());
            }
        }
    }

}
