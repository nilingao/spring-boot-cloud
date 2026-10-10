package cn.com.nla.common.socketio;

import cn.com.nla.common.socketio.basic.netty.msg.*;
import cn.com.nla.common.socketio.basic.netty.msg.model.Msg100000039;
import cn.com.nla.common.socketio.core.netty.md.*;
import cn.com.nla.common.socketio.core.netty.config.server.NettyServer;
import cn.com.nla.common.socketio.core.netty.config.client.NettyClient;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class NettyContractTest {
    static Msg100000039 message(int serial) {
        var result = new Msg100000039(); result.setId("用户"); result.setName("张三"); result.setAge(21); result.setSerial(serial); return result;
    }
    static ByteBuf encode(Message message) {
        ByteBuf buffer = Unpooled.buffer(); message.encode(buffer); return buffer;
    }
    @Test void fragmentedCrcHeaderAndBodyThenCoalescedFramesDecodeExactlyOnce() {
        var channel = new EmbeddedChannel(new MessageDecoder(MessageFactory.DEFAULT_INSTANCE));
        ByteBuf frames = Unpooled.buffer(); message(1).encode(frames); message(2).encode(frames);
        try {
            assertFalse(channel.writeInbound(frames.readRetainedSlice(13)));
            assertNull(channel.readInbound());
            assertTrue(channel.writeInbound(frames.readRetainedSlice(frames.readableBytes())));
            var first = (Msg100000039) channel.readInbound(); var second = (Msg100000039) channel.readInbound();
            assertEquals(1, first.getSerial()); assertEquals("张三", first.getName()); assertEquals(2, second.getSerial());
            assertNull(channel.readInbound());
        } finally { frames.release(); channel.finishAndReleaseAll(); }
    }
    @Test void decoderSupportsCustomCrcCodeAndTimeHeaderViaFactory() {
        var channel = new EmbeddedChannel(new MessageDecoder(code -> code == 7 ? CustomMessage.class : null));
        var sent = new CustomMessage(); sent.text = "自定义"; sent.time = 1234L; sent.setSerial(19);
        try {
            assertTrue(channel.writeInbound(encode(sent)));
            CustomMessage received = channel.readInbound();
            assertEquals("自定义", received.text); assertEquals(1234L, received.time); assertEquals(19, received.getSerial());
        } finally { channel.finishAndReleaseAll(); }
    }
    @Test void tamperedCrcUnknownCodeNegativeAndOversizedFramesAreRejected() {
        ByteBuf tampered = encode(message(1)); tampered.setByte(15, tampered.getByte(15) ^ 1);
        reject(tampered, CorruptedFrameException.class);
        reject(Unpooled.buffer().writeInt(999), CorruptedFrameException.class);
        reject(Unpooled.buffer().writeInt(100000039).writeInt(1).writeInt(-1).writeShort(0), CorruptedFrameException.class);
        reject(Unpooled.buffer().writeInt(100000039).writeInt(1).writeInt(Integer.MAX_VALUE).writeShort(0), TooLongFrameException.class);
    }
    static void reject(ByteBuf bytes, Class<? extends Exception> type) {
        var channel = new EmbeddedChannel(new MessageDecoder(MessageFactory.DEFAULT_INSTANCE));
        try { assertThrows(type, () -> channel.writeInbound(bytes)); }
        finally { channel.pipeline().remove(MessageDecoder.class); channel.finishAndReleaseAll(); }
    }
    @Test void malformedFieldLengthsDoNotAllocateUnboundedArrays() {
        var msg = message(1);
        ByteBuf buffer = Unpooled.buffer().writeInt(Integer.MAX_VALUE);
        try { assertThrows(CorruptedFrameException.class, () -> msg.readString(buffer)); } finally { buffer.release(); }
        ByteBuf negative = Unpooled.buffer().writeInt(-2);
        try { assertThrows(CorruptedFrameException.class, () -> msg.readIntList(negative)); } finally { negative.release(); }
    }
    @Test void realTcpClientServerRoundTripAndIdempotentCloseReleasePort() throws Exception {
        CountDownLatch received = new CountDownLatch(1);
        var server = server(); server.close(); server.startup();
        int port = ((InetSocketAddress) server.getChannel().localAddress()).getPort();
        var client = new NettyClient("127.0.0.1", port, 1, "socketio-tcp-test") {
            @Override protected void initChannel(SocketChannel channel) {
                channel.pipeline().addLast(new MessageDecoder(MessageFactory.DEFAULT_INSTANCE), new MessageEncoder(),
                    new SimpleChannelInboundHandler<Message>() {
                        @Override protected void channelRead0(ChannelHandlerContext context, Message msg) {
                            if (msg.getSerial() == 31 && "张三".equals(((Msg100000039) msg).getName())) { received.countDown(); }
                        }
                    });
            }
        };
        client.connectedNotice = connection -> connection.getChannel().writeAndFlush(message(31));
        try { client.startup(); assertTrue(received.await(5, TimeUnit.SECONDS)); }
        finally { client.close(); client.close(); server.close(); server.close(); }
        assertNull(client.getChannel());
        try (ServerSocket rebound = new ServerSocket(port)) { assertTrue(rebound.isBound()); }
    }
    @Test void tcpBindFailureCanBeClosedRepeatedlyWithoutLeakingResources() throws Exception {
        try (ServerSocket occupied = new ServerSocket(0)) {
            var server = new NettyServer(occupied.getLocalPort()) {
                protected SimpleChannelInboundHandler<Message> newNettyHandler() { return echo(); }
                protected MessageFactory newMessageFactory() { return MessageFactory.DEFAULT_INSTANCE; }
            };
            assertThrows(Exception.class, server::startup); server.close(); server.close(); assertNull(server.getChannel());
        }
    }
    static NettyServer server() {
        return new NettyServer(0) {
            protected SimpleChannelInboundHandler<Message> newNettyHandler() { return echo(); }
            protected MessageFactory newMessageFactory() { return MessageFactory.DEFAULT_INSTANCE; }
        };
    }
    static SimpleChannelInboundHandler<Message> echo() {
        return new SimpleChannelInboundHandler<>() {
            @Override protected void channelRead0(ChannelHandlerContext context, Message msg) { context.writeAndFlush(msg); }
        };
    }
    public static class CustomMessage extends Message {
        public long time; public String text;
        public int getMsgCode() { return 7; }
        public boolean checkCRC() { return true; }
        public void readTime(ByteBuf bytes) { time = bytes.readLong(); }
        public void writeTime(ByteBuf bytes) { bytes.writeLong(time); }
        public void readData(ByteBuf bytes) { text = readString(bytes); }
        public void writeData(ByteBuf bytes) { writeString(bytes, text); }
    }
}
