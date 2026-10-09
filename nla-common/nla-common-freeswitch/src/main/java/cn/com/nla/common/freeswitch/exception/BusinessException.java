package cn.com.nla.common.freeswitch.exception;


import cn.com.nla.common.freeswitch.enums.RespCode;

/**
 * @author caoliang
 */
public class BusinessException  extends RuntimeException{

    public Integer code;
    public String message;

    public BusinessException() {
    }
    public BusinessException(RespCode respCode) {
        super(respCode.getName());
        this.code =respCode.getValue();
        this.message =respCode.getName();
    }

    public BusinessException(int code,String message) {
        super(message);
        this.code =code;
        this.message =message;
    }

    public BusinessException(String message) {
        super(message);
        this.message =message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
