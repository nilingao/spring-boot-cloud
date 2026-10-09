package cn.com.nla.common.freeswitch.vo.sip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.sip.message.Message;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageTypeVo implements Serializable {
    //1.设备 2.上级平台
    private Integer type;
    //国标编号
    private String agentKey;
    //消息体
    private Message message;


    public enum TypeEnum {
        SOCKET(1, "socket方式"),
        SIP(2, "SIP方式"),
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
}
