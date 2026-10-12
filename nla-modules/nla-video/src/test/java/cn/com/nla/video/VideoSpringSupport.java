package cn.com.nla.video;

import cn.com.nla.common.core.utils.SpringUtils;
import io.github.linpeilie.Converter;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;
import org.springframework.context.support.GenericApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * 纯单测共享上下文：MapstructUtils 与 JsonUtils 均以静态字段调用 SpringUtils.getBean 初始化，
 * 不预装容器会在类加载时直接失败。沿用 nla-face 的 GenericApplicationContext 方案，
 * 转换器按 mapstruct-plus 生成类命名规则反射定位，避免为 12 张表手写桩。
 */
final class VideoSpringSupport {

    private static GenericApplicationContext context;

    private VideoSpringSupport() {
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
        context.registerBean(Converter.class, VideoTestConverter::new);
        context.refresh();
    }

    /**
     * 按类型注册并缓存一个 Mockito 桩，供 VideoService/RedisService 这类静态持有器解析。
     *
     * <p>静态上下文全 JVM 只装配一次，桩会被多个测试类共享；需要独立场景时先 {@code Mockito.reset}。
     * 因为 {@code SpringUtils} 继承了 hutool 的 {@code SpringUtil}，手动注册的单例对静态持有器可见。</p>
     *
     * @param type 服务接口或实现类
     * @param <T>  服务类型
     * @return 缓存的桩实例
     */
    static synchronized <T> T mock(Class<T> type) {
        ensureInstalled();
        String name = type.getName();
        Object existing = context.getBeanFactory().getSingleton(name);
        if (existing != null) {
            return type.cast(existing);
        }
        T bean = Mockito.mock(type);
        context.getBeanFactory().registerSingleton(name, bean);
        return bean;
    }

    /** 仅通过编译期生成的 BoToEntity / EntityToVo 映射器转换，缺失生成类时直接失败暴露意外依赖。 */
    static class VideoTestConverter extends Converter {

        @Override
        public <S, T> T convert(S source, Class<T> desc) {
            if (source == null) { return null; }
            String name = source.getClass().getPackageName() + "."
                + source.getClass().getSimpleName() + "To" + desc.getSimpleName() + "Mapper";
            try {
                Object generated = Mappers.getMapper(Class.forName(name));
                return desc.cast(generated.getClass().getMethod("convert", source.getClass())
                    .invoke(generated, source));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("缺少生成映射器: " + name, e);
            }
        }

        @Override
        public <S, T> List<T> convert(List<S> sourceList, Class<T> desc) {
            return sourceList.stream().map(s -> convert(s, desc)).toList();
        }
    }
}
