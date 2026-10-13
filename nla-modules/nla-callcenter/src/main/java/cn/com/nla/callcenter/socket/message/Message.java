package cn.com.nla.callcenter.socket.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

/**
 * 坐席 Socket.IO 入站消息基类。
 *
 * <p>字段名与旧工程 spring-boot-starter-socket-io 的 Message 保持一致（code/message/data），
 * 前端 SDK 按该协议回传；旧类的 Code 枚举属于短信二维码场景，此处不带。</p>
 *
 * @author TZY
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 业务数据
     */
    private Object data;
}
