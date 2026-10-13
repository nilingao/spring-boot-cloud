package cn.com.nla.callcenter.spi;

import cn.hutool.core.bean.BeanUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 数据层对象到协议视图对象的拷贝工具。
 *
 * <p>源对象为 {@code null} 时返回 {@code null}，保持协议层大量的空值判断语义；
 * 两侧字段同名时直接按属性名拷贝（数值与字符串互转由拷贝器完成，与旧 MapStruct
 * AgentConvert/PlatformConvert 的同名隐式转换语义一致），不同名映射关系旧转换亦未
 * 覆盖（如 soundRilePath 与 soundFilePath），维持旧语义不引入额外映射器。</p>
 *
 * @author TZY
 */
final class FsSpiConvert {

    /**
     * 工具类不允许实例化。
     */
    private FsSpiConvert() {
    }

    /**
     * 拷贝单个对象到目标类型。
     *
     * @param source     源对象
     * @param targetType 目标类型
     * @param <T>        目标类型
     * @return 目标对象，源对象为空时返回 null
     */
    static <T> T to(Object source, Class<T> targetType) {
        return source == null ? null : BeanUtil.copyProperties(source, targetType);
    }

    /**
     * 拷贝对象集合到目标类型列表。
     *
     * @param sources    源集合
     * @param targetType 目标类型
     * @param <T>        目标类型
     * @return 目标对象列表，源集合为空时返回空列表
     */
    static <T> List<T> toList(Collection<?> sources, Class<T> targetType) {
        if (sources == null || sources.isEmpty()) {
            return new ArrayList<>();
        }
        return BeanUtil.copyToList(sources, targetType);
    }
}
