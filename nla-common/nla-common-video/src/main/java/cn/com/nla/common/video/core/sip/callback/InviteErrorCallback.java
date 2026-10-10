package cn.com.nla.common.video.core.sip.callback;

public interface InviteErrorCallback<T> {

    void run(int code, String msg, T data);
}