package cn.com.nla.common.video.core.demo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.EventObject;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTypeObject implements Serializable {
    /**
     * 消息类型
     * REQUEST 请求类型
     * RESPONSE 相应类型
     * TIMEOUT 超时类型
     */
    private String type;
    /**
     * 消息信息
     */
    private EventObject eventObject;
}
