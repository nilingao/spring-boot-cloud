package cn.com.nla.face.core;

import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.json.utils.JsonUtils;
import cn.com.nla.face.domain.Person;
import cn.com.nla.face.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 人脸特征本地缓存，替代旧 FaceFeaturesCache 静态 Map：
 * 启动就绪后经 {@link PersonMapper#selectFeatureList()} 全量装载，人员增删改由服务层同步维护。
 * <p>
 * 仅单实例内存语义，与旧实现一致；多实例部署各自持有缓存，重启后与数据库对齐。
 * 同一图片编号重复入库时保留先装载的特征并告警，与旧 Collectors.toMap 直接崩溃相比检索链路不中断。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class FaceFeatureCache {

    private final PersonMapper personMapper;

    private final Map<String, float[]> features = new ConcurrentHashMap<>();

    /**
     * 应用就绪后全量装载特征缓存；单行解析失败仅跳过并告警，不阻断启动。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        clear();
        List<Person> list = personMapper.selectFeatureList();
        for (Person person : list) {
            if (StringUtils.isBlank(person.getImgId()) || StringUtils.isBlank(person.getExtract())) {
                continue;
            }
            float[] parsed;
            try {
                parsed = parseFeatures(person.getExtract());
            } catch (RuntimeException e) {
                log.warn("人脸特征解析失败，已跳过: id={}, imgId={}", person.getId(), person.getImgId());
                continue;
            }
            if (features.putIfAbsent(person.getImgId(), parsed) != null) {
                log.warn("存在重复图片编号的特征，保留先装载的一条: imgId={}", person.getImgId());
            }
        }
        log.info("人脸特征缓存装载完成，共 {} 条", features.size());
    }

    /**
     * 新增或覆盖指定图片编号的特征。
     *
     * @param imgId   图片自定义编号
     * @param feature 人脸特征数组
     */
    public void put(String imgId, float[] feature) {
        features.put(imgId, feature);
    }

    /**
     * 移除指定图片编号的特征。
     *
     * @param imgId 图片自定义编号
     */
    public void remove(String imgId) {
        features.remove(imgId);
    }

    /**
     * 清空全部缓存。
     */
    public void clear() {
        features.clear();
    }

    /**
     * 当前缓存的只读视图，供 1:N 检索遍历。
     *
     * @return 不可修改的特征映射
     */
    public Map<String, float[]> snapshot() {
        return Collections.unmodifiableMap(features);
    }

    /**
     * 缓存条目数。
     *
     * @return 条目数
     */
    public int size() {
        return features.size();
    }

    /**
     * 将 JSON 数组字符串解析为特征数组。
     *
     * @param json 形如 [0.1,0.2,...] 的特征字符串
     * @return 特征数组
     */
    public static float[] parseFeatures(String json) {
        List<Float> values = JsonUtils.parseArray(json, Float.class);
        float[] arr = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            arr[i] = values.get(i);
        }
        return arr;
    }
}
