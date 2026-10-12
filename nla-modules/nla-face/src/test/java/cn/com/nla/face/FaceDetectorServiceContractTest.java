package cn.com.nla.face;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.face.core.FaceFeatureCache;
import cn.com.nla.face.core.FacePipeline;
import cn.com.nla.face.domain.vo.FaceDetectVo;
import cn.com.nla.face.domain.vo.PersonVo;
import cn.com.nla.face.service.IPersonService;
import cn.com.nla.face.service.impl.FaceDetectorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/** 验证检测/比对/检索服务对流水线与缓存的编排及旧默认参数语义，全部依赖 mock，不触数据库与 JNI。 */
class FaceDetectorServiceContractTest {

    private FacePipeline pipeline;
    private FaceFeatureCache featureCache;
    private IPersonService personService;
    private FaceDetectorServiceImpl service;

    @BeforeEach
    void prepareService() {
        pipeline = mock(FacePipeline.class);
        featureCache = mock(FaceFeatureCache.class);
        personService = mock(IPersonService.class);
        service = new FaceDetectorServiceImpl(pipeline, featureCache, personService);
    }

    @Test
    void findFaceInfoDelegatesUploadStreamToPipeline() {
        FaceDetectVo vo = new FaceDetectVo();
        vo.setPredict(1);
        when(pipeline.analyze(any(InputStream.class))).thenReturn(vo);
        assertSame(vo, service.findFaceInfo(file()));
    }

    @Test
    void emptyOrNullUploadIsRejected() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        assertEquals("人脸图片不能为空", assertThrows(ServiceException.class, () -> service.findFaceInfo(empty)).getMessage());
        assertEquals("人脸图片不能为空", assertThrows(ServiceException.class, () -> service.findFaceInfo(null)).getMessage());
        assertEquals("人脸图片不能为空", assertThrows(ServiceException.class, () -> service.comparison(empty, file())).getMessage());
        assertEquals("人脸图片不能为空", assertThrows(ServiceException.class, () -> service.search(null, 1, 1)).getMessage());
        verifyNoInteractions(pipeline);
    }

    @Test
    void comparisonExtractsBothFeaturesThenScores() {
        float[] f1 = new float[]{0.5f};
        float[] f2 = new float[]{0.25f};
        when(pipeline.extract(any(InputStream.class))).thenReturn(f1, f2);
        when(pipeline.compare(f1, f2)).thenReturn(0.9f);
        assertEquals(0.9f, service.comparison(file(), file()));
        verify(pipeline, times(2)).extract(any(InputStream.class));
    }

    @Test
    void searchAppliesDefaultThresholdAndTopOne() {
        float[] probe = new float[]{0f};
        when(pipeline.extract(any(InputStream.class))).thenReturn(probe);
        when(featureCache.snapshot()).thenReturn(cached("low", 1f, "high", 2f));
        when(pipeline.compare(eq(probe), any())).thenAnswer(inv -> ((float[]) inv.getArgument(1))[0] == 1f ? 0.6f : 0.9f);
        List<PersonVo> hits = List.of(new PersonVo());
        when(personService.selectByImgIds(List.of("high"))).thenReturn(hits);
        assertSame(hits, service.search(file(), null, null));
    }

    @Test
    void searchHonoursRequestedSizeAndCustomThreshold() {
        when(pipeline.extract(any(InputStream.class))).thenReturn(new float[]{0f});
        when(featureCache.snapshot()).thenReturn(cached("a", 1f, "b", 2f, "c", 3f));
        when(pipeline.compare(any(), any())).thenAnswer(inv -> ((float[]) inv.getArgument(1))[0] == 1f ? 0.4f : 0.6f);
        List<PersonVo> hits = List.of(new PersonVo(), new PersonVo());
        when(personService.selectByImgIds(List.of("b", "c"))).thenReturn(hits);
        assertSame(hits, service.search(file(), 2, 50));
    }

    @Test
    void searchWithoutHitsSkipsPersonLookup() {
        when(pipeline.extract(any(InputStream.class))).thenReturn(new float[]{0f});
        when(featureCache.snapshot()).thenReturn(cached("low", 1f));
        when(pipeline.compare(any(), any())).thenReturn(0.1f);
        assertEquals(List.of(), service.search(file(), 3, 80));
        verify(personService, never()).selectByImgIds(anyList());
    }

    private static Map<String, float[]> cached(Object... pairs) {
        Map<String, float[]> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], new float[]{(Float) pairs[i + 1]});
        }
        return Collections.unmodifiableMap(map);
    }

    private static MockMultipartFile file() {
        return new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
    }
}
