package cn.com.nla.common.freeswitch.client.sip.callback;

public interface InviteErrorCallback<T> {

    void run(int code, String msg, T data);
}