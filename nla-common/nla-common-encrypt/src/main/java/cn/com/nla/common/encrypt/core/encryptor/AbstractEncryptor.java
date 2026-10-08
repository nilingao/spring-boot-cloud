package cn.com.nla.common.encrypt.core.encryptor;

import cn.com.nla.common.encrypt.core.EncryptContext;
import cn.com.nla.common.encrypt.core.IEncryptor;

/**
 * 所有加密执行者的基类
 *
 * @author TZY
 * @version 4.6.0
 */
public abstract class AbstractEncryptor implements IEncryptor {

    /**
     * 初始化加密执行者。
     *
     * @param context 加密上下文
     */
    public AbstractEncryptor(EncryptContext context) {
        // 用户配置校验与配置注入
    }

}
