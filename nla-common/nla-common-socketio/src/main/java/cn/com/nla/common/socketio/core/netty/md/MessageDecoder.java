package cn.com.nla.common.socketio.core.netty.md;

import cn.com.nla.common.socketio.basic.netty.msg.Message;
import cn.com.nla.common.socketio.basic.netty.msg.MessageFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;
import java.util.List;
import java.util.Objects;

/** Decode bounded frames; CRC covers only the current body. @author TZY */
public class MessageDecoder extends ByteToMessageDecoder {
    private final MessageFactory factory;
    private final int maxFrameLength;
    public MessageDecoder(MessageFactory factory) { this(factory, 1024 * 1024); }
    public MessageDecoder(MessageFactory factory, int maxFrameLength) {
        this.factory = Objects.requireNonNull(factory);
        if (maxFrameLength < 14) { throw new IllegalArgumentException("maxFrameLength must be at least 14"); }
        this.maxFrameLength = maxFrameLength;
    }
    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf buffer, List<Object> output) throws Exception {
        if (buffer.readableBytes() < 4) { return; }
        ByteBuf probe = buffer.duplicate();
        int code = probe.readInt();
        Class<? extends Message> type = factory.getClass(code);
        if (type == null) { throw new CorruptedFrameException("Unknown message code: " + code); }
        Message message = type.getDeclaredConstructor().newInstance();
        int bodyLength;
        try {
            message.readTime(probe);
            probe.readInt();
            bodyLength = probe.readInt();
            if (message.checkCRC()) { probe.readShort(); }
        } catch (IndexOutOfBoundsException incompleteHeader) { return; }
        int headerLength = probe.readerIndex() - buffer.readerIndex();
        if (bodyLength < 0) { throw new CorruptedFrameException("Negative body length"); }
        long frameLength = (long) headerLength + bodyLength;
        if (frameLength > maxFrameLength) { throw new TooLongFrameException("Frame exceeds " + maxFrameLength); }
        if (buffer.readableBytes() < frameLength) { return; }
        ByteBuf frame = buffer.slice(buffer.readerIndex(), (int) frameLength);
        message.decode(frame);
        if (frame.isReadable()) { throw new CorruptedFrameException("Message did not consume its complete body"); }
        buffer.skipBytes((int) frameLength);
        output.add(message);
    }
}
