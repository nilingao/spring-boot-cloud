package cn.com.nla.common.socketio.basic.netty.msg;

public class DefaultMessageFactory implements MessageFactory {

    @Override
    public Class<? extends Message> getClass(int msgCode) {
        return MsgCode.get(msgCode);
    }
}
