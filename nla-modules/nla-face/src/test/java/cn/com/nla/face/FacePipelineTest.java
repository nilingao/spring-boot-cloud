package cn.com.nla.face;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.facesdk.core.proxy.AgePredictorProxy;
import cn.com.nla.common.facesdk.core.proxy.EyeStateDetectorProxy;
import cn.com.nla.common.facesdk.core.proxy.FaceAntiSpoofingProxy;
import cn.com.nla.common.facesdk.core.proxy.FaceDetectorProxy;
import cn.com.nla.common.facesdk.core.proxy.FaceLandmarkerProxy;
import cn.com.nla.common.facesdk.core.proxy.FaceRecognizerProxy;
import cn.com.nla.common.facesdk.core.proxy.GenderPredictorProxy;
import cn.com.nla.common.facesdk.core.proxy.MaskDetectorProxy;
import cn.com.nla.face.core.FacePipeline;
import cn.com.nla.face.domain.vo.FaceDetectVo;
import com.seeta.sdk.EyeStateDetector;
import com.seeta.sdk.FaceAntiSpoofing;
import com.seeta.sdk.GenderPredictor;
import com.seeta.sdk.LandmarkerMask;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.SeetaRect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 用真实 PNG 解码 + mock 算法代理验证流水线属性映射、人脸数量分支与门控报错，不触碰 JNI。 */
class FacePipelineTest {

