package cn.com.nla.common.socketio.core.netty.biz;
import cn.com.nla.common.socketio.basic.netty.biz.Biz;
import cn.com.nla.common.socketio.basic.netty.factory.BizFactory;
import org.springframework.beans.factory.BeanFactory;

/** Explicit Spring factory; retains the biz%09d naming contract. @author TZY */
public class DefaultBizFactory implements BizFactory {
    private final BeanFactory beans;
    public DefaultBizFactory(BeanFactory beans) { this.beans = beans; }
    @Override public Biz create(int msgCode) {
        String name = String.format(java.util.Locale.ROOT, "biz%09d", msgCode);
        return beans.containsBean(name) ? beans.getBean(name, Biz.class) : null;
    }
}
