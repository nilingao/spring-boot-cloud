package cn.com.nla.common.mq.core.converter;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.util.MimeTypeUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** 对象使用 Jackson3 JSON，兼容入站文本/原始字节及泛型监听参数。 @author TZY */
public class MqMessageConverter implements SmartMessageConverter {
    private final JacksonJsonMessageConverter json;
    public MqMessageConverter(String... trustedPackages) { json = new JacksonJsonMessageConverter(trustedPackages); }

    @Override
    public Message toMessage(Object object, MessageProperties properties) {
        if (object instanceof byte[] bytes) {
            properties.setContentType(MessageProperties.CONTENT_TYPE_BYTES);
            properties.setContentLength(bytes.length);
            return new Message(bytes, properties);
        }
        return json.toMessage(object, properties);
    }

    @Override public Object fromMessage(Message message) { return fromMessage(message, null); }

    @Override
    public Object fromMessage(Message message, Object conversionHint) {
        var properties = message.getMessageProperties();
        var mime = MimeTypeUtils.parseMimeType(properties.getContentType());
        if (mime.getSubtype().equalsIgnoreCase("json") || mime.getSubtype().toLowerCase(java.util.Locale.ROOT).endsWith("+json")) {
            return json.fromMessage(message, conversionHint);
        }
        if (mime.getType().equalsIgnoreCase("text")) {
            Charset charset = mime.getCharset();
            if (charset == null && properties.getContentEncoding() != null) {
                charset = Charset.forName(properties.getContentEncoding());
            }
            return new String(message.getBody(), charset == null ? StandardCharsets.UTF_8 : charset);
        }
        return message.getBody();
    }
}
