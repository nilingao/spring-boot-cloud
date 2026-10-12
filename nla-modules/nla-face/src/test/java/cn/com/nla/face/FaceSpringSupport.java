package cn.com.nla.face;

import cn.com.nla.common.core.utils.SpringUtils;
import cn.com.nla.face.domain.Person;
import cn.com.nla.face.domain.PersonToPersonVoMapper;
import cn.com.nla.face.domain.bo.PersonBo;
import cn.com.nla.face.domain.bo.PersonBoToPersonMapper;
import cn.com.nla.face.domain.vo.PersonVo;
import io.github.linpeilie.Converter;
import org.mapstruct.factory.Mappers;
import org.springframework.context.support.GenericApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * 纯单测共享上下文：JsonUtils 与 MapstructUtils 均以静态字段调用 SpringUtils.getBean 初始化，
 * 不预装容器会在类加载时直接失败。此处沿用 nla-admin 契约测试的 GenericApplicationContext 方案，
 * 注册 JsonMapper 与基于 mapstruct 生成类的桩 Converter，整个模块安装一次即可。
 */
final class FaceSpringSupport {

    private static GenericApplicationContext context;

    private FaceSpringSupport() {
    }

    /**
     * 幂等安装测试用 Spring 静态上下文；先装载的 JsonMapper/Converter 会被工具类静态字段永久缓存。
     */
    static synchronized void ensureInstalled() {
        if (context != null) {
            return;
        }
        context = new GenericApplicationContext();
        context.registerBean(SpringUtils.class, SpringUtils::new);
        context.registerBean(JsonMapper.class, () -> JsonMapper.builder().build());
        context.registerBean(Converter.class, FaceTestConverter::new);
        context.refresh();
    }

    /** 仅覆盖被测代码实际经过的两条转换路径，其余转换直接失败暴露意外依赖。 */
    static class FaceTestConverter extends Converter {

        @Override
        public <S, T> T convert(S source, Class<T> desc) {
            if (source instanceof PersonBo bo && desc == Person.class) {
                return desc.cast(Mappers.getMapper(PersonBoToPersonMapper.class).convert(bo));
            }
            if (source instanceof Person person && desc == PersonVo.class) {
                return desc.cast(Mappers.getMapper(PersonToPersonVoMapper.class).convert(person));
            }
            throw new IllegalStateException("测试桩转换器不支持该转换: " + source + " -> " + desc);
        }

        @Override
        public <S, T> List<T> convert(List<S> sourceList, Class<T> desc) {
            return sourceList.stream().map(s -> convert(s, desc)).toList();
        }
    }
}
