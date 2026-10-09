package cn.com.nla.common.freeswitch.common.interfaces;


import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;

@FunctionalInterface
public interface ResultEvent {
    void result(RestResultEvent result);
}
