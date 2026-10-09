package cn.com.nla.common.freeswitch.enums.fs;

public enum DirectionEnum {
    /**
     * 呼入
     */
    INBOUND(1),

    /**
     * 外呼
     */
    OUTBOUND(2);

    private Integer code;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    DirectionEnum(Integer code) {
        this.code = code;
    }
}
