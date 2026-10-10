package cn.com.nla.common.socketio.basic.netty.factory;


import cn.com.nla.common.socketio.basic.netty.biz.Biz;

public interface BizFactory {
    public Biz create(int msgCode);
}
