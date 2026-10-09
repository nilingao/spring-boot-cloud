package cn.com.nla.common.freeswitch.utils;

import cn.hutool.core.util.XmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.sip.RequestEvent;
import javax.sip.message.Request;
import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * 基于dom4j的工具包
 *
 *
 */
@Slf4j
public class XmlUtils {
    /**
     * 日志服务
     */

    /**
     * 解析XML为Document对象
     *
     * @param xml 被解析的XMl
     *
     * @return Document
     */
    public static Element parseXml(String xml) {
        Document document = XmlUtil.parseXml(xml);
        return XmlUtil.getRootElement(document);
    }

    /**
     * 获取element对象的text的值
     *
     * @param em  节点的对象
     * @param tag 节点的tag
     * @return 节点
     */
    public static String getText(Element em, String tag) {
        if (null == em) {
            return null;
        }
       return getText(em,tag,null);
    }

    public static String getText(Element em, String tag,String dfValues) {
        if (null == em) {
            return null;
        }
        String text = XmlUtil.elementText(em, tag, dfValues);
        if(StringUtils.isEmpty(text)){
            return text;
        }
        return text.trim();
    }

    /**
     * xml转json
     *
     * @param element
     */
    public static Map<String, Object> node2Json(Element element) {
        return XmlUtil.xmlToMap(element);
    }
    public static  Element getRootElement(RequestEvent evt)  {

        return getRootElement(evt, "gb2312");
    }

    public static Element getRootElement(RequestEvent evt, String charset)  {
        Request request = evt.getRequest();
        return getRootElement(request.getRawContent(), charset);
    }

    public static Element getRootElement(byte[] content, String charset) {
        if (charset == null) {
            charset = "gb2312";
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
        InputSource inputSource = new InputSource(byteArrayInputStream);
        inputSource.setEncoding(charset);
        Document document = XmlUtil.readXML(inputSource);
        return XmlUtil.getRootElement(document);
    }

    private enum ChannelType{
        CivilCode, BusinessGroup,VirtualOrganization,Other
    }

    /**
     * 简单类型处理
     *
     * @param tClass
     * @param val
     * @return
     */
    private static Object simpleTypeDeal(Class<?> tClass, Object val) {
        if (tClass.equals(String.class)) {
            return val.toString();
        }
        if (tClass.equals(Integer.class)) {
            return Integer.valueOf(val.toString());
        }
        if (tClass.equals(Double.class)) {
            return Double.valueOf(val.toString());
        }
        if (tClass.equals(Long.class)) {
            return Long.valueOf(val.toString());
        }
        return val;
    }
}