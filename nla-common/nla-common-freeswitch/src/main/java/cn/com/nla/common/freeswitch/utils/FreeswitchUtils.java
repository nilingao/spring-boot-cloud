package cn.com.nla.common.freeswitch.utils;

import cn.com.nla.common.freeswitch.vo.fs.FreeswitchXmlVo;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * 通话中心工具类
 */
@Slf4j
public class FreeswitchUtils {

    public static Snowflake snowflake = null;

    static {
        Properties p = new Properties();
        try
        {
            // 加载classpath目录下的vm文件
            p.setProperty("resource.loader.file.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            // 定义字符集
            p.setProperty(Velocity.INPUT_ENCODING, StandardCharsets.UTF_8.name());
            // 初始化Velocity引擎，指定配置Properties
            Velocity.init(p);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        snowflake = IdUtil.createSnowflake(0, 0);
    }

    /**
     * 获取Fs xml模板相关信息
     * @param vo
     * @return
     */
    public static String getXmlConfig(FreeswitchXmlVo vo){
        if(vo.getFsTypeEnum() == null){
            return null;
        }
        StringWriter sw = new StringWriter();
        try {
            VelocityContext context = new VelocityContext();
            if(vo.getModelMap()!= null){
                for (Map.Entry<String, Object> entry : vo.getModelMap().entrySet()) {
                    context.put(entry.getKey(),entry.getValue());
                }
            }
            // 渲染模板
            Template tpl = Velocity.getTemplate(vo.getFsTypeEnum().getPath(), StandardCharsets.UTF_8.name());
            tpl.merge(context, sw);
        }catch (Exception e){
            log.error("模板数据解析失败：",e);
        }
        return sw.toString();
    }

    /**
     * 表达式替换
     *
     * @param body   #{[name]}
     * @param params
     */
    public static String expression(String body, Map<String, Object> params) {
        ExpressionParser parser = new SpelExpressionParser();
        TemplateParserContext parserContext = new TemplateParserContext("${","}");
        String content = null;
        try {
            content = parser.parseExpression(body, parserContext).getValue(params, String.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return content;
    }
}
