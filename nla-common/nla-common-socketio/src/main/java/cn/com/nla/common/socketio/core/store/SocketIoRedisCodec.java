package cn.com.nla.common.socketio.core.store;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.corundumstudio.socketio.protocol.Packet;
import io.netty.buffer.Unpooled;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

/** 独立 Jackson 2 codec，保留内部消息类型，不依赖应用默认 Redis codec。 @author TZY */
public class SocketIoRedisCodec extends BaseCodec {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .addMixIn(Packet.class, PacketMixin.class)
        .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.corundumstudio.socketio.")
            .allowIfSubType("cn.com.nla.")
            .allowIfSubType("java.util.")
            .allowIfSubType("java.time.")
            .allowIfSubType("java.lang.")
            .allowIfSubTypeIsArray().build(), ObjectMapper.DefaultTyping.NON_FINAL);

    private final Encoder encoder = value -> Unpooled.wrappedBuffer(mapper.writerFor(Object.class).writeValueAsBytes(value));
    private final Decoder<Object> decoder = (buffer, state) -> {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return mapper.readValue(bytes, Object.class);
    };

    @JsonIgnoreProperties({"dataSource", "attachments", "attachmentsCount"})
    private abstract static class PacketMixin { }

    @Override public Encoder getValueEncoder() { return encoder; }
    @Override public Decoder<Object> getValueDecoder() { return decoder; }
}
