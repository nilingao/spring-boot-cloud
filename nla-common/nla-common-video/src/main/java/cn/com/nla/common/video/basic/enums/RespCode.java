package cn.com.nla.common.video.basic.enums;

/**
 * Video 封装内部状态码。
 * <p>
 * 迁移自旧 springbootcomm RespCode，仅保留视频封装所需的语义状态码，
 * 不作为 Web 响应包装使用（Web 层统一由上层 controller 包装 R）。
 *
 * @author TZY
 */
public enum RespCode {

    /**
     * 成功
     */
    CODE_0(0, "成功"),
    /**
     * 参数错误 / 业务失败
     */
    CODE_2(2, "参数错误"),
    ;

    private final int value;
    private final String name;

    RespCode(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }
}
