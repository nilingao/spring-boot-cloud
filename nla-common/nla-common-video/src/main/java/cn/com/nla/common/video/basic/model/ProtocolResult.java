package cn.com.nla.common.video.basic.model;

import cn.com.nla.common.video.basic.enums.RespCode;
import lombok.*;
import java.io.Serializable;

/** SIP callback result; code zero means success. Web controllers adapt this to R.
 * @author TZY
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolResult<T> implements Serializable {
    private int code;
    private String message;
    private T data;

    public boolean ok() { return code == 0; }
    public static <T> ProtocolResult<T> result(int code, String message) {
        return result(code, message, null);
    }
    public static <T> ProtocolResult<T> result(int code, String message, T data) {
        return new ProtocolResult<>(code, message, data);
    }
    public static <T> ProtocolResult<T> result(RespCode code) {
        return result(code.getValue(), code.getName());
    }
}
