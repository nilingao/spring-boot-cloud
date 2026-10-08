package cn.com.nla.system.api.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 小程序登录用户上下文对象。
 *
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class XcxLoginUser extends LoginUser {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 小程序 openid。
     */
    private String openid;

}
