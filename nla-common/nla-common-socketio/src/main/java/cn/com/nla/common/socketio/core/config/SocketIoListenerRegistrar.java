package cn.com.nla.common.socketio.core.config;

import cn.com.nla.common.socketio.basic.listener.EventListener;
import cn.com.nla.common.socketio.basic.listener.NamespaceListener;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.util.ReflectionUtils;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 先校验整组监听器，再注册；不在 BeanPostProcessor 创建时提前实例化业务 Bean。 @author TZY */
public class SocketIoListenerRegistrar implements BeanPostProcessor, SmartInitializingSingleton, Ordered {
    private final ObjectProvider<SocketIOServer> servers;
    private final ObjectProvider<NamespaceListener> namespaces;
    private final ObjectProvider<EventListener<?>> events;
    private final ConcurrentLinkedQueue<AnnotatedBean> annotated = new ConcurrentLinkedQueue<>();

    public SocketIoListenerRegistrar(ObjectProvider<SocketIOServer> servers,
            ObjectProvider<NamespaceListener> namespaces, ObjectProvider<EventListener<?>> events) {
        this.servers = servers;
        this.namespaces = namespaces;
        this.events = events;
    }

    @Override public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> type = AopUtils.getTargetClass(bean);
        boolean hasAnnotations = ReflectionUtils.getUniqueDeclaredMethods(type).length > 0
            && java.util.Arrays.stream(ReflectionUtils.getUniqueDeclaredMethods(type)).anyMatch(method ->
                method.isAnnotationPresent(OnConnect.class) || method.isAnnotationPresent(OnDisconnect.class)
                || method.isAnnotationPresent(OnEvent.class));
        if (hasAnnotations) { annotated.add(new AnnotatedBean(bean, type)); }
        return bean;
    }

    @Override
    public void afterSingletonsInstantiated() {
        SocketIOServer server = servers.getObject();
        Map<String, NamespaceListener> registry = new LinkedHashMap<>();
        var eventList = events.orderedStream().toList();
        namespaces.orderedStream().forEach(listener -> validateNamespace(registry, listener));
        Set<String> eventKeys = new HashSet<>();
        for (EventListener<?> event : eventList) {
            validateNamespace(registry, event.getNamespace());
            if (event.getEventName() == null || event.getEventName().isBlank() || event.getEventClass() == null) {
                throw new IllegalArgumentException("Socket.IO event name/type must not be empty");
            }
            String key = event.getNamespace().getNamespaceName() + "\u0000" + event.getEventName();
            if (!eventKeys.add(key)) { throw new IllegalArgumentException("Duplicate Socket.IO event: " + event.getEventName()); }
        }
        registry.forEach((name, listener) -> {
            listener.setSocketIOServer(server);
            SocketIONamespace namespace = server.getNamespace(name);
            if (namespace == null) { namespace = server.addNamespace(name); }
            namespace.addConnectListener(listener);
            namespace.addDisconnectListener(listener);
        });
        eventList.forEach(event -> registerEvent(server, event));
        annotated.forEach(bean -> server.addListeners(bean.instance(), bean.type()));
        annotated.clear();
    }

    private static void validateNamespace(Map<String, NamespaceListener> registry, NamespaceListener listener) {
        if (listener == null || listener.getNamespaceName() == null
            || !(listener.getNamespaceName().isEmpty() || listener.getNamespaceName().startsWith("/"))) {
            throw new IllegalArgumentException("Socket.IO namespace must be empty (root) or start with /");
        }
        NamespaceListener previous = registry.putIfAbsent(listener.getNamespaceName(), listener);
        if (previous != null && previous != listener) {
            throw new IllegalArgumentException("Conflicting Socket.IO namespace: " + listener.getNamespaceName());
        }
    }

    private static <T> void registerEvent(SocketIOServer server, EventListener<T> event) {
        server.getNamespace(event.getNamespace().getNamespaceName())
            .addEventListener(event.getEventName(), event.getEventClass(), event);
    }

    private record AnnotatedBean(Object instance, Class<?> type) { }
}
