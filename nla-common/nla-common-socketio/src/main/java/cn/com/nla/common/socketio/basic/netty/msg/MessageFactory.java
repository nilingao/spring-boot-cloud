package cn.com.nla.common.socketio.basic.netty.msg;

public interface MessageFactory {

    public static MessageFactory DEFAULT_INSTANCE = new DefaultMessageFactory();

    public abstract Class<? extends Message> getClass(int msgCode);
}
