package cn.com.nla.common.socketio;

import cn.com.nla.common.socketio.basic.listener.EventListener;
import cn.com.nla.common.socketio.basic.listener.NamespaceListener;
import cn.com.nla.common.socketio.core.config.SocketIoAutoConfiguration;
import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.annotation.OnEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(30)
class SocketIoProtocolTest {
    private ApplicationContextRunner enabled(int port) {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(SocketIoAutoConfiguration.class))
            .withPropertyValues("socket-io.enabled=true", "socket-io.host=127.0.0.1", "socket-io.port=" + port,
                "socket-io.work-count=1", "socket-io.boss-count=1");
    }
    @Test void realWebsocketTypedEventAckAnnotationRoomBroadcastAndPortRelease() throws Exception {
        int port = freePort();
        var chat = new SocketIoContractTest.TestNamespace("/chat");
        var solo = new SocketIoContractTest.TestNamespace("/solo");
        enabled(port).withBean("chat", NamespaceListener.class, () -> chat)
            .withBean("solo", NamespaceListener.class, () -> solo)
            .withBean("echo", EventListener.class, () -> new SocketIoContractTest.Echo(chat))
            .withBean(AnnotatedEvents.class, AnnotatedEvents::new)
            .run(context -> {
                assertThat(context).hasNotFailed();
                try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
                    WsInbox inbox = new WsInbox();
                    WebSocket websocket = http.newWebSocketBuilder()
                        .buildAsync(URI.create("ws://127.0.0.1:" + port + "/socket.io/?EIO=4&transport=websocket"), inbox)
                        .get(5, TimeUnit.SECONDS);
                    try {
                        assertTrue(inbox.take().startsWith("0{"));
                        websocket.sendText("40/chat,", true).join();
                        assertTrue(inbox.take().startsWith("40/chat,"));
                        websocket.sendText("42/chat,7[\"echo\",{\"text\":\"你好\"}]", true).join();
                        assertEquals("43/chat,7[\"你好\"]", inbox.take());
                        context.getBean(SocketIOServer.class).getNamespace("/chat").getRoomOperations("room")
                            .sendEvent("notice", "房间通知");
                        assertEquals("42/chat,[\"notice\",\"房间通知\"]", inbox.take());
                        websocket.sendText("40/solo,", true).join();
                        assertTrue(inbox.take().startsWith("40/solo,"));
                        websocket.sendText("40", true).join();
                        assertTrue(inbox.take().startsWith("40"));
                        websocket.sendText("428[\"annotated\",\"注解回调\"]", true).join();
                        assertEquals("438[\"注解回调\"]", inbox.take());
                    } finally { websocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join(); }
                } catch (Exception failure) { throw new AssertionError(failure); }
            });
        try (ServerSocket rebound = new ServerSocket()) { rebound.bind(new InetSocketAddress("127.0.0.1", port)); }
    }
    @Test void pollingHandshakeAcceptsAuthorizedRequestsAndRejectsUnauthorizedOnes() throws Exception {
        int port = freePort();
        enabled(port).withBean(AuthorizationListener.class,
                () -> data -> new AuthorizationResult("valid".equals(data.getSingleUrlParam("token"))))
            .run(context -> {
                assertThat(context).hasNotFailed();
                try (HttpClient http = HttpClient.newHttpClient()) {
                    var denied = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                        + "/socket.io/?EIO=4&transport=polling&token=invalid")).build(), HttpResponse.BodyHandlers.ofString());
                    assertEquals(401, denied.statusCode());
                    var accepted = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                        + "/socket.io/?EIO=4&transport=polling&token=valid")).build(), HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, accepted.statusCode()); assertTrue(accepted.body().startsWith("0{"));
                } catch (Exception failure) { throw new AssertionError(failure); }
            });
    }
    @Test void occupiedPortFailsStartupWithoutBreakingExistingListener() throws Exception {
        try (ServerSocket occupied = new ServerSocket(0, 10, InetAddress.getLoopbackAddress())) {
            enabled(occupied.getLocalPort()).run(context -> assertThat(context).hasFailed());
            assertFalse(occupied.isClosed());
        }
    }
    static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0, 10, InetAddress.getLoopbackAddress())) { return socket.getLocalPort(); }
    }
    public static class AnnotatedEvents {
        @OnEvent("annotated") public void annotated(SocketIOClient client, String data, AckRequest ack) { ack.sendAckData(data); }
    }
    static class WsInbox implements WebSocket.Listener {
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final StringBuilder text = new StringBuilder();
        @Override public void onOpen(WebSocket socket) { socket.request(1); }
        @Override public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            text.append(data);
            if (last) {
                String message = text.toString(); text.setLength(0);
                if (message.equals("2")) { socket.sendText("3", true); } else { messages.add(message); }
            }
            socket.request(1); return CompletableFuture.completedFuture(null);
        }
        String take() throws InterruptedException {
            String next = messages.poll(5, TimeUnit.SECONDS); assertNotNull(next, "WebSocket packet timed out"); return next;
        }
    }
}
