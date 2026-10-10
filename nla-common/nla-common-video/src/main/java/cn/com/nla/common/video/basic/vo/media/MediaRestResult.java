package cn.com.nla.common.video.basic.vo.media;


import cn.com.nla.common.video.basic.enums.RespCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaRestResult implements Serializable {
    public static final MediaRestResult SUCCESS = new MediaRestResult();

    private int code;

    private int hit;

    private Boolean exist;

    private Integer local_port;

    private Integer port;
    private String msg;
    private Object data;

    public MediaRestResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static MediaRestResult result(RespCode respCode) {
        return new MediaRestResult(respCode.getValue(), respCode.getName());
    }

    public static MediaRestResult result(int code, String msg) {
        return new MediaRestResult(code, msg);
    }
    public static MediaRestResult result(int code, String msg, Object data) {
        MediaRestResult result = new MediaRestResult(code, msg);
        result.data = data;
        return result;
    }
}
