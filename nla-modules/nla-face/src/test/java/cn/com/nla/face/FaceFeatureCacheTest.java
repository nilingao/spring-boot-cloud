package cn.com.nla.face;

import cn.com.nla.face.core.FaceFeatureCache;
import cn.com.nla.face.domain.Person;
import cn.com.nla.face.mapper.PersonMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证特征缓存装载容错与 CRUD 同步语义，Mapper 全 mock，不连接数据库。 */
class FaceFeatureCacheTest {

    @BeforeAll
    static void installSpringHolder() {
        FaceSpringSupport.ensureInstalled();
    }

    @Test
    void loadSkipsInvalidRowsAndKeepsFirstDuplicate() {
        PersonMapper mapper = mock(PersonMapper.class);
        when(mapper.selectFeatureList()).thenReturn(List.of(
            row(1L, "A", "[0.5,0.25]"),
            row(2L, " ", "[0.1]"),
            row(3L, "B", ""),
            row(4L, "C", "not-json"),
            row(5L, "A", "[0.9,0.9]")));
        FaceFeatureCache cache = new FaceFeatureCache(mapper);
        cache.load();
        assertEquals(1, cache.size());
        assertArrayEquals(new float[]{0.5f, 0.25f}, cache.snapshot().get("A"));
    }

    @Test
    void loadClearsEntriesFromPreviousGeneration() {
        PersonMapper mapper = mock(PersonMapper.class);
        when(mapper.selectFeatureList()).thenReturn(List.of(row(1L, "A", "[0.5]")));
        FaceFeatureCache cache = new FaceFeatureCache(mapper);
        cache.put("Z", new float[]{1f});
        cache.load();
        assertFalse(cache.snapshot().containsKey("Z"));
        assertEquals(1, cache.size());
    }

    @Test
    void putRemoveClearAndReadOnlySnapshotView() {
        PersonMapper mapper = mock(PersonMapper.class);
        FaceFeatureCache cache = new FaceFeatureCache(mapper);
        cache.put("A", new float[]{0.5f});
        assertArrayEquals(new float[]{0.5f}, cache.snapshot().get("A"));
        assertThrows(UnsupportedOperationException.class, () -> cache.snapshot().put("B", new float[]{1f}));
        cache.remove("A");
        assertEquals(0, cache.size());
        cache.put("B", new float[]{1f});
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    void parseFeaturesHandlesValidAndEmptyArrays() {
        assertArrayEquals(new float[]{0.5f, -0.25f}, FaceFeatureCache.parseFeatures("[0.5,-0.25]"), 0f);
        assertEquals(0, FaceFeatureCache.parseFeatures("[]").length);
        assertThrows(RuntimeException.class, () -> FaceFeatureCache.parseFeatures("not-json"));
    }

    private static Person row(Long id, String imgId, String extract) {
        Person person = new Person();
        person.setId(id);
        person.setImgId(imgId);
        person.setExtract(extract);
        return person;
    }
}
