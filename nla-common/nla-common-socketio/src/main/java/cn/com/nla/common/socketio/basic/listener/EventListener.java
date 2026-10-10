package cn.com.nla.common.socketio.basic.listener;

import com.corundumstudio.socketio.listener.DataListener;

public interface EventListener<T> extends DataListener<T> {

    public Class<T>  getEventClass();
    /**
     * 获取所属事件名
     * @return
     */
    public String getEventName();

    /**
     * 获取所属空间
     * @return
     */
    public NamespaceListener getNamespace();


}
