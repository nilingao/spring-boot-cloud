package cn.com.nla.common.freeswitch.vo.result;

import lombok.Getter;
import lombok.Setter;

import java.util.EventObject;

/**
 * 未获取设备事件
 */
@Getter
@Setter
public class RestResultEvent extends EventObject {

    public int code;
    public String message;
    public String callId;
    public Object data;


    public RestResultEvent(int code, String message, Object data) {
        super("");
        this.code = code;
        this.message = message;
        this.data = data;
    }
    public RestResultEvent(int code, String message, String callId, Object data) {
        super("");
        this.code = code;
        this.callId = callId;
        this.message = message;
        this.data = data;
    }
    public RestResultEvent(int code, String message) {
        super("");
        this.code = code;
        this.message = message;
    }

}