    private FaceDetectorProxy detector;
    private FaceLandmarkerProxy landmarker;
    private FaceAntiSpoofingProxy antiSpoofing;
    private GenderPredictorProxy gender;
    private AgePredictorProxy age;
    private EyeStateDetectorProxy eyes;
    private MaskDetectorProxy mask;
    private FaceRecognizerProxy recognizer;
    private ObjectProvider<FaceRecognizerProxy> recognizerProvider;
    private FacePipeline pipeline;

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> available(T proxy) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(proxy);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> unavailable() {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    @BeforeEach
    void prepareProxies() {
        detector = mock(FaceDetectorProxy.class);
        landmarker = mock(FaceLandmarkerProxy.class);
        antiSpoofing = mock(FaceAntiSpoofingProxy.class);
        gender = mock(GenderPredictorProxy.class);
        age = mock(AgePredictorProxy.class);
        eyes = mock(EyeStateDetectorProxy.class);
        mask = mock(MaskDetectorProxy.class);
        recognizer = mock(FaceRecognizerProxy.class);
        recognizerProvider = available(recognizer);
        pipeline = new FacePipeline(available(detector), available(landmarker), available(antiSpoofing),
            available(gender), available(age), available(eyes), available(mask), recognizerProvider);
    }

    @Test
    void analyzeMapsEveryAttributeAndFixesRightEyeSource() throws Exception {
        stubHappyPath();
        FaceDetectVo vo = pipeline.analyze(image());
        assertEquals(FaceAntiSpoofing.Status.SPOOF.ordinal(), vo.getPredict());
        assertEquals(1, vo.getGender());
        assertEquals(33, vo.getAge());
        // 旧实现右眼误取 detect[0]；此处双眼状态刻意不同，右眼必须取 detect[1]
        assertEquals(0, vo.getLeftEyeState());
        assertEquals(2, vo.getRightEyeState());
        assertEquals(0, vo.getMaskStatus());
        assertNull(vo.getExtract());
        verify(recognizerProvider, never()).getIfAvailable();
        verify(detector, times(2)).detect(any());
    }

    @Test
    void analyzeMapsFemaleAndNoMask() throws Exception {
        stubHappyPath();
        when(gender.predictGenderWithCrop(any(), any()))
            .thenReturn(gender.new GenderItem(GenderPredictor.GENDER.FEMALE, true));
        when(mask.detect(any(), any())).thenReturn(mask.new MaskItem(false, 0.1f));
        FaceDetectVo vo = pipeline.analyze(image());
        assertEquals(2, vo.getGender());
        assertEquals(1, vo.getMaskStatus());
    }

    @Test
    void extractRunsPipelineAndReturnsRecognizerFeature() throws Exception {
        stubHappyPath();
        float[] feature = new float[]{0.5f, 0.25f};
        when(recognizer.extract(any(), any())).thenReturn(feature);
        assertSame(feature, pipeline.extract(image()));
        verify(detector, times(2)).detect(any());
    }

    @Test
    void compareDelegatesToRecognizerSimilarity() {
        float[] f1 = new float[]{0.5f};
        float[] f2 = new float[]{0.25f};
        when(recognizer.calculateSimilarity(f1, f2)).thenReturn(0.87f);
        assertEquals(0.87f, pipeline.compare(f1, f2));
    }

    @Test
    void missingFaceIsRejected() throws Exception {
        when(detector.detect(any())).thenReturn(new SeetaRect[0]);
        ServiceException e = assertThrows(ServiceException.class, () -> pipeline.analyze(image()));
        assertThat(e.getMessage()).contains("未检测到人脸信息");
    }

    @Test
    void multipleFacesAreRejected() throws Exception {
        when(detector.detect(any())).thenReturn(new SeetaRect[]{rect(1, 1, 10, 10), rect(2, 2, 10, 10)});
        ServiceException e = assertThrows(ServiceException.class, () -> pipeline.analyze(image()));
        assertThat(e.getMessage()).contains("检测到多张人脸");
    }

    @Test
    void detectorFailureIsReportedAsBusinessError() throws Exception {
        when(detector.detect(any())).thenThrow(new IllegalStateException("native crash"));
        ServiceException e = assertThrows(ServiceException.class, () -> pipeline.analyze(image()));
        assertThat(e.getMessage()).contains("获取人脸质量信息错误");
    }

    @Test
    void missingLandmarksAreRejected() throws Exception {
        when(detector.detect(any())).thenReturn(new SeetaRect[]{rect(10, 10, 50, 50)});
        LandmarkerMask empty = new LandmarkerMask();
        empty.setMasks(new int[0]);
        empty.setSeetaPointFS(new SeetaPointF[0]);
        when(landmarker.isMask(any(), any())).thenReturn(empty);
        ServiceException e = assertThrows(ServiceException.class, () -> pipeline.analyze(image()));
        assertThat(e.getMessage()).contains("未获取人脸特质点");
    }

    @Test
    void disabledSdkReportsConfigurationError() {
        FacePipeline gated = new FacePipeline(unavailable(), unavailable(), unavailable(), unavailable(),
            unavailable(), unavailable(), unavailable(), unavailable());
        ServiceException e = assertThrows(ServiceException.class, () -> gated.analyze(image()));
        assertThat(e.getMessage()).contains("人脸识别功能未启用");
    }

    @Test
    void unreadableImageIsRejectedBeforeDetection() {
        InputStream garbage = new ByteArrayInputStream("not-an-image".getBytes());
        ServiceException e = assertThrows(ServiceException.class, () -> pipeline.extract(garbage));
        assertThat(e.getMessage()).contains("无法读取图片");
    }

    /** 整图 100x100 检出 (10,10,50,50)，裁剪后 50x50 二次检出 (5,5,20,20)。 */
    private void stubHappyPath() throws Exception {
        when(detector.detect(any())).thenReturn(
            new SeetaRect[]{rect(10, 10, 50, 50)}, new SeetaRect[]{rect(5, 5, 20, 20)});
        LandmarkerMask points = new LandmarkerMask();
        points.setMasks(new int[]{1});
        points.setSeetaPointFS(new SeetaPointF[5]);
        when(landmarker.isMask(any(), any())).thenReturn(points);
        when(antiSpoofing.predict(any(), any(), any())).thenReturn(FaceAntiSpoofing.Status.SPOOF);
        when(gender.predictGenderWithCrop(any(), any()))
            .thenReturn(gender.new GenderItem(GenderPredictor.GENDER.MALE, true));
        when(age.predictAgeWithCrop(any(), any())).thenReturn(33);
        when(eyes.detect(any(), any())).thenReturn(new EyeStateDetector.EYE_STATE[]{
            EyeStateDetector.EYE_STATE.EYE_CLOSE, EyeStateDetector.EYE_STATE.EYE_RANDOM});
        when(mask.detect(any(), any())).thenReturn(mask.new MaskItem(true, 0.9f));
    }

    private static SeetaRect rect(int x, int y, int width, int height) {
        SeetaRect rect = new SeetaRect();
        rect.x = x;
        rect.y = y;
        rect.width = width;
        rect.height = height;
        return rect;
    }

    private static InputStream image() {
        try {
            BufferedImage png = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(png, "png", out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
