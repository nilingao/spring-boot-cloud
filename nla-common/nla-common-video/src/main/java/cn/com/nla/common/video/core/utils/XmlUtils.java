package cn.com.nla.common.video.core.utils;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.common.CatalogEventConstant;
import cn.com.nla.common.video.basic.enums.GbIdConstant;
import cn.com.nla.common.video.basic.enums.PtzTypeEnum;
import cn.com.nla.common.video.basic.interfaces.MessageElement;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.core.properties.SipConfigProperties;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.sip.RequestEvent;
import javax.sip.message.Request;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

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
     * @param json
     */
    public static void node2Json(Element element, JSONObject json) {
        //xml 转 map
        Map<String, Object> map = XmlUtil.xmlToMap(element);
        //map 转 json
        json = JSONUtil.parseObj(map);
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

    public static DeviceChannelVo channelContentHandler(Element itemDevice, DeviceVo deviceVo, String event){
        DeviceChannelVo deviceChannelVo = new DeviceChannelVo();
        deviceChannelVo.setDeviceId(deviceVo.getDeviceId());
        String channelId = XmlUtils.getText(itemDevice, "DeviceID");
        if (ObjectUtils.isEmpty(channelId)) {
            log.warn("解析Catalog消息时发现缺少 DeviceID");
            return null;
        }
        deviceChannelVo.setChannelId(channelId);
        if (event != null && !event.equals(CatalogEventConstant.ADD) && !event.equals(CatalogEventConstant.UPDATE)) {
            // 除了ADD和update情况下需要识别全部内容，
            return deviceChannelVo;
        }
        // 名称
        deviceChannelVo.setName(XmlUtils.getText(itemDevice, "Name"));
        // 父设备/区域/系统ID
        String parentId = XmlUtils.getText(itemDevice, "ParentID");
        if (parentId != null && parentId.equalsIgnoreCase("null")) {
            parentId = null;
        }
        // 虚拟组织所属的业务分组ID,业务分组根据特定的业务需求制定,一个业务分组包含一组特定的虚拟组织
        String businessGroupID = XmlUtils.getText(itemDevice, "BusinessGroupID");
        // 行政区域
        String civilCode = XmlUtils.getText(itemDevice, "CivilCode");
        if(channelId.length() <= 8){
            deviceChannelVo.setHasAudio(ConstEnum.Flag.NO.getValue());
            if (parentId.contains("/")) {
                if (businessGroupID == null) {
                    businessGroupID = parentId.substring(0, parentId.indexOf("/"));
                }
                deviceChannelVo.setParentId(parentId.substring(parentId.lastIndexOf("/") + 1));
            }else {
                deviceChannelVo.setParentId(parentId);
            }
            // 兼容设备通道信息中自己为自己父节点的情况
            if (deviceChannelVo.getParentId().equals(deviceChannelVo.getChannelId())) {
                deviceChannelVo.setParentId(null);
            }
            deviceChannelVo.setCivilCode(civilCode);
            deviceChannelVo.setStatus(ConstEnum.Flag.YES.getValue());
            return deviceChannelVo;
        }
        if(channelId.length() != 20) {
            log.error("[xml解析] 失败，编号不符合国标28181定义： {}", channelId);
            return null;
        }
        GbIdConstant.Type type = GbIdConstant.Type.getType(Integer.parseInt(channelId.substring(10, 13)));
        if(type == GbIdConstant.Type.TYPE_136 || type == GbIdConstant.Type.TYPE_137 || type == GbIdConstant.Type.TYPE_138 ){
            deviceChannelVo.setHasAudio(ConstEnum.Flag.NO.getValue());
        }else {
            deviceChannelVo.setHasAudio(ConstEnum.Flag.YES.getValue());
        }
        // 设备厂商
        String manufacturer = XmlUtils.getText(itemDevice, "Manufacturer");
        // 设备型号
        String model = XmlUtils.getText(itemDevice, "Model");
        // 设备归属
        String owner = XmlUtils.getText(itemDevice, "Owner");
        // 证书序列号(有证书的设备必选)
        String certNum = XmlUtils.getText(itemDevice, "CertNum");
        // 注册方式(必选)缺省为1;1:符合IETFRFC3261标准的认证注册模式;2:基于口令的双向认证注册模式;3:基于数字证书的双向认证注册模式
        String registerWay = XmlUtils.getText(itemDevice, "RegisterWay");
        // 保密属性(必选)缺省为0;0:不涉密,1:涉密
        String secrecy = XmlUtils.getText(itemDevice, "Secrecy");
        // 安装地址
        String address = XmlUtils.getText(itemDevice, "Address");
        // 信令安全模式(可选)缺省为0; 0:不采用;2:S/MIME 签名方式;3:S/MIME加密签名同时采用方式;4:数字摘要方式
        String safetyWay = XmlUtils.getText(itemDevice, "SafetyWay");
        // 警区
        String block = XmlUtils.getText(itemDevice, "Block");
        // 设备口令
        String password = XmlUtils.getText(itemDevice, "Password");
        // 设备状态
        String status = XmlUtils.getText(itemDevice, "Status");
        // 识别自带的目录标识  当为设备时,是否有子设备(必选)1有,0没有
        String parental = XmlUtils.getText(itemDevice, "Parental");
        // 设备/区域/系统IP地址
        String ipAddress = XmlUtils.getText(itemDevice, "IPAddress");
        // 证书有效标识(有证书的设备必选)缺省为0;证书有效标识:0:无效 1:有效
        String certifiable = XmlUtils.getText(itemDevice, "Certifiable");
        // 无效原因码(有证书且证书无效的设备必选)
        String errCode = XmlUtils.getText(itemDevice, "ErrCode");
        // 证书终止有效期(有证书的设备必选)
        String endTime = XmlUtils.getText(itemDevice, "EndTime");
        // 设备/区域/系统端口
        String port = XmlUtils.getText(itemDevice, "Port");
        // 经度
        String longitude = XmlUtils.getText(itemDevice, "Longitude");
        // 纬度
        String latitude = XmlUtils.getText(itemDevice, "Latitude");
        // -摄像机类型扩展,标识摄像机类型:1-球机;2-半球;3-固定枪机;4-遥控枪机。当目录项为摄像机时可选
        String ptzType = XmlUtils.getText(itemDevice, "PTZType");
        switch (type){
            case TYPE_200:
                deviceChannelVo.setStatus(ConstEnum.Flag.YES.getValue());
                deviceChannelVo.setManufacture(manufacturer);
                deviceChannelVo.setModel(model);
                deviceChannelVo.setOwner(owner);
                deviceChannelVo.setAddress(address);
                deviceChannelVo.setSecrecy("null".equals(secrecy)||StringUtils.isEmpty(secrecy)?0:Integer.parseInt(secrecy));
                if(StringUtils.isNotEmpty(civilCode)){
                    deviceChannelVo.setCivilCode(civilCode);
                    deviceChannelVo.setParentId(civilCode);
                }else if(StringUtils.isNotEmpty(parentId)){
                    deviceChannelVo.setParentId(parentId);
                }
                if (ObjectUtils.isEmpty(registerWay)) {
                    deviceChannelVo.setRegisterWay(1);
                } else {
                    deviceChannelVo.setRegisterWay(Integer.parseInt(registerWay));
                }
                break;
            case TYPE_215:
                deviceChannelVo.setStatus(ConstEnum.Flag.YES.getValue());
                if(StringUtils.isNotEmpty(parentId)){
                    deviceChannelVo.setParentId(parentId);
                }else {
                    deviceChannelVo.setCivilCode(civilCode);
                }
                break;
            case TYPE_216:
                deviceChannelVo.setStatus(ConstEnum.Flag.YES.getValue());
                deviceChannelVo.setBusinessGroupId(businessGroupID);
                if (StringUtils.isNotEmpty(parentId) ) {
                    if (parentId.contains("/")) {
                        parentId = parentId.substring(0, parentId.indexOf("/"));
                    }
                    deviceChannelVo.setParentId(parentId);
                }else {
                    deviceChannelVo.setParentId(businessGroupID);
                }
                break;
            default:
                deviceChannelVo.setManufacture(manufacturer);
                deviceChannelVo.setModel(model);
                deviceChannelVo.setOwner(owner);
                deviceChannelVo.setCivilCode(civilCode);
                deviceChannelVo.setBusinessGroupId(businessGroupID);
                deviceChannelVo.setBlock(block);
                deviceChannelVo.setAddress(address);
                deviceChannelVo.setSecrecy("null".equals(secrecy)||StringUtils.isEmpty(secrecy)?0:Integer.parseInt(secrecy));
                deviceChannelVo.setRegisterWay(NumberUtil.isNumber(registerWay)?Integer.parseInt(registerWay):1);
                deviceChannelVo.setSafetyWay(NumberUtil.isNumber(safetyWay)?Integer.parseInt(safetyWay):0);
                deviceChannelVo.setCertNum(certNum);
                deviceChannelVo.setCertifiable(NumberUtil.isNumber(certifiable)?Integer.parseInt(certifiable):0);
                deviceChannelVo.setErrCode(NumberUtil.isNumber(errCode)?Integer.parseInt(errCode):0);
                deviceChannelVo.setEndTime(cn.hutool.core.date.LocalDateTimeUtil.of("null".equals(endTime)||StringUtils.isEmpty(endTime)?null:DateUtil.parse(endTime)));
                deviceChannelVo.setIpAddress(ipAddress);
                deviceChannelVo.setPort(NumberUtil.isNumber(port)?Integer.parseInt(port):0);
                deviceChannelVo.setPassword(password);
                deviceChannelVo.setLongitude(NumberUtil.isNumber(longitude)?Double.parseDouble(longitude):0.0);
                deviceChannelVo.setLatitude(NumberUtil.isNumber(latitude)?Double.parseDouble(latitude):0.0);
                deviceChannelVo.setParental(StringUtils.isNotEmpty(parental) && NumberUtil.isNumber(parental) && Integer.parseInt(parental) == 0?0:1);
                deviceChannelVo.setGpsTime(java.time.LocalDateTime.now());
                deviceChannelVo.setSecrecy(NumberUtil.isNumber(secrecy)?Integer.parseInt(secrecy):0);
                if (StringUtils.isNotEmpty(parentId) ) {
                    if (parentId.contains("/")) {
                        parentId = parentId.substring(0, parentId.indexOf("/"));
                    }
                    //如果是SIP国标 改为设备国标
                    if(RedisService.getRegisterServerManager().getSip(parentId)!=null){
                        parentId = deviceVo.getDeviceId();
                    }
                    deviceChannelVo.setParentId(parentId);
                }else {
                    if(StringUtils.isNotEmpty(businessGroupID)){
                        deviceChannelVo.setParentId(businessGroupID);
                    }else {
                        deviceChannelVo.setParentId(deviceVo.getDeviceId());
                    }
//                    else if(StringUtils.isNotEmpty(deviceChannelVo.getCivilCode())){
//                        deviceChannelVo.setParentId(deviceChannelVo.getCivilCode());
//                    }
                }
                if (StringUtils.isEmpty(status)) {
                    // ONLINE OFFLINE HIKVISION DS-7716N-E4 NVR的兼容性处理
                    if (status.equals("ON") || status.equals("On") || status.equals("ONLINE") || status.equals("OK")) {
                        deviceChannelVo.setStatus(1);
                    }
                    if (status.equals("OFF") || status.equals("Off") || status.equals("OFFLINE")) {
                        deviceChannelVo.setStatus(0);
                    }
                }else {
                    deviceChannelVo.setStatus(1);
                }
                if(StringUtils.isEmpty(ptzType)){
                    //兼容INFO中的信息
                    Element info = XmlUtil.getElement(itemDevice, "Info");
                    if(XmlUtils.getText(info, "PTZType") == null || "".equals(XmlUtils.getText(info, "PTZType"))){
                        deviceChannelVo.setPtzType(PtzTypeEnum.UNKNOWN.getValue());
                    }else{
                        deviceChannelVo.setPtzType(Integer.parseInt(XmlUtils.getText(info, "PTZType")));
                    }
                }else {
                    deviceChannelVo.setPtzType(Integer.parseInt(XmlUtils.getText(itemDevice, "PTZType")));
                }
                break;
        }
        return deviceChannelVo;
    }

    /**
     * 新增方法支持内部嵌套
     *
     * @param element xmlElement
     * @param clazz 结果类
     * @param <T> 泛型
     * @return 结果对象
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static <T> T loadElement(Element element, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        T t = clazz.getDeclaredConstructor().newInstance();
        for (Field field : fields) {
            ReflectionUtils.makeAccessible(field);
            MessageElement annotation = field.getAnnotation(MessageElement.class);
            if (annotation == null) {
                continue;
            }
            String value = annotation.value();
            String subVal = annotation.subVal();

            Element element1 = XmlUtil.getElement(element,value);
            if (element1 == null) {
                continue;
            }
            if ("".equals(subVal)) {
                // 无下级数据
                Element deviceModel = XmlUtil.getElement(element, value);
                List<Element> elements = XmlUtil.getElements(deviceModel, "Item");

                Object fieldVal =  elements.isEmpty() ? element1.getNodeValue() : loadElement(element1, field.getType());
                Object o = simpleTypeDeal(field.getType(), fieldVal);
                ReflectionUtils.setField(field, t,  o);
            } else {
                // 存在下级数据
                ArrayList<Object> list = new ArrayList<>();
                Type genericType = field.getGenericType();
                if (!(genericType instanceof ParameterizedType)) {
                    continue;
                }
                Class<?> aClass = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
                Element deviceModel = XmlUtil.getElement(element, subVal);
                for (Element element2 : XmlUtil.getElements(deviceModel,"Item")) {
                    list.add(loadElement(element2, aClass));
                }
                ReflectionUtils.setField(field, t, list);
            }
        }
        return t;
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